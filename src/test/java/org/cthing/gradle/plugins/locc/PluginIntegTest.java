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
    private static final Path BASE_DIR = Path.of(System.getProperty("buildDir"), "integTest");
    private static final Path WORKING_DIR = Path.of(System.getProperty("projectDir"), "testkit");

    static {
        try {
            Files.createDirectories(BASE_DIR);
            Files.createDirectories(WORKING_DIR);

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
        this.projectDir = Files.createTempDirectory(BASE_DIR, "project");
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
        verifyFileReports("/reports/simple-project");
        verifyConsoleReport("""
                            ---------------------------------------------
                            Language    Files    Blank    Comment    Code
                            ---------------------------------------------
                            CSS             1        0          0       7
                            HTML            1        0          0      12
                            Java            3        9         29      21
                            ---------------------------------------------
                            Total                    9         29      40
                            ---------------------------------------------
                            """.stripIndent(), result.getOutput());
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testSimpleExcludeTestProject(final String gradleVersion) throws IOException, ProcessingException {
        copyProject("simple-exclude-test-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);
        verifyFileReports("/reports/simple-exclude-test-project");
        verifyConsoleReport("""
                            ---------------------------------------------
                            Language    Files    Blank    Comment    Code
                            ---------------------------------------------
                            CSS             1        0          0       7
                            HTML            1        0          0      12
                            Java            2        5         25      12
                            ---------------------------------------------
                            Total                    5         25      31
                            ---------------------------------------------
                            """.stripIndent(), result.getOutput());
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testComplexProject(final String gradleVersion) throws IOException, ProcessingException {
        copyProject("complex-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);
        verifyFileReports("/reports/complex-project");
        verifyConsoleReport("""
                            ---------------------------------------------
                            Language    Files    Blank    Comment    Code
                            ---------------------------------------------
                            C Header        1        3          0      10
                            C++             2        3          0      18
                            Java            4       10         44      22
                            Kotlin          2        3          0      15
                            Swift           3        3          0      17
                            ---------------------------------------------
                            Total                   22         44      82
                            ---------------------------------------------
                            """.stripIndent(), result.getOutput());
    }

    @ParameterizedTest
    @MethodSource("gradleVersionProvider")
    public void testComplexExcludeTestProject(final String gradleVersion) throws IOException, ProcessingException {
        copyProject("complex-exclude-test-project");
        final BuildResult result = createGradleRunner(gradleVersion).build();
        verifyBuild(result, SUCCESS);
        verifyFileReports("/reports/complex-exclude-test-project");
        verifyConsoleReport("""
                            ---------------------------------------------
                            Language    Files    Blank    Comment    Code
                            ---------------------------------------------
                            C Header        1        3          0      10
                            C++             1        2          0      11
                            Java            4       10         44      22
                            Kotlin          1        1          0       6
                            Swift           1        0          0       5
                            ---------------------------------------------
                            Total                   16         44      54
                            ---------------------------------------------
                            """.stripIndent(), result.getOutput());
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
        verifyFileReports("/reports/extensions-project");
    }

    private void copyProject(final String projectName) throws IOException {
        final URL projectUrl = getClass().getResource("/projects/" + projectName);
        assertThat(projectUrl).isNotNull();
        PathUtils.copyDirectory(Path.of(projectUrl.getPath()), this.projectDir);
    }

    private GradleRunner createGradleRunner(final String gradleVersion) {
        return GradleRunner.create()
                           .withProjectDir(this.projectDir.toFile())
                           .withTestKitDir(WORKING_DIR.toFile())
                           .withArguments("countCodeLines")
                           .withPluginClasspath()
                           .withGradleVersion(gradleVersion);
    }

    private void verifyBuild(final BuildResult result, final TaskOutcome outcome) {
        final BuildTask task = result.task(":countCodeLines");
        assertThat(task).isNotNull();
        assertThat(task.getOutcome()).as(result.getOutput()).isEqualTo(outcome);
    }

    private void verifyFileReports(final String reportsDir) throws IOException, ProcessingException {
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

    private void verifyConsoleReport(final String expected, final String actual) {
        assertThat(actual).contains(expected);
    }

    private void showReport(final Path report) throws IOException {
        if (System.getenv("CTHING_INTEG_TEST") != null) {
            System.out.println(report.getFileName() + " " + "-".repeat(60));
            System.out.print(Files.readString(report));
            System.out.println("-".repeat(70));
        }
    }
}
