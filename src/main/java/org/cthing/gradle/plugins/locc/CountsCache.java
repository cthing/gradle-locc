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

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.cthing.locc4j.CountUtils;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;


/**
 * Maintains a cache of various counts requested by reports. This avoids each report calculating the same counts.
 * For example, all reports need the total counts, so it is inefficient for each report to calculate it.
 */
public class CountsCache {

    private final Map<Path, Map<Language, Counts>> pathCounts;
    @Nullable
    private Set<Language> languages;
    @Nullable
    private Counts totalCounts;
    @Nullable
    private Map<Language, Counts> languageCounts;
    @Nullable
    private Map<Path, Counts> fileCounts;
    @Nullable
    private Set<Path> unrecognized;

    CountsCache(final Map<Path, Map<Language, Counts>> pathCounts) {
        this.pathCounts = pathCounts;
    }

    /**
     * Obtains the counts for languages in each file.
     *
     * @return Counts for languages in each file.
     */
    public Map<Path, Map<Language, Counts>> getPathCounts() {
        return this.pathCounts;
    }

    /**
     * Obtains the languages in all counted files.
     *
     * @return All languages in all counted files
     */
    public Set<Language> getLanguages() {
        if (this.languages == null) {
            this.languages = CountUtils.languages(this.pathCounts);
        }
        return this.languages;
    }

    /**
     * Calculates the total line counts for all counted files.
     *
     * @return Total line count for all files and languages.
     */
    public Counts getTotalCounts() {
        if (this.totalCounts == null) {
            this.totalCounts = CountUtils.total(this.pathCounts);
        }
        return this.totalCounts;
    }

    /**
     * Calculates the line counts for each language.
     *
     * @return Line counts for each language
     */
    public Map<Language, Counts> getLanguageCounts() {
        if (this.languageCounts == null) {
            this.languageCounts = CountUtils.byLanguage(this.pathCounts);
        }
        return this.languageCounts;
    }

    /**
     * Calculates the line counts for each file.
     *
     * @return Line counts for each file regardless of language
     */
    public Map<Path, Counts> getFileCounts() {
        if (this.fileCounts == null) {
            this.fileCounts = CountUtils.byFile(this.pathCounts);
        }
        return this.fileCounts;
    }

    /**
     * Obtains the files that were not recognized and therefore produced no counts.
     *
     * @return Files that were not recognized. If the primary language of a file cannot be determined or
     *      is not supported by this library, the entry for the file contains an empty language map.
     */
    public Set<Path> getUnrecognized() {
        if (this.unrecognized == null) {
            this.unrecognized = CountUtils.unrecognized(this.pathCounts);
        }
        return this.unrecognized;
    }
}
