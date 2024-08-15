/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.enterprise.connectedapps.processor;

import static com.google.android.enterprise.connectedapps.processor.ClassNameUtilities.append;
import static com.google.android.enterprise.connectedapps.processor.ClassNameUtilities.transformClassName;
import static com.google.android.enterprise.connectedapps.processor.CommonClassNames.FAKE_PROFILE_CONNECTOR_CLASSNAME;
import static com.google.android.enterprise.connectedapps.processor.CommonClassNames.UNAVAILABLE_PROFILE_EXCEPTION_CLASSNAME;
import static com.google.android.enterprise.connectedapps.processor.containers.CrossProfileMethodInfo.AutomaticallyResolvedParameterFilterBehaviour.REMOVE_AUTOMATICALLY_RESOLVED_PARAMETERS;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.android.enterprise.connectedapps.processor.containers.CrossProfileMethodInfo;
import com.google.android.enterprise.connectedapps.processor.containers.CrossProfileTypeInfo;
import com.google.android.enterprise.connectedapps.processor.containers.GeneratorContext;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

/**
 * Generate the {@code Profile_*_FakeOtherCacheable} class for a single cross-profile type.
 *
 * <p>This must only be used once. It should be used after {@link EarlyValidator} has been used to
 * validate that the annotated code is correct.
 */
final class FakeOtherCacheableGenerator {

  private boolean generated = false;
  private final GeneratorContext generatorContext;
  private final GeneratorUtilities generatorUtilities;
  private final CrossProfileTypeInfo crossProfileType;

  FakeOtherCacheableGenerator(
      GeneratorContext generatorContext, CrossProfileTypeInfo crossProfileType) {
    this.generatorContext = checkNotNull(generatorContext);
    this.generatorUtilities = new GeneratorUtilities(generatorContext);
    this.crossProfileType = checkNotNull(crossProfileType);
  }

  void generate() {
    if (generated) {
      throw new IllegalStateException(
          "FakeOtherCacheableGenerator#generate can only be called once");
    }
    generated = true;

    generateFakeOtherCacheable();
  }

  private void generateFakeOtherCacheable() {
    ClassName className = getFakeOtherCacheableClassName(generatorContext, crossProfileType);

    ClassName singleSenderCanThrowInterface =
        InterfaceGenerator.getSingleSenderCanThrowInterfaceClassName(
            generatorContext, crossProfileType);
    ClassName singleSenderCanThrowCacheableInterface =
        InterfaceGenerator.getSingleSenderCanThrowCacheableInterfaceClassName(
            generatorContext, crossProfileType);

    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(className)
            .addJavadoc(
                "Fake implementation of {@link $T} for use during tests.\n\n"
                    + "<p>This acts based on the state of the passed in {@link $T} and acts as if"
                    + " making a call on the other profile.\n",
                singleSenderCanThrowCacheableInterface,
                FAKE_PROFILE_CONNECTOR_CLASSNAME)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(singleSenderCanThrowCacheableInterface);

    classBuilder.addField(
        FieldSpec.builder(singleSenderCanThrowInterface, "singleSenderCanThrow")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .build());

    classBuilder.addMethod(
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(singleSenderCanThrowInterface, "singleSenderCanThrow")
            .addStatement("this.singleSenderCanThrow = singleSenderCanThrow")
            .build());

    for (CrossProfileMethodInfo method : crossProfileType.crossProfileMethods()) {
      if (!method.isCacheable()) {
        continue;
      }

      if (method.isBlocking(generatorContext, crossProfileType)) {
        generateBlockingMethodOnFakeOtherCacheable(classBuilder, method, crossProfileType);
      } else {
        throw new IllegalStateException("Unknown method type: " + method);
      }
    }

    generatorUtilities.writeClassToFile(className.packageName(), classBuilder);
  }

  private void generateBlockingMethodOnFakeOtherCacheable(
      TypeSpec.Builder classBuilder,
      CrossProfileMethodInfo method,
      CrossProfileTypeInfo crossProfileType) {

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(method.simpleName())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addExceptions(method.thrownExceptions())
            .addException(UNAVAILABLE_PROFILE_EXCEPTION_CLASSNAME)
            .returns(method.returnTypeTypeName())
            .addParameters(
                GeneratorUtilities.extractParametersFromMethod(
                    crossProfileType.supportedTypes(),
                    method.methodElement(),
                    REMOVE_AUTOMATICALLY_RESOLVED_PARAMETERS));

    methodBuilder.addStatement("return singleSenderCanThrow.$L()", method.simpleName());

    classBuilder.addMethod(methodBuilder.build());
  }

  static ClassName getFakeOtherCacheableClassName(
      GeneratorContext generatorContext, CrossProfileTypeInfo crossProfileType) {
    return transformClassName(crossProfileType.generatedClassName(), append("_FakeOtherCacheable"));
  }
}
