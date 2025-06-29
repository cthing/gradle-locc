/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.locc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cthing.locc4j.Counter;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class CountsCacheTest {

    private static final Map<Path, Map<Language, Counts>> PATH_COUNTS = new HashMap<>();
    private static final Path CPP_PATH = Path.of("/tmp/file1.cpp");
    private static final Path JAVA_PATH = Path.of("/tmp/file2.java");
    private static final Path UNRECOGNIZED_PATH = Path.of("/tmp/file3.foo");

    private CountsCache countsCache;

    @BeforeAll
    public static void createCounts() throws IOException {
        final Counter counter1 = new Counter(Language.Cpp);
        final Map<Language, Counts> counts1 =
                counter1.count("""
                               void App::OnMouseHook(WPARAM wParam, LPARAM lParam) {
                                   static_cast<AppFrame*>(m_pMainWnd)->GetView()->OnMouseHook(wParam, lParam);
                               }

                               BOOL App::SaveAllModified() {
                                   return MeaPositionLogMgr::Instance().SaveIfModified();
                               }
                               """);
        final Counter counter2 = new Counter(Language.Java);
        final Map<Language, Counts> counts2 =
                counter2.count("""

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

                               """);
        PATH_COUNTS.put(CPP_PATH, counts1);
        PATH_COUNTS.put(JAVA_PATH, counts2);
        PATH_COUNTS.put(UNRECOGNIZED_PATH, new EnumMap<>(Language.class));
    }

    @BeforeEach
    public void setup() {
        this.countsCache = new CountsCache(PATH_COUNTS);
    }

    @Test
    public void testGetPathCounts() {
        assertThat(this.countsCache.getPathCounts()).isEqualTo(PATH_COUNTS);
    }

    @Test
    public void testGetLanguages() {
        assertThat(this.countsCache.getLanguages()).containsExactlyInAnyOrder(Language.Cpp, Language.Java);
    }

    @Test
    public void testGetTotalCounts() {
        final Counts counts = this.countsCache.getTotalCounts();
        assertThat(counts.getTotalLines()).isEqualTo(20);
        assertThat(counts.getCodeLines()).isEqualTo(12);
        assertThat(counts.getCommentLines()).isEqualTo(5);
        assertThat(counts.getBlankLines()).isEqualTo(3);
    }

    @Test
    public void testGetLanguageCounts() {
        final Map<Language, Counts> counts = this.countsCache.getLanguageCounts();
        assertThat(counts).hasSize(2);
        assertThat(counts.get(Language.Cpp)).isEqualTo(PATH_COUNTS.get(CPP_PATH).get(Language.Cpp));
        assertThat(counts.get(Language.Java)).isEqualTo(PATH_COUNTS.get(JAVA_PATH).get(Language.Java));
    }

    @Test
    public void testGetLanguagePathCounts() {
        final Map<Language, Set<Path>> paths = this.countsCache.getLanguagePathCounts();
        assertThat(paths).hasSize(2);
        assertThat(paths.get(Language.Cpp)).containsExactly(CPP_PATH);
        assertThat(paths.get(Language.Java)).containsExactly(JAVA_PATH);
    }

    @Test
    public void testGetFileCounts() {
        final Map<Path, Counts> counts = this.countsCache.getFileCounts();
        assertThat(counts).hasSize(2);
        assertThat(counts.get(CPP_PATH)).isEqualTo(PATH_COUNTS.get(CPP_PATH).get(Language.Cpp));
        assertThat(counts.get(JAVA_PATH)).isEqualTo(PATH_COUNTS.get(JAVA_PATH).get(Language.Java));
    }

    @Test
    public void testGetUnrecognized() {
        assertThat(this.countsCache.getUnrecognized()).containsExactly(UNRECOGNIZED_PATH);
    }
}
