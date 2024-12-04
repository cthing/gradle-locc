/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.locc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xmlunit.assertj3.XmlAssert;
import org.xmlunit.placeholder.PlaceholderDifferenceEvaluator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.gradle.testkit.runner.TaskOutcome.NO_SOURCE;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;


@SuppressWarnings("DataFlowIssue")
public class PluginIntegTest {

    private static final Pattern TIMESTAMPT_REGEX =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:-\\d{2}:\\d{2}|Z)");
    private static final JsonSchema JSON_SCHEMA;
    private static final URL XML_SCHEMA;

    static {
        try {
            final JsonNode schema = JsonLoader.fromResource("/org/cthing/gradle/plugins/locc/locc-1.json");
            JSON_SCHEMA = JsonSchemaFactory.byDefault().getJsonSchema(schema);

            XML_SCHEMA = PluginIntegTest.class.getResource("/org/cthing/gradle/plugins/locc/locc-1.xsd");
        } catch (final IOException | ProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }


    private Path projectDir;

    public static Stream<Arguments> gradleVersionProvider() {
        return Stream.of(
                arguments("8.4"),
                arguments(GradleVersion.current().getVersion())
        );
    }

    @BeforeEach
    public void setup() throws IOException {
        final Path baseDir = Path.of(System.getProperty("buildDir"), "integTest");
        Files.createDirectories(baseDir);
        this.projectDir = Files.createTempDirectory(baseDir, null);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testNoSourceSets(final String gradleVersion) throws IOException {
        Files.writeString(this.projectDir.resolve("settings.gradle.kts"), "rootProject.name=\"test\"");
        Files.writeString(this.projectDir.resolve("build.gradle.kts"), """
                plugins {
                    id("org.cthing.locc")
                }
                """);

        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, NO_SOURCE);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testEmptySourceSet(final String gradleVersion) throws IOException {
        Files.writeString(this.projectDir.resolve("settings.gradle.kts"), "rootProject.name=\"test\"");
        Files.writeString(this.projectDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("org.cthing.locc")
                }
                """);

        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, NO_SOURCE);
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testSimpleProject(final String gradleVersion) throws IOException, ProcessingException {
        copyProject("simple-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);
        verifyAllReports("/reports/simple-project");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testSimpleExcludeTestProject(final String gradleVersion) throws IOException, ProcessingException {
        copyProject("simple-exclude-test-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);
        verifyAllReports("/reports/simple-exclude-test-project");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testComplexProject(final String gradleVersion) throws IOException, ProcessingException {
        copyProject("complex-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);
        verifyAllReports("/reports/complex-project");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testComplexExcludeTestProject(final String gradleVersion) throws IOException, ProcessingException {
        copyProject("complex-exclude-test-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);
        verifyAllReports("/reports/complex-exclude-test-project");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testAbsoluteProject(final String gradleVersion) throws IOException {
        copyProject("absolute-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);

        final Path actualReport = this.projectDir.resolve("build/reports/locc/locc.json");
        assertThat(actualReport).isReadable();
        showReport(actualReport);

        final JsonNode rootNode = JsonLoader.fromFile(actualReport.toFile());
        final File pathname = new File(rootNode.get("files").get(0).get("pathname").asText());
        assertThat(pathname).isAbsolute();
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testAugmentedProject(final String gradleVersion) throws IOException {
        copyProject("augmented-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);
        verifyXmlReport("/reports/augmented-project");
        verifyHtmlReport("/reports/augmented-project");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testReplacedProject(final String gradleVersion) throws IOException {
        copyProject("replaced-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);
        verifyXmlReport("/reports/replaced-project");
        verifyHtmlReport("/reports/replaced-project");
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testExtensionsProject(final String gradleVersion) throws IOException, ProcessingException {
        copyProject("extensions-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);
        verifyAllReports("/reports/extensions-project");
    }

    private void copyProject(final String projectName) throws IOException {
        final URL projectUrl = getClass().getResource("/projects/" + projectName);
        assertThat(projectUrl).isNotNull();
        PathUtils.copyDirectory(Path.of(projectUrl.getPath()), this.projectDir);
    }

    private GradleRunner createGradleRunner(final String gradleVersion) {
        return GradleRunner.create()
                           .withProjectDir(this.projectDir.toFile())
                           .withArguments("countCodeLines")
                           .withPluginClasspath()
                           .withGradleVersion(gradleVersion);
    }

    private void verifyBuild(final BuildResult result, final TaskOutcome outcome) {
        final BuildTask task = result.task(":countCodeLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(outcome);
    }

    private void verifyAllReports(final String reportsDir) throws IOException, ProcessingException {
        verifyXmlReport(reportsDir);
        verifyJsonReport(reportsDir);
        verifyYamlReport(reportsDir);
        verifyTextReport(reportsDir);
        verifyCsvReport(reportsDir);
        verifyHtmlReport(reportsDir);
    }

    private void verifyXmlReport(final String reportsDir) throws IOException {
        final Path actualReport = this.projectDir.resolve("build/reports/locc/locc.xml");
        assertThat(actualReport).isReadable();
        showReport(actualReport);
        XmlAssert.assertThat(actualReport).isValidAgainst(XML_SCHEMA);

        final URL expectedReport = getClass().getResource(reportsDir + "/locc.xml");
        XmlAssert.assertThat(actualReport)
                 .and(expectedReport)
                 .withDifferenceEvaluator(new PlaceholderDifferenceEvaluator())
                 .areIdentical();
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfStringBuilder")
    private void verifyJsonReport(final String reportsDir)
            throws IOException, ProcessingException {
        final Path actualReport = this.projectDir.resolve("build/reports/locc/locc.json");
        assertThat(actualReport).isReadable();
        showReport(actualReport);

        final String expectedJson = IOUtils.resourceToString(reportsDir + "/locc.json", StandardCharsets.UTF_8);
        final String actualJson = Files.readString(actualReport);
        assertThatJson(actualJson).isEqualTo(expectedJson);

        final JsonNode rootNode = JsonLoader.fromFile(actualReport.toFile());
        final ProcessingReport report = JSON_SCHEMA.validate(rootNode);
        if (!report.isSuccess()) {
            final StringBuilder buffer = new StringBuilder("Validation of JSON report failed.\n");
            for (final ProcessingMessage msg : report) {
                buffer.append(msg.getMessage()).append('\n');
            }
            fail(buffer.toString());
        }
    }

    @SuppressWarnings("MismatchedQueryAndUpdateOfStringBuilder")
    private void verifyYamlReport(final String reportsDir)
            throws IOException, ProcessingException {
        final Path actualReport = this.projectDir.resolve("build/reports/locc/locc.yaml");
        assertThat(actualReport).isReadable();
        showReport(actualReport);

        final String expectedYaml = IOUtils.resourceToString(reportsDir + "/locc.yaml", StandardCharsets.UTF_8);
        final String actualYaml = Files.readString(actualReport);
        final String noTimestampYaml = TIMESTAMPT_REGEX.matcher(actualYaml).replaceFirst("ignore");
        assertThat(noTimestampYaml).isEqualTo(expectedYaml);

        final YAMLFactory factory = new YAMLFactory();
        final JsonNode rootNode = new ObjectMapper().readTree(factory.createParser(actualYaml));
        final ProcessingReport report = JSON_SCHEMA.validate(rootNode);
        if (!report.isSuccess()) {
            final StringBuilder buffer = new StringBuilder("Validation of YAML report failed.\n");
            for (final ProcessingMessage msg : report) {
                buffer.append(msg.getMessage()).append('\n');
            }
            fail(buffer.toString());
        }
    }

    private void verifyTextReport(final String reportsDir) throws IOException {
        final Path actualReport = this.projectDir.resolve("build/reports/locc/locc.txt");
        assertThat(actualReport).isReadable();
        showReport(actualReport);

        final String expectedText = IOUtils.resourceToString(reportsDir + "/locc.txt", StandardCharsets.UTF_8);
        final String actualText = TIMESTAMPT_REGEX.matcher(Files.readString(actualReport)).replaceFirst("ignore");
        assertThat(actualText).isEqualTo(expectedText);
    }

    private void verifyCsvReport(final String reportsDir) throws IOException {
        final Path actualReport = this.projectDir.resolve("build/reports/locc/locc.csv");
        showReport(actualReport);

        final String expectedText = IOUtils.resourceToString(reportsDir + "/locc.csv", StandardCharsets.UTF_8);
        assertThat(actualReport).hasContent(expectedText);
    }

    private void verifyHtmlReport(final String reportsDir) throws IOException {
        final Path actualReport = this.projectDir.resolve("build/reports/locc/locc.html");
        showReport(actualReport);

        final String expectedText = IOUtils.resourceToString(reportsDir + "/locc.html", StandardCharsets.UTF_8);
        final String actualText = TIMESTAMPT_REGEX.matcher(Files.readString(actualReport)).replaceFirst("ignore");
        assertThat(actualText).isEqualTo(expectedText);
    }

    private void showReport(final Path report) throws IOException {
        if (System.getenv("CTHING_INTEG_TEST") != null) {
            System.out.println(report.getFileName() + " " + "-".repeat(60));
            System.out.print(Files.readString(report));
            System.out.println("-".repeat(70));
        }
    }
}
