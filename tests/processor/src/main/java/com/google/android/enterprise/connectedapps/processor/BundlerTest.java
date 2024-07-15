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

import static com.google.android.enterprise.connectedapps.processor.TestUtilities.annotatedNotesCrossProfileType;
import static com.google.android.enterprise.connectedapps.processor.TestUtilities.annotatedNotesProvider;
import static com.google.android.enterprise.connectedapps.processor.TestUtilities.CUSTOM_WRAPPER;
import static com.google.android.enterprise.connectedapps.processor.TestUtilities.NOTES_PACKAGE;
import static com.google.android.enterprise.connectedapps.processor.TestUtilities.PARCELABLE_CUSTOM_WRAPPER;
import static com.google.android.enterprise.connectedapps.processor.TestUtilities.UNSUPPORTED_TYPE;
import static com.google.android.enterprise.connectedapps.processor.TestUtilities.UNSUPPORTED_TYPE_NAME;
import static com.google.android.enterprise.connectedapps.processor.TestUtilities.CUSTOM_PROFILE_CONNECTOR_QUALIFIED_NAME;
import static com.google.android.enterprise.connectedapps.processor.TestUtilities.PROFILE_CONNECTOR_QUALIFIED_NAME;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.android.enterprise.connectedapps.processor.annotationdiscovery.AnnotationFinder;
import com.google.android.enterprise.connectedapps.processor.annotationdiscovery.AnnotationPrinter;
import com.google.android.enterprise.connectedapps.processor.annotationdiscovery.AnnotationStrings;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class BundlerTest {

  private static final String ADDITIONAL_TYPE_INVALID_TYPE_ERROR =
      "The additional type %s cannot be used by used as a parameter for, or returned by methods"
          + " annotated @CrossProfile";

  private final AnnotationPrinter annotationPrinter;

  public BundlerTest(AnnotationPrinter annotationPrinter) {
    this.annotationPrinter = annotationPrinter;
  }

  @Parameters(name = "{0}")
  public static Iterable<AnnotationStrings> getAnnotationPrinters() {
    return AnnotationFinder.annotationStrings();
  }

  @Test
  public void generatesBundlerClass() {
    Compilation compilation =
        javac()
            .withProcessors(new Processor())
            .compile(
                annotatedNotesProvider(annotationPrinter),
                annotatedNotesCrossProfileType(annotationPrinter));

    assertThat(compilation).generatedSourceFile(NOTES_PACKAGE + ".NotesType_Bundler");
  }

  @Test
  public void bundlerClassImplementsBundler() {
    Compilation compilation =
        javac()
            .withProcessors(new Processor())
            .compile(
                annotatedNotesProvider(annotationPrinter),
                annotatedNotesCrossProfileType(annotationPrinter));

    assertThat(compilation)
        .generatedSourceFile(NOTES_PACKAGE + ".NotesType_Bundler")
        .contentsAsUtf8String()
        .contains("NotesType_Bundler implements Bundler");
  }

  @Test
  public void additionalUsedType_isIncludedInBundler() {
    JavaFileObject crossProfileTypeWithAdditionalUsedType =
        JavaFileObjects.forSourceLines(
            NOTES_PACKAGE + ".NotesType",
            "package " + NOTES_PACKAGE + ";",
            "import " + annotationPrinter.crossProfileQualifiedName() + ";",
            annotationPrinter.crossProfileAsAnnotation("additionalUsedTypes=String.class"),
            "public final class NotesType {",
            "  void emptyMethod() {",
            "  };",
            "}");

    Compilation compilation =
        javac()
            .withProcessors(new Processor())
            .compile(
                annotatedNotesProvider(annotationPrinter), crossProfileTypeWithAdditionalUsedType);

    assertThat(compilation)
        .generatedSourceFile(NOTES_PACKAGE + ".NotesType_Bundler")
        .contentsAsUtf8String()
        .contains("java.lang.String");
  }

  @Test
  public void crossProfileTypeWithAdditionalUsedType_unsupportedType_failsCompile() {
    JavaFileObject crossProfileTypeWithAdditionalUsedType =
        JavaFileObjects.forSourceLines(
            NOTES_PACKAGE + ".NotesType",
            "package " + NOTES_PACKAGE + ";",
            "import " + annotationPrinter.crossProfileQualifiedName() + ";",
            annotationPrinter.crossProfileAsAnnotation(
                "additionalUsedTypes=" + UNSUPPORTED_TYPE_NAME + ".class"),
            "public final class NotesType {",
            "  void emptyMethod() {",
            "  }",
            "}");

    Compilation compilation =
        javac()
            .withProcessors(new Processor())
            .compile(
                UNSUPPORTED_TYPE,
                annotatedNotesProvider(annotationPrinter),
                crossProfileTypeWithAdditionalUsedType);

    assertThat(compilation)
        .hadErrorContaining(
            String.format(ADDITIONAL_TYPE_INVALID_TYPE_ERROR, UNSUPPORTED_TYPE_NAME));
  }

  @Test
  public void crossProfileTypeWithAdditionalUsedType_typedWithCustomWrapper_isIncludedInBundler() {
    JavaFileObject crossProfileTypeWithAdditionalUsedType =
        JavaFileObjects.forSourceLines(
            NOTES_PACKAGE + ".NotesType",
            "package " + NOTES_PACKAGE + ";",
            "import " + annotationPrinter.crossProfileQualifiedName() + ";",
            annotationPrinter.crossProfileAsAnnotation(
                "parcelableWrappers=ParcelableCustomWrapper.class,"
                    + " additionalUsedTypes=CustomWrapper.class"),
            "public final class NotesType {",
            annotationPrinter.crossProfileAsAnnotation(),
            "  void emptyMethod() {",
            "  }",
            "}");

    Compilation compilation =
        javac()
            .withProcessors(new Processor())
            .compile(
                annotatedNotesProvider(annotationPrinter),
                crossProfileTypeWithAdditionalUsedType,
                CUSTOM_WRAPPER,
                PARCELABLE_CUSTOM_WRAPPER);

    assertThat(compilation)
        .generatedSourceFile(NOTES_PACKAGE + ".NotesType_Bundler")
        .contentsAsUtf8String()
        .contains("notes.CustomWrapper");
  }

  @Test
  public void customProfileConnectorWithAdditionalUsedType_isIncludedInBundler() {
    final JavaFileObject notesConnector =
        JavaFileObjects.forSourceLines(
            NOTES_PACKAGE + ".NotesConnector",
            "package " + NOTES_PACKAGE + ";",
            "import " + CUSTOM_PROFILE_CONNECTOR_QUALIFIED_NAME + ";",
            "import " + PROFILE_CONNECTOR_QUALIFIED_NAME + ";",
            "@CustomProfileConnector(additionalUsedTypes=String.class)",
            "public interface NotesConnector extends ProfileConnector {",
            "}");

    final JavaFileObject notesType =
        JavaFileObjects.forSourceLines(
            NOTES_PACKAGE + ".NotesType",
            "package " + NOTES_PACKAGE + ";",
            "import " + annotationPrinter.crossProfileQualifiedName() + ";",
            annotationPrinter.crossProfileAsAnnotation("connector=NotesConnector.class"),
            "public final class NotesType {",
            "  void emptyMethod() {",
            "  };",
            "}");

    Compilation compilation =
        javac()
            .withProcessors(new Processor())
            .compile(annotatedNotesProvider(annotationPrinter), notesConnector, notesType);

    assertThat(compilation)
        .generatedSourceFile(NOTES_PACKAGE + ".NotesType_Bundler")
        .contentsAsUtf8String()
        .contains("java.lang.String");
  }
}
