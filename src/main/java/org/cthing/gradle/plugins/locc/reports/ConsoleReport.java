/*
 * Copyright 2025 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.locc.reports;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.cthing.gradle.plugins.locc.CountsCache;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;


/**
 * Generates a line count report to the console.
 */
public final class ConsoleReport extends AbstractLoccReport {

    private static final class Widths {
        int language;
        int files;
        int blank;
        int comment;
        int code;
    }

    private static final int COL_SEPARATOR = 4;
    private static final String COL_PADDING = " ".repeat(COL_SEPARATOR);

    @Inject
    public ConsoleReport(final Task task, final DirectoryProperty reportsDir) {
        super(task, "console", "Report to console", false);
        // To satisfy the reports interface but no file is written.
        getOutputLocation().value(reportsDir.file(REPORT_BASE_NAME + ".console"));
    }

    @Override
    public void generateReport(final CountsCache countsCache) {
        final Map<Language, Counts> langCounts = countsCache.getLanguageCounts();
        final List<Language> languages = new ArrayList<>(langCounts.keySet());
        languages.sort(Comparator.comparing(Language::getDisplayName));
        final Map<Language, Set<Path>> langPathCounts = countsCache.getLanguagePathCounts();
        final Counts total = countsCache.getTotalCounts();
        final Widths widths = new Widths();

        widths.language = "Language".length();
        widths.files = "Files".length();
        widths.blank = "Blank".length();
        widths.comment = "Comment".length();
        widths.code = "Code".length();

        for (final Language language : languages) {
            final String name = language.getDisplayName();
            final Set<Path> paths = langPathCounts.get(language);
            final Counts counts = langCounts.get(language);

            widths.language = maxWidth(widths.language, name);
            widths.blank = maxWidth(widths.blank, counts.getBlankLines());
            widths.comment = maxWidth(widths.comment, counts.getCommentLines());
            widths.code = maxWidth(widths.code, counts.getCodeLines());
            widths.files = maxWidth(widths.files, paths.size());
        }

        widths.language = maxWidth(widths.language, "Total");
        widths.blank = maxWidth(widths.blank, total.getBlankLines());
        widths.comment = maxWidth(widths.comment, total.getCommentLines());
        widths.code = maxWidth(widths.code, total.getCodeLines());

        final int totalWidth = widths.language + widths.files + widths.blank + widths.comment + widths.code
                               + 4 * COL_SEPARATOR;
        final String divider = "-".repeat(totalWidth);

        final StringBuilder builder = new StringBuilder();

        builder.append(divider).append('\n')
               .append(leftAlign("Language", widths.language)).append(COL_PADDING)
               .append(leftAlign("Files", widths.files)).append(COL_PADDING)
               .append(leftAlign("Blank", widths.blank)).append(COL_PADDING)
               .append(leftAlign("Comment", widths.comment)).append(COL_PADDING)
               .append("Code").append('\n')
               .append(divider).append('\n');

        for (final Language language : languages) {
            final String name = language.getDisplayName();
            final Set<Path> paths = langPathCounts.get(language);
            final Counts counts = langCounts.get(language);

            builder.append(leftAlign(name, widths.language)).append(COL_PADDING)
                   .append(rightAlign(paths.size(), widths.files)).append(COL_PADDING)
                   .append(rightAlign(counts.getBlankLines(), widths.blank)).append(COL_PADDING)
                   .append(rightAlign(counts.getCommentLines(), widths.comment)).append(COL_PADDING)
                   .append(rightAlign(counts.getCodeLines(), widths.code))
                   .append('\n');
        }

        builder.append(divider).append('\n')
               .append(leftAlign("Total", widths.language)).append(COL_PADDING)
               .append(" ".repeat(widths.files)).append(COL_PADDING)
               .append(rightAlign(total.getBlankLines(), widths.blank)).append(COL_PADDING)
               .append(rightAlign(total.getCommentLines(), widths.comment)).append(COL_PADDING)
               .append(rightAlign(total.getCodeLines(), widths.code))
               .append('\n')
               .append(divider).append('\n');

        this.task.getLogger().lifecycle(builder.toString());
    }

    private int maxWidth(final int existing, final String value) {
        return Math.max(existing, value.length());
    }

    private int maxWidth(final int existing, final int value) {
        return Math.max(existing, String.valueOf(value).length());
    }

    private String leftAlign(final String str, final int width) {
        final String spec = "%-" + width + "s";
        return String.format(spec, str);
    }

    private String rightAlign(final int value, final int width) {
        final String spec = "%" + width + "d";
        return String.format(spec, value);
    }
}
