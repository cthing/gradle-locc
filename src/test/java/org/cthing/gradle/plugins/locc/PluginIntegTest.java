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

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.assertj3.XmlAssert;
import org.xmlunit.placeholder.PlaceholderDifferenceEvaluator;
import org.xmlunit.validation.Languages;
import org.xmlunit.validation.Validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;


@SuppressWarnings("DataFlowIssue")
public class PluginIntegTest {

    private File projectDir;

    @BeforeEach
    public void setup() throws IOException {
        final Path baseDir = Path.of(System.getProperty("buildDir"), "integTest");
        Files.createDirectories(baseDir);
        this.projectDir = Files.createTempDirectory(baseDir, null).toFile();
    }

    @Test
    public void testNoSourceSets() throws IOException {
        Files.writeString(this.projectDir.toPath().resolve("settings.gradle.kts"), "rootProject.name=\"test\"");
        Files.writeString(this.projectDir.toPath().resolve("build.gradle.kts"), """
                plugins {
                    id("org.cthing.locc")
                }
                """);

        final BuildResult result = GradleRunner.create()
                                               .withProjectDir(this.projectDir)
                                               .withArguments("countLines")
                                               .withPluginClasspath()
                                               .build();
        final BuildTask task = result.task(":countLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(NO_SOURCE);
    }

    @Test
    public void testEmptySourceSet() throws IOException {
        Files.writeString(this.projectDir.toPath().resolve("settings.gradle.kts"), "rootProject.name=\"test\"");
        Files.writeString(this.projectDir.toPath().resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("org.cthing.locc")
                }
                """);

        final BuildResult result = GradleRunner.create()
                                               .withProjectDir(this.projectDir)
                                               .withArguments("countLines")
                                               .withPluginClasspath()
                                               .build();
        final BuildTask task = result.task(":countLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(NO_SOURCE);
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

    @Test
    public void testSimpleProject() throws IOException, ProcessingException {
        final URL projectUrl = getClass().getResource("/simple-project");
        assertThat(projectUrl).isNotNull();
        FileUtils.copyDirectory(new File(projectUrl.getPath()), this.projectDir);

        final BuildResult result = GradleRunner.create()
                                               .withProjectDir(this.projectDir)
                                               .withArguments("countLines")
                                               .withPluginClasspath()
                                               .withDebug(true)
                                               .build();
        final BuildTask task = result.task(":countLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(SUCCESS);

        verifyXmlReport("/reports/simple-project");
        verifyJsonReport("/reports/simple-project");
        verifyYamlReport("/reports/simple-project");
        verifyTextReport("/reports/simple-project");
        verifyCsvReport("/reports/simple-project");
        verifyHtmlReport("/reports/simple-project");
    }

    @Test
    public void testComplexProject() throws IOException, ProcessingException {
        final URL projectUrl = getClass().getResource("/complex-project");
        assertThat(projectUrl).isNotNull();
        FileUtils.copyDirectory(new File(projectUrl.getPath()), this.projectDir);

        final BuildResult result = GradleRunner.create()
                                               .withProjectDir(this.projectDir)
                                               .withArguments(":countLines")
                                               .withPluginClasspath()
                                               .withDebug(true)
                                               .build();
        final BuildTask task = result.task(":countLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(SUCCESS);

        verifyXmlReport("/reports/complex-project");
        verifyJsonReport("/reports/complex-project");
        verifyYamlReport("/reports/complex-project");
        verifyTextReport("/reports/complex-project");
        verifyCsvReport("/reports/complex-project");
        verifyHtmlReport("/reports/complex-project");
    }

    @Test
    public void testAbsoluteProject() throws IOException {
        final URL projectUrl = getClass().getResource("/absolute-project");
        assertThat(projectUrl).isNotNull();
        FileUtils.copyDirectory(new File(projectUrl.getPath()), this.projectDir);

        final BuildResult result = GradleRunner.create()
                                               .withProjectDir(this.projectDir)
                                               .withArguments("countLines")
                                               .withPluginClasspath()
                                               .withDebug(true)
                                               .build();
        final BuildTask task = result.task(":countLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(SUCCESS);

        final File actualReport = new File(this.projectDir, "build/reports/locc/locc.json");
        assertThat(actualReport).isReadable();
        showReport(actualReport);

        final JsonNode rootNode = JsonLoader.fromFile(actualReport);
        final File pathname = new File(rootNode.get("files").get(0).get("pathname").asText());
        assertThat(pathname).isAbsolute();
    }

    private void verifyXmlReport(final String reportsDir) throws IOException {
        try (InputStream expectedReport = getClass().getResourceAsStream(reportsDir + "/locc.xml");
             InputStream schema = getClass().getResourceAsStream("/org/cthing/gradle/plugins/locc/locc-1.xsd")) {
            final File actualReport = new File(this.projectDir, "build/reports/locc/locc.xml");
            assertThat(actualReport).isReadable();
            showReport(actualReport);
            XmlAssert.assertThat(actualReport).isValidAgainst(schema);
            XmlAssert.assertThat(actualReport)
                     .and(expectedReport)
                     .withDifferenceEvaluator(new PlaceholderDifferenceEvaluator())
                     .areIdentical();
        }
    }

    private void verifyJsonReport(final String reportsDir)
            throws IOException, ProcessingException {
        final File expectedReport = new File(getClass().getResource(reportsDir + "/locc.json").getPath());
        final File actualReport = new File(this.projectDir, "build/reports/locc/locc.json");
        assertThat(actualReport).isReadable();
        showReport(actualReport);

        final String expectedJson = Files.readString(expectedReport.toPath());
        final String actualJson = Files.readString(actualReport.toPath());
        assertThatJson(actualJson).isEqualTo(expectedJson);

        final JsonSchema validator = createJsonValidator();
        final JsonNode rootNode = JsonLoader.fromFile(actualReport);
        final ProcessingReport report = validator.validate(rootNode);
        if (!report.isSuccess()) {
            final StringBuilder buffer = new StringBuilder("Validation of JSON report failed.\n");
            for (final ProcessingMessage msg : report) {
                buffer.append(msg.getMessage()).append('\n');
            }
            fail(buffer.toString());
        }
    }

    private void verifyYamlReport(final String reportsDir)
            throws IOException, ProcessingException {
        final File expectedReport = new File(getClass().getResource(reportsDir + "/locc.yaml").getPath());
        final File actualReport = new File(this.projectDir, "build/reports/locc/locc.yaml");
        assertThat(actualReport).isReadable();
        showReport(actualReport);

        final String expectedYaml = Files.readString(expectedReport.toPath());
        final String actualYaml = Files.readString(actualReport.toPath());
        final String noTimestampYaml =
                actualYaml.replaceFirst("'\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}-\\d{2}:\\d{2}'", "'ignore'");
        assertThat(noTimestampYaml).isEqualTo(expectedYaml);

        final JsonSchema validator = createJsonValidator();
        final YAMLFactory factory = new YAMLFactory();
        final JsonNode rootNode = new ObjectMapper().readTree(factory.createParser(actualYaml));
        final ProcessingReport report = validator.validate(rootNode);
        if (!report.isSuccess()) {
            final StringBuilder buffer = new StringBuilder("Validation of YAML report failed.\n");
            for (final ProcessingMessage msg : report) {
                buffer.append(msg.getMessage()).append('\n');
            }
            fail(buffer.toString());
        }
    }

    private void verifyTextReport(final String reportsDir) throws IOException {
        final File expectedReport = new File(getClass().getResource(reportsDir + "/locc.txt").getPath());
        final File actualReport = new File(this.projectDir, "build/reports/locc/locc.txt");
        assertThat(actualReport).isReadable();
        showReport(actualReport);

        final String expectedText = Files.readString(expectedReport.toPath());
        final String actualText = Files.readString(actualReport.toPath())
                                       .replaceFirst("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}-\\d{2}:\\d{2}", "ignore");
        assertThat(actualText).isEqualTo(expectedText);
    }

    private void verifyCsvReport(final String reportsDir) throws IOException {
        final File expectedReport = new File(getClass().getResource(reportsDir + "/locc.csv").getPath());
        final File actualReport = new File(this.projectDir, "build/reports/locc/locc.csv");
        showReport(actualReport);

        assertThat(actualReport).hasSameTextualContentAs(expectedReport);
    }

    private void verifyHtmlReport(final String reportsDir) throws IOException {
        final File expectedReport = new File(getClass().getResource(reportsDir + "/locc.html").getPath());
        final File actualReport = new File(this.projectDir, "build/reports/locc/locc.html");
        showReport(actualReport);

        final String expectedText = Files.readString(expectedReport.toPath());
        final String actualText = Files.readString(actualReport.toPath())
                                       .replaceFirst("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}-\\d{2}:\\d{2}", "ignore");
        assertThat(actualText).isEqualTo(expectedText);
    }

    private JsonSchema createJsonValidator() throws IOException, ProcessingException {
        final JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();
        final File schemaFile = new File(getClass().getResource("/org/cthing/gradle/plugins/locc/locc-1.json").getPath());
        final JsonNode schema = JsonLoader.fromFile(schemaFile);
        return schemaFactory.getJsonSchema(schema);
    }

    private void showReport(final File report) throws IOException {
        if (System.getenv("CTHING_INTEG_TEST") != null) {
            System.out.println(report.getName() + " " + "-".repeat(60));
            System.out.print(Files.readString(report.toPath()));
            System.out.println("-".repeat(70));
        }
    }
}
