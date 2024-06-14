/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.locc;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.reporting.ReportingExtension;


/**
 * Global configuration for the plugin.
 */
public class LoccExtension {

    private final Property<Boolean> includeTestSources;
    private final Property<Boolean> countDocStrings;
    private final DirectoryProperty reportsDir;

    public LoccExtension(final Project project) {
        final ObjectFactory objects = project.getObjects();
        this.includeTestSources = objects.property(Boolean.class).convention(Boolean.TRUE);
        this.countDocStrings = objects.property(Boolean.class).convention(Boolean.TRUE);

        final DirectoryProperty baseReportsDir = project.getExtensions().getByType(ReportingExtension.class).getBaseDirectory();
        this.reportsDir = objects.directoryProperty().convention(baseReportsDir.map(base -> base.dir("locc")));
    }

    /**
     * Obtains the flag indicating whether test sources should be counted. The default is {@code true},
     * which will count test sources.
     *
     * @return Flag indicating whether test sources should be counted.
     */
    public Property<Boolean> getIncludeTestSources() {
        return this.includeTestSources;
    }

    /**
     * Obtains the flag indicating whether to count documentation string as comments or ignore them. The default
     * is {@code true} to count documentation strings as comments.
     *
     * @return Flag indicating whether to count documentation strings as comments.
     */
    public Property<Boolean> getCountDocStrings() {
        return this.countDocStrings;
    }

    /**
     * Obtains the directory into which count reports are written. The default is {@code build/reports/locc}.
     *
     * @return Directory into which count reports are written.
     */
    public DirectoryProperty getReportsDir() {
        return this.reportsDir;
    }
}
