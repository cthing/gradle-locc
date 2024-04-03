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
