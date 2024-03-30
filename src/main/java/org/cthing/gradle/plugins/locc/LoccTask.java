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
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.cthing.gradle.plugins.locc.reports.LoccReport;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.FileCounter;
import org.cthing.locc4j.Language;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.reporting.Reporting;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException;

import groovy.lang.Closure;


/**
 * Performs the work of counting the project's code file.
 */
public class LoccTask extends SourceTask implements Reporting<LoccReports> {

    private final Property<Boolean> countDocStrings;
    private final LoccReports reports;

    public LoccTask() {
        final Project project = getProject();
        final LoccExtension extension = project.getExtensions().getByType(LoccExtension.class);

        this.countDocStrings = project.getObjects().property(Boolean.class).convention(extension.getCountDocStrings());

        final DirectoryProperty reportsDir = project.getObjects().directoryProperty().convention(extension.getReportsDir());
        this.reports = new LoccReports(this, reportsDir);
    }

    /**
     * Obtains the flag indicating whether to count documentation string as comments or ignore them. The default
     * is {@code true} to count documentation strings as comments.
     *
     * @return Flag indicating whether to count documentation strings as comments.
     */
    @Input
    @Optional
    public Property<Boolean> getCountDocStrings() {
        return this.countDocStrings;
    }

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
        counter.countDocStrings(this.countDocStrings.get());
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
    }

    private void generateReport(final LoccReport report, final CountsCache countsCache) {
        if (report.getRequired().get()) {
            report.generateReport(countsCache);
        }
    }
}
