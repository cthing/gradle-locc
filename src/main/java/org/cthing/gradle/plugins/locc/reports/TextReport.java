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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cthing.gradle.plugins.locc.CountsCache;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskExecutionException;


/**
 * Generates a line count report in plain text.
 */
public final class TextReport extends AbstractLoccReport {

    @Inject
    public TextReport(final Task task, final DirectoryProperty reportsDir) {
        super(task, "text", "Report in text format", false);
        getOutputLocation().value(reportsDir.file(REPORT_BASE_NAME + ".txt"));
    }

    @Override
    public void generateReport(final CountsCache countsCache) {
        final Counts totalCounts = countsCache.getTotalCounts();
        final Set<Language> languages = countsCache.getLanguages();

        final File destination = getOutputLocation().getAsFile().get();
        try (BufferedWriter writer =
                     new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(destination.toPath()),
                                                               StandardCharsets.UTF_8))) {
            writer.write("Line Count Report For ");
            writeln(writer, this.task.getProject().getName());
            writeln(writer, "-".repeat(80));
            writeln(writer, "Date: ", timestamp());
            writeln(writer, "Project version: ", this.task.getProject().getVersion().toString());
            writeln(writer, "Number of files: ", countsCache.getPathCounts().size());
            writeln(writer, "Number unrecognized files: ", countsCache.getUnrecognized().size());
            writeln(writer, "Number of languages: ", languages.size());
            writeln(writer, "Total lines: ", totalCounts.getTotalLines());
            writeln(writer, "Code lines: ", totalCounts.getCodeLines());
            writeln(writer, "Comment lines: ", totalCounts.getCommentLines());
            writeln(writer, "Blank lines: ", totalCounts.getBlankLines());
            writeLanguages(writer, countsCache);
            writeFiles(writer, countsCache);
        } catch (final IOException ex) {
            throw new TaskExecutionException(this.task, ex);
        }

    }

    private void writeLanguages(final BufferedWriter writer, final CountsCache countsCache) throws IOException {
        writer.newLine();
        writeln(writer, "Languages");
        writeln(writer, "-".repeat(9));

        final Map<Language, Counts> langCounts = countsCache.getLanguageCounts();
        final List<Language> languages = new ArrayList<>(langCounts.keySet());
        languages.sort(Comparator.comparing(Language::getDisplayName));
        for (final Language language : languages) {
            final String description = language.getDescription();
            if (description == null) {
                writeln(writer, String.format("%s", language.getDisplayName()));
            } else {
                writeln(writer, String.format("%s: %s", language.getDisplayName(), description));
            }

            writeCounts(writer, langCounts.get(language));
            writer.newLine();
        }
    }

    private void writeFiles(final BufferedWriter writer, final CountsCache countsCache) throws IOException {
        writeln(writer, "Files");
        writeln(writer, "-".repeat(5));

        final Map<Path, Counts> pathTotals = countsCache.getFileCounts();
        final Path rootProjectPath = this.task.getProject().getRootProject().getProjectDir().toPath();
        final List<Path> paths = new ArrayList<>(countsCache.getPathCounts().keySet());
        paths.sort(Path::compareTo);
        boolean first = true;
        for (final Path path : paths) {
            if (!first) {
                writer.newLine();
            }
            first = false;

            final Map<Language, Counts> langCounts = countsCache.getPathCounts().get(path);
            final Path relativePath = rootProjectPath.relativize(path);
            writeln(writer, relativePath.toString());
            writeCounts(writer, pathTotals.get(path));

            final List<Language> languages = new ArrayList<>(langCounts.keySet());
            languages.sort(Comparator.comparing(Language::getDisplayName));
            writer.write("    Languages: ");
            writeln(writer, languages.stream().map(Language::getDisplayName).collect(Collectors.joining(", ")));
        }
    }

    private void writeCounts(final BufferedWriter writer, final Counts counts) throws IOException {
        writeln(writer, String.format("    Lines: %d total, %d code, %d comment, %d blank",
                                      counts.getTotalLines(), counts.getCodeLines(),
                                      counts.getCommentLines(), counts.getBlankLines()));
    }

    private void writeln(final BufferedWriter writer, final String str1, final String str2) throws IOException {
        writer.write(str1);
        writer.write(str2);
        writer.newLine();
    }

    private void writeln(final BufferedWriter writer, final String str, final int val) throws IOException {
        writer.write(str);
        writer.write(Integer.toString(val, 10));
        writer.newLine();
    }

    private void writeln(final BufferedWriter writer, final String str) throws IOException {
        writer.write(str);
        writer.newLine();
    }
}
