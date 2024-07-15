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
import static com.google.android.enterprise.connectedapps.processor.CommonClassNames.UNAVAILABLE_PROFILE_EXCEPTION_CLASSNAME;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.android.enterprise.connectedapps.processor.containers.CrossProfileCallbackParameterInfo;
import com.google.android.enterprise.connectedapps.processor.containers.CrossProfileMethodInfo;
import com.google.android.enterprise.connectedapps.processor.containers.CrossProfileTypeInfo;
import com.google.android.enterprise.connectedapps.processor.containers.GeneratorContext;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;

/**
 * Generate the {@code Profile_*_OtherProfileCacheable} class for a single cross-profile type.
 *
 * <p>This must only be used once. It should be used after {@link EarlyValidator} has been used to
 * validate that the annotated code is correct.
 */
final class OtherProfileCacheableGenerator {

  private boolean generated = false;
  private final GeneratorContext generatorContext;
  private final GeneratorUtilities generatorUtilities;
  private final CrossProfileTypeInfo crossProfileType;

  OtherProfileCacheableGenerator(
      GeneratorContext generatorContext, CrossProfileTypeInfo crossProfileType) {
    this.generatorContext = checkNotNull(generatorContext);
    this.generatorUtilities = new GeneratorUtilities(generatorContext);
    this.crossProfileType = checkNotNull(crossProfileType);
  }

  void generate() {
    if (generated) {
      throw new IllegalStateException(
          "OtherProfileCacheableGenerator#generate can only be called once");
    }
    generated = true;

    generateOtherProfileCacheableClass();
  }

  private void generateOtherProfileCacheableClass() {
    ClassName className = getOtherProfileCacheableClassName(generatorContext, crossProfileType);

    ClassName singleSenderCanThrowInterface =
        InterfaceGenerator.getSingleSenderCanThrowInterfaceClassName(
            generatorContext, crossProfileType);
    ClassName singleSenderCanThrowCacheableInterface =
        InterfaceGenerator.getSingleSenderCanThrowCacheableInterfaceClassName(
            generatorContext, crossProfileType);

    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(className)
            .addJavadoc(
                "Implementation of {@link $T} used when interacting with the cache.\n",
                singleSenderCanThrowCacheableInterface)
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
        generateBlockingMethodOnOtherProfileCacheableClass(classBuilder, method);
      } else if (method.isCrossProfileCallback(generatorContext)) {
        generateCrossProfileCallbackMethodOnOtherProfileCacheableClass(classBuilder, method);
      } else if (method.isFuture(crossProfileType)) {
        generateFutureMethodOnOtherProfileCacheableClass(classBuilder, method);
      } else {
        throw new IllegalStateException("Unknown method type: " + method);
      }
    }

    generatorUtilities.writeClassToFile(className.packageName(), classBuilder);
  }

  private void generateBlockingMethodOnOtherProfileCacheableClass(
      TypeSpec.Builder classBuilder, CrossProfileMethodInfo method) {

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(method.simpleName())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addExceptions(method.thrownExceptions())
            .addException(UNAVAILABLE_PROFILE_EXCEPTION_CLASSNAME)
            .returns(method.returnTypeTypeName());

    // TODO: Check the cache
    methodBuilder.addStatement("return singleSenderCanThrow.$L()", method.simpleName());

    classBuilder.addMethod(methodBuilder.build());
  }

  // TODO: Add to the javadocs for future and callback methods to reflect what will be cached.
  private void generateFutureMethodOnOtherProfileCacheableClass(
      TypeSpec.Builder classBuilder, CrossProfileMethodInfo method) {

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(method.simpleName())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addExceptions(method.thrownExceptions())
            .addException(UNAVAILABLE_PROFILE_EXCEPTION_CLASSNAME)
            .returns(method.returnTypeTypeName());

    // TODO: Check the cache
    methodBuilder.addStatement("return singleSenderCanThrow.$L()", method.simpleName());

    classBuilder.addMethod(methodBuilder.build());
  }

  private void generateCrossProfileCallbackMethodOnOtherProfileCacheableClass(
      TypeSpec.Builder classBuilder, CrossProfileMethodInfo method) {
    CrossProfileCallbackParameterInfo callback =
        method.getCrossProfileCallbackParam(generatorContext).get();

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(method.simpleName())
            .addAnnotation(Override.class)
            .addModifiers(Modifier.PUBLIC)
            .addExceptions(method.thrownExceptions())
            .addException(UNAVAILABLE_PROFILE_EXCEPTION_CLASSNAME)
            .returns(method.returnTypeTypeName());

    // TODO: Check the cache
    methodBuilder.addStatement("return singleSenderCanThrow.$L()", method.simpleName());

    classBuilder.addMethod(methodBuilder.build());
  }

  static ClassName getOtherProfileCacheableClassName(
      GeneratorContext generatorContext, CrossProfileTypeInfo crossProfileType) {
    return transformClassName(
        crossProfileType.generatedClassName(), append("_OtherProfileCacheable"));
  }
}
