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

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;


public class PluginApplyTest {

    @Test
    public void testApply(@TempDir final File projectDir) {
        final Project project = ProjectBuilder.builder().withName("testProject").withProjectDir(projectDir).build();
        project.getPluginManager().apply("org.cthing.locc");

        assertThat(project.getExtensions().findByName(LoccPlugin.EXTENSION_NAME)).isInstanceOf(LoccExtension.class);

        final Task task = project.getTasks().findByName(LoccPlugin.COUNT_LINES_TASK_NAME);
        assertThat(task).isNotNull().isInstanceOf(LoccTask.class);
        final LoccReports reports = ((LoccTask)task).getReports();
        assertThat(reports.getXml().getRequired().get()).isTrue();
        assertThat(reports.getHtml().getRequired().get()).isTrue();
        assertThat(reports.getYaml().getRequired().get()).isFalse();
        assertThat(reports.getJson().getRequired().get()).isFalse();
        assertThat(reports.getCsv().getRequired().get()).isFalse();
        assertThat(reports.getText().getRequired().get()).isFalse();
        assertThat(reports.getDot().getRequired().get()).isFalse();
    }
}
