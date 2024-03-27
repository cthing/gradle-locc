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

package org.cthing.gradle.plugins.locc.reports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.cthing.annotations.AccessForTesting;
import org.cthing.locc4j.CountUtils;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskExecutionException;


/**
 * Generates a line count report in the <a href="https://yaml.org/">YAML Ain't Markup Language</a>.
 */
public final class YamlReport extends AbstractLoccReport {

    private static final int FORMAT_VERSION = 1;
    private static final String INDENT_4 = "    ";
    private static final String INDENT_8 = "        ";

    @AccessForTesting
    enum Quoting {
        None,
        Single,
        Double
    }

    @Inject
    public YamlReport(final Task task, final DirectoryProperty reportsDir) {
        super(task, "yaml", "Report in YAML format", false);
        getOutputLocation().value(reportsDir.file(REPORT_BASE_NAME + ".yaml"));
    }

    @Override
    public void generateReport(final Map<Path, Map<Language, Counts>> pathCounts) {
        final Counts totalCounts = CountUtils.total(pathCounts);
        final Set<Language> languages = CountUtils.languages(pathCounts);

        final File destination = getOutputLocation().getAsFile().get();
        try (BufferedWriter writer =
                     new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(destination.toPath()),
                                                               StandardCharsets.UTF_8))) {
            writeln(writer, "---");
            writeln(writer, "formatVersion: ", FORMAT_VERSION);
            writeln(writer, "date: ", timestamp());
            writeln(writer, "projectName: ", this.task.getProject().getName());
            writeln(writer, "projectVersion: ", this.task.getProject().getVersion().toString());
            writeln(writer, "numFiles: ", pathCounts.size());
            writeln(writer, "numUnrecognized: ", CountUtils.unrecognized(pathCounts).size());
            writeln(writer, "numLanguages: ", languages.size());
            writeCounts(writer, null, totalCounts);
            writeLanguages(writer, pathCounts);
            writeFiles(writer, pathCounts);
            writeln(writer, "...");
        } catch (final IOException ex) {
            throw new TaskExecutionException(this.task, ex);
        }
    }

    private void writeLanguages(final BufferedWriter writer, final Map<Path, Map<Language, Counts>> pathCounts)
            throws IOException {
        writeln(writer, "languages:");

        final Map<Language, Counts> langCounts = CountUtils.byLanguage(pathCounts);
        final List<Language> languages = new ArrayList<>(langCounts.keySet());
        languages.sort(Comparator.comparing(Language::getDisplayName));
        for (final Language language : languages) {
            writeln(writer, "  - name: ", language.name());
            writeln(writer, "    displayName: ", language.getDisplayName());
            writeln(writer, "    description: ", language.getDescription());
            writeln(writer, "    website: ", language.getWebsite());
            writeCounts(writer, INDENT_4, langCounts.get(language));
        }
    }

    private void writeFiles(final BufferedWriter writer, final Map<Path, Map<Language, Counts>> pathCounts)
            throws IOException {
        writeln(writer, "files:");

        final Map<Path, Counts> pathTotals = CountUtils.byFile(pathCounts);
        final Path rootProjectPath = this.task.getProject().getRootProject().getProjectDir().toPath();
        final List<Path> paths = new ArrayList<>(pathCounts.keySet());
        paths.sort(Path::compareTo);
        for (final Path path : paths) {
            final Map<Language, Counts> langCounts = pathCounts.get(path);
            final Path relativePath = rootProjectPath.relativize(path);
            writeln(writer, "  - pathname: ", relativePath.toString());
            writeln(writer, "    numLanguages: ", langCounts.size());
            writeCounts(writer, INDENT_4, pathTotals.get(path));

            writeln(writer, "    languages:");

            final List<Language> languages = new ArrayList<>(langCounts.keySet());
            languages.sort(Comparator.comparing(Language::getDisplayName));
            for (final Language language : languages) {
                writeln(writer, "      - name: ", language.name());
                writeCounts(writer, INDENT_8, langCounts.get(language));
            }
        }
    }

    private void writeCounts(final BufferedWriter writer, @Nullable final String indent, final Counts counts)
            throws IOException {
        writeln(writer, indent, "totalLines: ", counts.getTotalLines());
        writeln(writer, indent, "codeLines: ", counts.getCodeLines());
        writeln(writer, indent, "commentLines: ", counts.getCommentLines());
        writeln(writer, indent, "blankLines: ", counts.getBlankLines());
    }

    private void writeln(final BufferedWriter writer, final String str) throws IOException {
        writer.write(str);
        writer.newLine();
    }

    private void writeln(final BufferedWriter writer, final String str, @Nullable final String val)
            throws IOException {
        if (val != null) {
            writer.write(str);
            writeEscaped(writer, val);
            writer.newLine();
        }
    }

    private void writeln(final BufferedWriter writer, final String str, final int val) throws IOException {
        writeln(writer, null, str, val);
    }

    private void writeln(final BufferedWriter writer, @Nullable final String indent, final String str,
                         final int val) throws IOException {
        if (indent != null) {
            writer.write(indent);
        }
        writer.write(str);
        writer.write(Integer.toString(val, 10));
        writer.newLine();
    }

    /**
     * Writes the specified string using the appropriate YAML escaping if needed.
     *
     * @param writer Writer for the string
     * @param str String to escape and write
     * @throws IOException if there was a problem writing the string.
     */
    @AccessForTesting
    static void writeEscaped(final Writer writer, final String str) throws IOException {
        final Quoting quoting = requiresQuotes(str);
        if (quoting == Quoting.None) {
            writer.write(str);
            return;
        }
        if (quoting == Quoting.Single) {
            writer.write('\'');
            writer.write(str);
            writer.write('\'');
            return;
        }

        writer.write('"');

        int idx = 0;
        while (idx < str.length()) {
            final int ch = str.codePointAt(idx);
            final String escaped = switch (ch) {
                case 0x00 -> "\\0";
                case 0x07 -> "\\a";
                case 0x08 -> "\\b";
                case 0x09 -> "\\t";
                case 0x0A -> "\\n";
                case 0x0B -> "\\v";
                case 0x0D -> "\\r";
                case 0x22 -> "\\\"";
                case 0x2F -> "\\/";
                case 0x5C -> "\\\\";
                case 0x85 -> "\\N";
                case 0xA0 -> "\\_";
                case 0x2028 -> "\\L";
                case 0x2029 -> "\\P";
                default -> {
                    if (ch < 0x20) {
                        yield String.format("\\x%02X", ch);
                    }
                    if (ch <= 0x7E) {
                        yield String.valueOf((char)ch);
                    }
                    if (ch <= 0xFF) {
                        yield String.format("\\x%02X", ch);
                    }
                    if (ch <= 0xFFFF) {
                        yield String.format("\\u%04X", ch);
                    }
                    yield String.format("\\U%08X", ch);
                }
            };
            writer.write(escaped);

            idx += Character.charCount(ch);
        }

        writer.write('"');
    }

    /**
     * Indicates whether the specified string requires quoting according to the YAML spec. Anything requiring
     * escapes must be double-quoted. If the string only contains YAML indicator characters, it can be single-quoted.
     *
     * @param str String to test
     * @return Whether the string requires single, double or no quotes.
     */
    @AccessForTesting
    static Quoting requiresQuotes(final String str) {
        if (str.isEmpty()) {
            return Quoting.Single;
        }

        boolean needsSingle = str.startsWith(" ") || str.endsWith(" ") || str.startsWith("---") || str.endsWith("...");

        int idx = 0;
        while (idx < str.length()) {
            final int ch = str.codePointAt(idx);

            if ("#,[]{}&*!|>%@?:-/".indexOf(ch) != -1) {
                needsSingle = true;
            } else if (ch < 0x20 || ch > 0x7E || ch == '"' || ch == '\'' || ch == '\\') {
                return Quoting.Double;
            }

            idx += Character.charCount(ch);
        }

        return needsSingle ? Quoting.Single : Quoting.None;
    }
}
