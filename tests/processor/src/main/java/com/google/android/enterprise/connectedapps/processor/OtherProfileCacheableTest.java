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

import static com.google.android.enterprise.connectedapps.processor.TestUtilities.NOTES_PACKAGE;
import static com.google.android.enterprise.connectedapps.processor.TestUtilities.annotatedNotesCrossProfileType;
import static com.google.android.enterprise.connectedapps.processor.TestUtilities.annotatedNotesProvider;
import static com.google.common.truth.Truth.assertThat;
import static com.google.testing.compile.CompilationSubject.assertThat;
import static com.google.testing.compile.Compiler.javac;

import com.google.android.enterprise.connectedapps.processor.annotationdiscovery.AnnotationFinder;
import com.google.android.enterprise.connectedapps.processor.annotationdiscovery.AnnotationPrinter;
import com.google.android.enterprise.connectedapps.processor.annotationdiscovery.AnnotationStrings;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import java.util.Optional;
import javax.tools.JavaFileObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public final class OtherProfileCacheableTest {

  private final AnnotationPrinter annotationPrinter;

  public OtherProfileCacheableTest(AnnotationPrinter annotationPrinter) {
    this.annotationPrinter = annotationPrinter;
  }

  @Parameters(name = "{0}")
  public static Iterable<AnnotationStrings> getAnnotationPrinters() {
    return AnnotationFinder.annotationStrings();
  }

  @Test
  public void compile_generatesOtherProfileCacheableClass() {
    JavaFileObject notesType =
        JavaFileObjects.forSourceLines(
            NOTES_PACKAGE + ".NotesType",
            "package " + NOTES_PACKAGE + ";",
            "import com.google.android.enterprise.connectedapps.annotations.Cacheable;",
            "import " + annotationPrinter.crossProfileQualifiedName() + ";",
            "public final class NotesType {",
            annotationPrinter.crossProfileAsAnnotation(),
            "  @Cacheable",
            "  public int countNotes() {",
            "    return 1;",
            "  }",
            "}");

    Compilation compilation =
        javac()
            .withProcessors(new Processor())
            .compile(notesType, annotatedNotesProvider(annotationPrinter));

    assertThat(compilation).generatedSourceFile(NOTES_PACKAGE + ".NotesType_OtherProfileCacheable");
  }

  @Test
  public void compile_otherProfileClassCacheableImplementsSingleSenderCanThrowCacheable() {
    JavaFileObject notesType =
        JavaFileObjects.forSourceLines(
            NOTES_PACKAGE + ".NotesType",
            "package " + NOTES_PACKAGE + ";",
            "import com.google.android.enterprise.connectedapps.annotations.Cacheable;",
            "import " + annotationPrinter.crossProfileQualifiedName() + ";",
            "public final class NotesType {",
            annotationPrinter.crossProfileAsAnnotation(),
            "  @Cacheable",
            "  public int countNotes() {",
            "    return 1;",
            "  }",
            "}");

    Compilation compilation =
        javac()
            .withProcessors(new Processor())
            .compile(notesType, annotatedNotesProvider(annotationPrinter));

    assertThat(compilation)
        .generatedSourceFile(NOTES_PACKAGE + ".NotesType_OtherProfileCacheable")
        .contentsAsUtf8String()
        .contains(
            "class NotesType_OtherProfileCacheable implements "
                + "NotesType_SingleSenderCanThrowCacheable");
  }

  @Test
  public void compile_noCacheableMethods_doesNotGenerateOtherProfileCacheableClass() {
    Compilation compilation =
        javac()
            .withProcessors(new Processor())
            .compile(
                annotatedNotesProvider(annotationPrinter),
                annotatedNotesCrossProfileType(annotationPrinter));

    Optional<JavaFileObject> otherProfileCacheableClass =
        compilation.generatedSourceFile(NOTES_PACKAGE + ".NotesType_OtherProfileCacheable");

    assertThat(otherProfileCacheableClass).isEmpty();
  }
}
