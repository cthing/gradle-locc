/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
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

import javax.inject.Inject;

import org.cthing.escapers.YamlEscaper;
import org.cthing.gradle.plugins.locc.CountsCache;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskExecutionException;
import org.jspecify.annotations.Nullable;


/**
 * Generates a line count report in the <a href="https://yaml.org/">YAML Ain't Markup Language</a>.
 */
public final class YamlReport extends AbstractLoccReport {

    private static final int FORMAT_VERSION = 1;
    private static final String INDENT_4 = "    ";
    private static final String INDENT_8 = "        ";

    @Inject
    public YamlReport(final Task task, final DirectoryProperty reportsDir) {
        super(task, "yaml", "Report in YAML format", false);
        getOutputLocation().value(reportsDir.file(REPORT_BASE_NAME + ".yaml"));
    }

    @Override
    public void generateReport(final CountsCache countsCache) {
        final Counts totalCounts = countsCache.getTotalCounts();
        final Set<Language> languages = countsCache.getLanguages();

        final File destination = getOutputLocation().getAsFile().get();
        try (BufferedWriter writer =
                     new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(destination.toPath()),
                                                               StandardCharsets.UTF_8))) {
            writeln(writer, "---");
            writeln(writer, "formatVersion: ", FORMAT_VERSION);
            writeln(writer, "date: ", timestamp());
            writeln(writer, "projectName: ", this.task.getProject().getName());
            writeln(writer, "projectVersion: ", this.task.getProject().getVersion().toString());
            writeln(writer, "numFiles: ", countsCache.getPathCounts().size());
            writeln(writer, "numUnrecognized: ", countsCache.getUnrecognized().size());
            writeln(writer, "numLanguages: ", languages.size());
            writeCounts(writer, null, totalCounts);
            writeLanguages(writer, countsCache);
            writeFiles(writer, countsCache);
            writeln(writer, "...");
        } catch (final IOException ex) {
            throw new TaskExecutionException(this.task, ex);
        }
    }

    private void writeLanguages(final BufferedWriter writer, final CountsCache countsCache) throws IOException {
        writeln(writer, "languages:");

        final Map<Language, Counts> langCounts = countsCache.getLanguageCounts();
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

    private void writeFiles(final BufferedWriter writer, final CountsCache countsCache) throws IOException {
        writeln(writer, "files:");

        final Map<Path, Counts> pathTotals = countsCache.getFileCounts();
        final Set<Path> unrecognized = countsCache.getUnrecognized();
        final List<Path> paths = new ArrayList<>(countsCache.getPathCounts().keySet());
        paths.sort(Path::compareTo);
        for (final Path path : paths) {
            final Map<Language, Counts> langCounts = countsCache.getPathCounts().get(path);
            writeln(writer, "  - pathname: ", preparePathname(path).toString());
            writeln(writer, "    numLanguages: ", langCounts.size());
            if (unrecognized.contains(path)) {
                writeln(writer, "    unrecognized: ", "true");
            }
            writeCounts(writer, INDENT_4, pathTotals.getOrDefault(path, Counts.ZERO));

            final List<Language> languages = new ArrayList<>(langCounts.keySet());
            if (languages.isEmpty()) {
                writeln(writer, "    languages: []");
            } else {
                writeln(writer, "    languages:");

                languages.sort(Comparator.comparing(Language::getDisplayName));
                for (final Language language : languages) {
                    writeln(writer, "      - name: ", language.name());
                    writeCounts(writer, INDENT_8, langCounts.get(language));
                }
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
            YamlEscaper.escape(val, writer);
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
}
