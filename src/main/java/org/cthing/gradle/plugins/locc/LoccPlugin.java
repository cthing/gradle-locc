/*
 * Copyright 2024 C Thing Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cthing.gradle.plugins.locc;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ReportingBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.language.cpp.CppApplication;
import org.gradle.language.cpp.CppComponent;
import org.gradle.language.cpp.CppLibrary;
import org.gradle.language.swift.SwiftApplication;
import org.gradle.language.swift.SwiftComponent;
import org.gradle.language.swift.SwiftLibrary;
import org.gradle.nativeplatform.test.cpp.CppTestSuite;
import org.gradle.nativeplatform.test.xctest.SwiftXCTestSuite;


/**
 * Gradle plugin for counting lines of code.
 */
public class LoccPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "locc";
    public static final String COUNT_LINES_TASK_NAME = "countLines";

    @Override
    public void apply(final Project project) {
        project.getPluginManager().apply(ReportingBasePlugin.class);

        project.getExtensions().create(EXTENSION_NAME, LoccExtension.class, project);

        project.getTasks().register(COUNT_LINES_TASK_NAME, LoccTask.class, loccTask -> {
            final Callable<Set<File>> filesProvider = () -> {
                final Set<File> files = new HashSet<>();

                for (final Project proj : project.getAllprojects()) {
                    final SourceSetContainer sourceSets = proj.getExtensions().findByType(SourceSetContainer.class);
                    if (sourceSets != null) {
                        for (final SourceSet sourceSet : sourceSets) {
                            files.addAll(sourceSet.getAllSource().getFiles());
                        }
                    }

                    List.of(CppApplication.class, CppLibrary.class, CppTestSuite.class).forEach(clazz -> {
                        final CppComponent cppComponent = proj.getExtensions().findByType(clazz);
                        if (cppComponent != null) {
                            files.addAll(cppComponent.getCppSource().getFiles());
                            files.addAll(cppComponent.getHeaderFiles().getFiles());
                        }
                    });

                    List.of(SwiftApplication.class, SwiftLibrary.class, SwiftXCTestSuite.class).forEach(clazz -> {
                        final SwiftComponent swiftComponent = proj.getExtensions().findByType(clazz);
                        if (swiftComponent != null) {
                            files.addAll(swiftComponent.getSwiftSource().getFiles());
                        }
                    });
                }

                return files;
            };

            loccTask.setSource(filesProvider);
        });
    }
}
