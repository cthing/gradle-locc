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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.assertj3.XmlAssert;
import org.xmlunit.placeholder.PlaceholderDifferenceEvaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;


public class PluginIntegTest {

    @Test
    public void testNoSourceSets(@TempDir final Path projectDir) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("org.cthing.locc")
                }
                """);

        final BuildResult result = GradleRunner.create()
                                               .withProjectDir(projectDir.toFile())
                                               .withArguments("countLines")
                                               .withPluginClasspath()
                                               .build();
        final BuildTask task = result.task(":countLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(NO_SOURCE);
    }

    @Test
    public void testEmptySourceSet(@TempDir final Path projectDir) throws IOException {
        Files.writeString(projectDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("org.cthing.locc")
                }
                """);

        final BuildResult result = GradleRunner.create()
                                               .withProjectDir(projectDir.toFile())
                                               .withArguments("countLines")
                                               .withPluginClasspath()
                                               .build();
        final BuildTask task = result.task(":countLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(NO_SOURCE);
    }

    @Test
    public void testSimpleProject(@TempDir final File projectDir) throws IOException {
        final URL projectUrl = getClass().getResource("/simple-project");
        assertThat(projectUrl).isNotNull();
        FileUtils.copyDirectory(new File(projectUrl.getPath()), projectDir);

        final BuildResult result = GradleRunner.create()
                                               .withProjectDir(projectDir)
                                               .withArguments("countLines")
                                               .withPluginClasspath()
                                               .withDebug(true)
                                               .build();
        final BuildTask task = result.task(":countLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(SUCCESS);

        verifyXmlReport(projectDir, "/reports/simple-project");
    }

    @Test
    public void testComplexProject(@TempDir final File projectDir) throws IOException {
        final URL projectUrl = getClass().getResource("/complex-project");
        assertThat(projectUrl).isNotNull();
        FileUtils.copyDirectory(new File(projectUrl.getPath()), projectDir);

        final BuildResult result = GradleRunner.create()
                                               .withProjectDir(projectDir)
                                               .withArguments(":countLines")
                                               .withPluginClasspath()
                                               .withDebug(true)
                                               .build();
        final BuildTask task = result.task(":countLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(SUCCESS);

        verifyXmlReport(projectDir, "/reports/complex-project");
    }

    private void verifyXmlReport(final File projectDir, final String reportsDir) throws IOException {
        try (InputStream expectedReport = getClass().getResourceAsStream(reportsDir + "/locc.xml");
             InputStream schema = getClass().getResourceAsStream("/org/cthing/gradle/plugins/locc/locc-1.xsd")) {
            final File actualReport = new File(projectDir, "build/reports/locc/locc.xml");
            XmlAssert.assertThat(actualReport).isValidAgainst(schema);
            XmlAssert.assertThat(actualReport)
                     .and(expectedReport)
                     .withDifferenceEvaluator(new PlaceholderDifferenceEvaluator())
                     .areIdentical();
        }
    }
}
