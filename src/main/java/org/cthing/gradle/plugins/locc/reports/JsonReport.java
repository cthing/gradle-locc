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

import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;


/**
 * Generates a line count report in <a href="https://www.json.org/json-en.html">JavaScript Object Notation</a>.
 */
public final class JsonReport extends AbstractLoccReport {

    @Inject
    public JsonReport(final Task task, final DirectoryProperty reportsDir) {
        super(task, "json", "Report in JSON format", false);
        getOutputLocation().value(reportsDir.file(REPORT_BASE_NAME + ".json"));
    }

    @Override
    public void generateReport(final Map<Path, Map<Language, Counts>> counts) {

    }
}
