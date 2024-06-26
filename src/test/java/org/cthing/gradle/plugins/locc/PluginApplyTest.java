/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.locc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.stream.StreamSource;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xmlunit.validation.Languages;
import org.xmlunit.validation.Validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;

import static org.assertj.core.api.Assertions.assertThat;


@SuppressWarnings("DataFlowIssue")
public class PluginApplyTest {

    @Test
    public void testApply(@TempDir final File projectDir) {
        final Project project = ProjectBuilder.builder().withName("testProject").withProjectDir(projectDir).build();
        project.getPluginManager().apply("org.cthing.locc");

        assertThat(project.getExtensions().findByName(LoccPlugin.EXTENSION_NAME)).isInstanceOf(LoccExtension.class);

        final Task task = project.getTasks().findByName(LoccPlugin.TASK_NAME);
        assertThat(task).isNotNull().isInstanceOf(LoccTask.class);
        final LoccReports reports = ((LoccTask)task).getReports();
        assertThat(reports.getXml().getRequired().get()).isTrue();
        assertThat(reports.getHtml().getRequired().get()).isTrue();
        assertThat(reports.getYaml().getRequired().get()).isFalse();
        assertThat(reports.getJson().getRequired().get()).isFalse();
        assertThat(reports.getCsv().getRequired().get()).isFalse();
        assertThat(reports.getText().getRequired().get()).isFalse();
    }

    @Test
    public void testXmlSchema() throws IOException {
        try (InputStream schema = getClass().getResourceAsStream("/org/cthing/gradle/plugins/locc/locc-1.xsd")) {
            final Validator validator = Validator.forLanguage(Languages.W3C_XML_SCHEMA_NS_URI);
            validator.setSchemaSource(new StreamSource(schema));
            assertThat(validator.validateSchema().isValid()).isTrue();
        }
    }

    @Test
    public void testJsonSchema() throws IOException {
        final JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();
        final SyntaxValidator validator = schemaFactory.getSyntaxValidator();
        final File schema = new File(getClass().getResource("/org/cthing/gradle/plugins/locc/locc-1.json").getPath());
        final JsonNode rootNode = JsonLoader.fromFile(schema);
        assertThat(validator.validateSchema(rootNode).isSuccess()).isTrue();
    }
}
