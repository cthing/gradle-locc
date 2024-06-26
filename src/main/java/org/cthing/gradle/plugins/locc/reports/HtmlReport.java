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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cthing.escapers.HtmlEscaper;
import org.cthing.gradle.plugins.locc.CountsCache;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskExecutionException;


/**
 * Generates a line count report in <a href="https://en.wikipedia.org/wiki/HTML">HTML format</a>.
 */
public final class HtmlReport extends AbstractLoccReport {

    @Inject
    public HtmlReport(final Task task, final DirectoryProperty reportsDir) {
        super(task, "html", "Report in HTML format", true);
        getOutputLocation().value(reportsDir.file(REPORT_BASE_NAME + ".html"));
    }

    @Override
    public void generateReport(final CountsCache countsCache) {
        final File destination = getOutputLocation().getAsFile().get();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(destination.toPath()),
                                                                       StandardCharsets.UTF_8))) {
            writeDocumentStart(writer);
            writeHead(writer);
            writeBodyStart(writer);
            writeSummary(writer, countsCache);
            writeLanguages(writer, countsCache);
            writeFiles(writer, countsCache);
            writeBodyEnd(writer);
            writeDocumentEnd(writer);
        } catch (final IOException ex) {
            throw new TaskExecutionException(this.task, ex);
        }
    }

    private void writeDocumentStart(final Writer writer) throws IOException {
        writer.write("""
                     <!DOCTYPE html>
                     <html lang="en">
                     """);
    }

    private void writeDocumentEnd(final Writer writer) throws IOException {
        writer.write("""
                     </html>
                     """);
    }

    private void writeHead(final Writer writer) throws IOException {
        writer.write("""
                         <head>
                             <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
                             <title>Line Count Report For %s</title>
                             <style>
                                 body {
                                     margin: 40px;
                                     padding: 0;
                                     font-family: sans-serif;
                                 }
                                 table {
                                     border: 1px solid #C3C3C3;
                                     border-collapse: collapse;
                                 }
                                 td, th {
                                     border: 1px solid #C3C3C3;
                                     padding: 5px 15px 5px 15px;
                                 }
                                 .CountCell {
                                     text-align: right;
                                 }
                                 .TotalCell {
                                     font-weight: bold;
                                 }
                                 .Unrecognized {
                                     color: #DC0000;
                                 }
                                 a:link {
                                     text-decoration: none;
                                 }
                                 a:visited {
                                     text-decoration: none;
                                 }
                                 a:hover {
                                     text-decoration: none;
                                 }
                                 a:active {
                                     text-decoration: none;
                                 }
                             </style>
                         </head>
                     """.formatted(this.task.getProject().getName()));
    }

    private void writeBodyStart(final Writer writer) throws IOException {
        writer.write("""
                         <body>
                             <h1>Line Count Report For %s</h1>
                     """.formatted(HtmlEscaper.escape(this.task.getProject().getName())));
    }

    private void writeBodyEnd(final Writer writer) throws IOException {
        writer.write("""
                         </body>
                     """);
    }

    private void writeSummary(final Writer writer, final CountsCache countsCache) throws IOException {
        final Counts totalCounts = countsCache.getTotalCounts();
        final Set<Language> languages = countsCache.getLanguages();

        writer.write("""

                             <h2>Summary</h2>
                             <table>
                                 <tbody>
                                     <tr>
                                         <td>Project</td>
                                         <td>%s</td>
                                     </tr>
                                     <tr>
                                         <td>Version</td>
                                         <td>%s</td>
                                     </tr>
                                     <tr>
                                         <td>Report date</td>
                                         <td>%s</td>
                                     </tr>
                                     <tr>
                                         <td>Number of files</td>
                                         <td>%d</td>
                                     </tr>
                                     <tr>
                                         <td>Number of languages</td>
                                         <td>%d</td>
                                     </tr>
                                     <tr>
                                         <td>Unrecognized files</td>
                                         <td>%d</td>
                                     </tr>
                                     <tr>
                                         <td>Total lines</td>
                                         <td>%d</td>
                                     </tr>
                                     <tr>
                                         <td>Code lines</td>
                                         <td>%d</td>
                                     </tr>
                                     <tr>
                                         <td>Comment lines</td>
                                         <td>%d</td>
                                     </tr>
                                     <tr>
                                         <td>Blank lines</td>
                                         <td>%d</td>
                                     </tr>
                                 </tbody>
                             </table>
                     """.formatted(HtmlEscaper.escape(this.task.getProject().getName()),
                                   HtmlEscaper.escape(this.task.getProject().getVersion().toString()),
                                   HtmlEscaper.escape(timestamp()), countsCache.getPathCounts().size(),
                                   languages.size(), countsCache.getUnrecognized().size(),
                                   totalCounts.getTotalLines(), totalCounts.getCodeLines(),
                                   totalCounts.getCommentLines(), totalCounts.getBlankLines()));
    }

    private void writeLanguages(final Writer writer, final CountsCache countsCache) throws IOException {
        writer.write("""

                             <h2>Line Count by Language</h2>
                             <table>
                                 <thead>
                                     <tr>
                                         <th>Name</th>
                                         <th>Description</th>
                                         <th class="CountCell">Total Lines</th>
                                         <th class="CountCell">Code Lines</th>
                                         <th class="CountCell">Comment Lines</th>
                                         <th class="CountCell">Blank Lines</th>
                                     </tr>
                                 </thead>
                                 <tbody>
                     """);

        final Map<Language, Counts> langCounts = countsCache.getLanguageCounts();
        final List<Language> sortedLanguages = new ArrayList<>(langCounts.keySet());
        sortedLanguages.sort(Comparator.comparing(Language::getDisplayName));
        for (final Language language : sortedLanguages) {
            final Counts counts = langCounts.get(language);
            String description = language.getDescription();
            if (description == null) {
                description = "";
            } else {
                description = HtmlEscaper.escape(description);
                final String website = language.getWebsite();
                if (website != null) {
                    description = String.format("<a href=\"%s\">%s</a>", website, description);
                }
            }

            writer.write("""
                                         <tr>
                                             <td>%s</td>
                                             <td>%s</td>
                                             <td class="CountCell">%d</td>
                                             <td class="CountCell">%d</td>
                                             <td class="CountCell">%d</td>
                                             <td class="CountCell">%d</td>
                                         </tr>
                         """.formatted(HtmlEscaper.escape(language.getDisplayName()), description,
                                       counts.getTotalLines(), counts.getCodeLines(),
                                       counts.getCommentLines(), counts.getBlankLines()));
        }

        final Counts totalCounts = countsCache.getTotalCounts();
        writer.write("""
                                     <tr>
                                         <td class="TotalCell">Total</td>
                                         <td></td>
                                         <td class="TotalCell CountCell">%d</td>
                                         <td class="TotalCell CountCell">%d</td>
                                         <td class="TotalCell CountCell">%d</td>
                                         <td class="TotalCell CountCell">%d</td>
                                     </tr>
                     """.formatted(totalCounts.getTotalLines(), totalCounts.getCodeLines(),
                                   totalCounts.getCommentLines(), totalCounts.getBlankLines()));

        writer.write("""
                                 </tbody>
                             </table>
                     """);
    }

    private void writeFiles(final Writer writer, final CountsCache countsCache) throws IOException {
        writer.write("""

                             <h2>Line Count by File</h2>
                             <table>
                                 <thead>
                                     <tr>
                                         <th>Pathname</th>
                                         <th class="CountCell">Total Lines</th>
                                         <th class="CountCell">Code Lines</th>
                                         <th class="CountCell">Comment Lines</th>
                                         <th class="CountCell">Blank Lines</th>
                                         <th>Languages</th>
                                     </tr>
                                 </thead>
                                 <tbody>
                     """);

        final Map<Path, Counts> pathTotals = countsCache.getFileCounts();
        final Set<Path> unrecognized = countsCache.getUnrecognized();
        final List<Path> paths = new ArrayList<>(countsCache.getPathCounts().keySet());
        paths.sort(Path::compareTo);
        for (final Path path : paths) {
            final Counts counts = pathTotals.getOrDefault(path, Counts.ZERO);
            final List<Language> sortedLanguages = new ArrayList<>(countsCache.getPathCounts().get(path).keySet());
            sortedLanguages.sort(Comparator.comparing(Language::getDisplayName));

            String unrecognizedClass = "";
            if (unrecognized.contains(path)) {
                unrecognizedClass = " class=\"Unrecognized\"";
            }

            writer.write("""
                                         <tr>
                                             <td%s>%s</td>
                                             <td class="CountCell">%d</td>
                                             <td class="CountCell">%d</td>
                                             <td class="CountCell">%d</td>
                                             <td class="CountCell">%d</td>
                                             <td>%s</td>
                                         </tr>
                         """.formatted(unrecognizedClass, HtmlEscaper.escape(preparePathname(path).toString()),
                                       counts.getTotalLines(), counts.getCodeLines(), counts.getCommentLines(),
                                       counts.getBlankLines(),
                                       HtmlEscaper.escape(sortedLanguages.stream()
                                                                         .map(Language::getDisplayName)
                                                                         .collect(Collectors.joining(", ")))));
        }

        final Counts totalCounts = countsCache.getTotalCounts();
        writer.write("""
                                     <tr>
                                         <td class="TotalCell">Total</td>
                                         <td class="TotalCell CountCell">%d</td>
                                         <td class="TotalCell CountCell">%d</td>
                                         <td class="TotalCell CountCell">%d</td>
                                         <td class="TotalCell CountCell">%d</td>
                                         <td></td>
                                     </tr>
                     """.formatted(totalCounts.getTotalLines(), totalCounts.getCodeLines(),
                                   totalCounts.getCommentLines(), totalCounts.getBlankLines()));

        writer.write("""
                                 </tbody>
                             </table>
                     """);
    }
}
