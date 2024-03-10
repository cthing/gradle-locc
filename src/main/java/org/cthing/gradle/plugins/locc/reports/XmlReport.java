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

import java.nio.file.Path;
import java.util.Map;

import javax.inject.Inject;

import org.cthing.locc4j.Language;
import org.cthing.locc4j.Stats;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;


/**
 * Generates a line count report in the <a href="https://www.w3.org/XML/">Extensible Markup Language</a>.
 */
public final class XmlReport extends AbstractLoccReport {

    @Inject
    public XmlReport(final Project project, final DirectoryProperty reportsDir) {
        super(project, "xml", "Report in XML format", true);
        getOutputLocation().value(reportsDir.file(REPORT_BASE_NAME + ".xml"));
    }

    @Override
    public void generateReport(final Map<Path, Map<Language, Stats>> counts) {

    }
}
