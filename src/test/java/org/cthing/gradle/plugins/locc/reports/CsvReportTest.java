/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.locc.reports;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class CsvReportTest {

    public static Stream<Arguments> escapeProvider() {
        return Stream.of(
                arguments("a", "a"),
                arguments("ab c", "ab c"),
                arguments("  ", "  "),
                arguments("", ""),
                arguments(null, ""),
                arguments("Hello \"Cruel\" World", "\"Hello \"\"Cruel\"\" World\""),
                arguments("This, and that", "\"This, and that\"")
        );
    }

    @ParameterizedTest
    @MethodSource("escapeProvider")
    public void testEscape(@Nullable final String input, final String output) {
        assertThat(CsvReport.escape(input)).isEqualTo(output);
    }
}
