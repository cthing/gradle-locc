/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.locc.reports;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.cthing.annotations.AccessForTesting;
import org.cthing.escapers.CsvEscaper;
import org.cthing.gradle.plugins.locc.CountsCache;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskExecutionException;
import org.jspecify.annotations.Nullable;


/**
 * Generates a line count report in the
 * <a href="https://en.wikipedia.org/wiki/Comma-separated_values">comma-separated values format</a>.
 */
public final class CsvReport extends AbstractLoccReport {

    @Inject
    public CsvReport(final Task task, final DirectoryProperty reportsDir) {
        super(task, "csv", "Report in CSV format", false);
        getOutputLocation().value(reportsDir.file(REPORT_BASE_NAME + ".csv"));
    }

    @Override
    public void generateReport(final CountsCache countsCache) {
        final Counts totalCounts = countsCache.getTotalCounts();

        final File destination = getOutputLocation().getAsFile().get();
        try (BufferedWriter writer =
                     new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(destination.toPath()),
                                                               StandardCharsets.UTF_8))) {
            writeln(writer, "ID,Name,Description,Total Lines,Code Lines,Comment Lines,Blank Lines");
            writeln(writer, String.format("ALL,All,All languages,%d,%d,%d,%d",
                                          totalCounts.getTotalLines(), totalCounts.getCodeLines(),
                                          totalCounts.getCommentLines(), totalCounts.getBlankLines()));

            final Map<Language, Counts> langCounts = countsCache.getLanguageCounts();
            final List<Language> languages = new ArrayList<>(langCounts.keySet());
            languages.sort(Comparator.comparing(Language::getDisplayName));
            for (final Language language : languages) {
                final Counts counts = langCounts.get(language);
                writeln(writer, String.format("%s,%s,%s,%d,%d,%d,%d", escape(language.name()),
                                              escape(language.getDisplayName()), escape(language.getDescription()),
                                              counts.getTotalLines(), counts.getCodeLines(),
                                              counts.getCommentLines(), counts.getBlankLines()));
            }
        } catch (final IOException ex) {
            throw new TaskExecutionException(this.task, ex);
        }
    }

    private void writeln(final Writer writer, final String str) throws IOException {
        writer.write(str);
        writer.write("\r\n");
    }

    /**
     * Performs quoting and escaping of CSV content. Apply double quotes to any string that contains a comma. If
     * a string contains double quotes, replace them with two consecutive double quotes.
     *
     * @param str String to escape
     * @return Escaped string. If {@code null} is specified, an empty string is returned.
     */
    @AccessForTesting
    static String escape(@Nullable final String str) {
        final String escaped = CsvEscaper.escape(str);
        return (escaped == null) ? "" : escaped;
    }
}
