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

import javax.inject.Inject;

import org.cthing.gradle.plugins.locc.CountsCache;
import org.cthing.jsonwriter.JsonWriter;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskExecutionException;


/**
 * Generates a line count report in <a href="https://www.json.org/json-en.html">JavaScript Object Notation</a>.
 */
public final class JsonReport extends AbstractLoccReport {

    private static final int FORMAT_VERSION = 1;

    @Inject
    public JsonReport(final Task task, final DirectoryProperty reportsDir) {
        super(task, "json", "Report in JSON format", false);
        getOutputLocation().value(reportsDir.file(REPORT_BASE_NAME + ".json"));
    }

    @Override
    public void generateReport(final CountsCache countsCache) {
        final Counts totalCounts = countsCache.getTotalCounts();
        final Set<Language> languages = countsCache.getLanguages();

        final File destination = getOutputLocation().getAsFile().get();
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(destination.toPath()),
                                                                       StandardCharsets.UTF_8))) {
            final JsonWriter jsonWriter = new JsonWriter(writer);
            jsonWriter.setPrettyPrint(true);

            jsonWriter.startObject();

            jsonWriter.member("formatVersion", FORMAT_VERSION)
                      .member("date", timestamp())
                      .member("projectName", this.task.getProject().getName())
                      .member("projectVersion", this.task.getProject().getVersion().toString())
                      .member("numFiles", countsCache.getPathCounts().size())
                      .member("numUnrecognized", countsCache.getUnrecognized().size())
                      .member("numLanguages", languages.size());
            writeCounts(jsonWriter, totalCounts);
            writeLanguages(jsonWriter, countsCache);
            writeFiles(jsonWriter, countsCache);

            jsonWriter.endObject();
        } catch (final IOException ex) {
            throw new TaskExecutionException(this.task, ex);
        }
    }

    private void writeLanguages(final JsonWriter jsonWriter, final CountsCache countsCache) throws IOException {
        jsonWriter.memberStartArray("languages");

        final Map<Language, Counts> langCounts = countsCache.getLanguageCounts();
        final List<Language> languages = new ArrayList<>(langCounts.keySet());
        languages.sort(Comparator.comparing(Language::getDisplayName));
        for (final Language language : languages) {
            jsonWriter.startObject();

            jsonWriter.member("name", language.name())
                      .member("displayName", language.getDisplayName())
                      .member("description", language.getDescription())
                      .member("website", language.getWebsite());
            writeCounts(jsonWriter, langCounts.get(language));

            jsonWriter.endObject();
        }

        jsonWriter.endArray();
    }

    private void writeFiles(final JsonWriter jsonWriter, final CountsCache countsCache) throws IOException {
        jsonWriter.memberStartArray("files");

        final Map<Path, Counts> pathTotals = countsCache.getFileCounts();
        final Set<Path> unrecognized = countsCache.getUnrecognized();
        final List<Path> paths = new ArrayList<>(countsCache.getPathCounts().keySet());
        paths.sort(Path::compareTo);
        for (final Path path : paths) {
            final boolean unrecog = unrecognized.contains(path);

            jsonWriter.startObject();

            final Map<Language, Counts> langCounts = countsCache.getPathCounts().get(path);
            jsonWriter.member("pathname", preparePathname(path).toString())
                      .member("numLanguages", langCounts.size());
            if (unrecog) {
                jsonWriter.member("unrecognized", true);
            }
            writeCounts(jsonWriter, pathTotals.getOrDefault(path, Counts.ZERO));

            if (!unrecog) {
                jsonWriter.memberStartArray("languages");

                final List<Language> languages = new ArrayList<>(langCounts.keySet());
                languages.sort(Comparator.comparing(Language::getDisplayName));
                for (final Language language : languages) {
                    jsonWriter.startObject();

                    jsonWriter.member("name", language.name());
                    writeCounts(jsonWriter, langCounts.get(language));

                    jsonWriter.endObject();
                }

                jsonWriter.endArray();
            }

            jsonWriter.endObject();
        }

        jsonWriter.endArray();
    }

    private void writeCounts(final JsonWriter jsonWriter, final Counts counts) throws IOException {
        jsonWriter.member("totalLines", counts.getTotalLines())
                  .member("codeLines", counts.getCodeLines())
                  .member("commentLines", counts.getCommentLines())
                  .member("blankLines", counts.getBlankLines());
    }
}
