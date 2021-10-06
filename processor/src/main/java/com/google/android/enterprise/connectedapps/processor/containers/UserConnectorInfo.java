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
package com.google.android.enterprise.connectedapps.processor.containers;

import com.google.android.enterprise.connectedapps.annotations.AvailabilityRestrictions;
import com.google.android.enterprise.connectedapps.annotations.CustomUserConnector;
import com.google.android.enterprise.connectedapps.processor.GeneratorUtilities;
import com.google.android.enterprise.connectedapps.processor.SupportedTypes;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/** Wrapper of an interface used as a user connector. */
@AutoValue
public abstract class UserConnectorInfo {

  @AutoValue
  abstract static class CustomUserConnectorAnnotationInfo {
    abstract ClassName serviceName();

    abstract ImmutableCollection<TypeElement> parcelableWrapperClasses();

    abstract ImmutableCollection<TypeElement> futureWrapperClasses();

    abstract ImmutableCollection<TypeElement> importsClasses();

    abstract AvailabilityRestrictions availabilityRestrictions();
  }

  public abstract TypeElement connectorElement();

  public ClassName connectorClassName() {
    return ClassName.get(connectorElement());
  }

  public abstract ClassName serviceName();

  public abstract SupportedTypes supportedTypes();

  public abstract ImmutableCollection<TypeElement> parcelableWrapperClasses();

  public abstract ImmutableCollection<TypeElement> futureWrapperClasses();

  public abstract ImmutableCollection<TypeElement> importsClasses();

  public abstract AvailabilityRestrictions availabilityRestrictions();

  public static UserConnectorInfo create(
      ProcessingEnvironment processingEnv,
      TypeElement connectorElement,
      SupportedTypes globalSupportedTypes) {
    Elements elements = processingEnv.getElementUtils();
    CustomUserConnectorAnnotationInfo annotationInfo =
        extractFromCustomUserConnectorAnnotation(processingEnv, elements, connectorElement);

    Set<TypeElement> parcelableWrappers = new HashSet<>(annotationInfo.parcelableWrapperClasses());
    Set<TypeElement> futureWrappers = new HashSet<>(annotationInfo.futureWrapperClasses());

    for (TypeElement importConnectorClass : annotationInfo.importsClasses()) {
      UserConnectorInfo importConnector =
          UserConnectorInfo.create(processingEnv, importConnectorClass, globalSupportedTypes);
      parcelableWrappers.addAll(importConnector.parcelableWrapperClasses());
      futureWrappers.addAll(importConnector.futureWrapperClasses());
    }

    return new AutoValue_UserConnectorInfo(
        connectorElement,
        annotationInfo.serviceName(),
        globalSupportedTypes
            .asBuilder()
            .addParcelableWrappers(
                ParcelableWrapper.createCustomParcelableWrappers(
                    processingEnv.getTypeUtils(),
                    processingEnv.getElementUtils(),
                    parcelableWrappers))
            .addFutureWrappers(
                FutureWrapper.createCustomFutureWrappers(
                    processingEnv.getTypeUtils(), processingEnv.getElementUtils(), futureWrappers))
            .build(),
        ImmutableSet.copyOf(parcelableWrappers),
        ImmutableSet.copyOf(futureWrappers),
        annotationInfo.importsClasses(),
        annotationInfo.availabilityRestrictions());
  }

  private static CustomUserConnectorAnnotationInfo extractFromCustomUserConnectorAnnotation(
      ProcessingEnvironment processingEnv, Elements elements, TypeElement connectorElement) {
    CustomUserConnector customUserConnector =
        connectorElement.getAnnotation(CustomUserConnector.class);

    if (customUserConnector == null) {
      return new AutoValue_UserConnectorInfo_CustomUserConnectorAnnotationInfo(
          getDefaultServiceName(elements, connectorElement),
          ImmutableSet.of(),
          ImmutableSet.of(),
          ImmutableSet.of(),
          AvailabilityRestrictions.DEFAULT);
    }

    Collection<TypeElement> parcelableWrappers =
        GeneratorUtilities.extractClassesFromAnnotation(
            processingEnv.getTypeUtils(), customUserConnector::parcelableWrappers);
    Collection<TypeElement> futureWrappers =
        GeneratorUtilities.extractClassesFromAnnotation(
            processingEnv.getTypeUtils(), customUserConnector::futureWrappers);
    Collection<TypeElement> imports =
        GeneratorUtilities.extractClassesFromAnnotation(
            processingEnv.getTypeUtils(), customUserConnector::imports);

    String serviceClassName = customUserConnector.serviceClassName();

    return new AutoValue_UserConnectorInfo_CustomUserConnectorAnnotationInfo(
        serviceClassName.isEmpty()
            ? getDefaultServiceName(elements, connectorElement)
            : ClassName.bestGuess(serviceClassName),
        ImmutableSet.copyOf(parcelableWrappers),
        ImmutableSet.copyOf(futureWrappers),
        ImmutableSet.copyOf(imports),
        customUserConnector.availabilityRestrictions());
  }

  public static ClassName getDefaultServiceName(Elements elements, TypeElement connectorElement) {
    PackageElement originalPackage = elements.getPackageOf(connectorElement);

    return ClassName.get(
        originalPackage.getQualifiedName().toString(),
        String.format("%s_Service", connectorElement.getSimpleName().toString()));
  }
}
