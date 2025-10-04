/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.locc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.cthing.gradle.plugins.locc.reports.LoccReport;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.FileCounter;
import org.cthing.locc4j.Language;
import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.reporting.Reporting;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import groovy.lang.Closure;


/**
 * Performs the work of counting the project's code file.
 */
@SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
public abstract class LoccTask extends SourceTask implements Reporting<LoccReports> {

    private final LoccReports reports;

    public LoccTask() {
        this.reports = new LoccReports(this, getReportsDir());
    }

    /**
     * Obtains the line count report directory.
     *
     * @return Line count report directory
     */
    @Internal
    public abstract DirectoryProperty getReportsDir();

    /**
     * Obtains the flag indicating whether to count documentation string as comments or ignore them. The default
     * is {@code true} to count documentation strings as comments.
     *
     * @return Flag indicating whether to count documentation strings as comments.
     */
    @Input
    @Optional
    public abstract Property<Boolean> getCountDocStrings();

    /**
     * Adds the specified file extension to specified language's list of extensions. If an extension already
     * maps to a language, it is replaced.
     *
     * @param fileExtension File extension to add (without the leading period). Extensions are case-insensitive.
     * @param language Language to map to the specified extension
     */
    public void addExtension(final String fileExtension, final Language language) {
        Language.addExtension(fileExtension, language);
    }

    /**
     * Removes the specified file extension. If the extension is not present, this method does nothing.
     *
     * @param fileExtension File extension to remove (without the leading period). Extensions are case-insensitive.
     */
    public void removeExtension(final String fileExtension) {
        Language.removeExtension(fileExtension);
    }

    @Nested
    @Override
    public LoccReports getReports() {
        return this.reports;
    }

    @Override
    public LoccReports reports(final Action<? super LoccReports> configureAction) {
        configureAction.execute(this.reports);
        return this.reports;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public LoccReports reports(final Closure closure) {
        final Closure cl = (Closure)closure.clone();
        cl.setResolveStrategy(Closure.DELEGATE_FIRST);
        cl.setDelegate(this.reports);
        cl.call(this.reports);
        return this.reports;
    }

    /**
     * Performs the work of counting lines.
     */
    @TaskAction
    public void count() {
        final List<Path> files = getSource().getFiles().stream().map(File::toPath).toList();

        final FileCounter counter = new FileCounter();
        counter.countDocStrings(getCountDocStrings().get());
        try {
            final Map<Path, Map<Language, Counts>> counts = counter.count(files);
            generateReports(counts);
        } catch (final IOException ex) {
            throw new TaskExecutionException(this, ex);
        }
    }

    private void generateReports(final Map<Path, Map<Language, Counts>> counts) {
        final CountsCache countsCache = new CountsCache(counts);
        generateReport(this.reports.getXml(), countsCache);
        generateReport(this.reports.getHtml(), countsCache);
        generateReport(this.reports.getYaml(), countsCache);
        generateReport(this.reports.getJson(), countsCache);
        generateReport(this.reports.getCsv(), countsCache);
        generateReport(this.reports.getText(), countsCache);
        generateReport(this.reports.getConsole(), countsCache);
    }

    private void generateReport(final LoccReport report, final CountsCache countsCache) {
        if (report.getRequired().get()) {
            report.generateReport(countsCache);
        }
    }
}
