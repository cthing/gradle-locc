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

import org.cthing.locc4j.Language;
import org.cthing.locc4j.Stats;
import org.gradle.api.reporting.SingleFileReport;


/**
 * Represents a line count report generated by the plugin.
 */
public interface LoccReport extends SingleFileReport {

    /**
     * Writes the line count report in a specific file format.
     *
     * @param counts Line count data to report
     */
    void generateReport(Map<Path, Map<Language, Stats>> counts);
}
