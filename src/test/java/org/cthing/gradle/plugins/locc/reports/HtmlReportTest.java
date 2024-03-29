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

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


public class HtmlReportTest {

    public static Stream<Arguments> writeEscapedProvider() {
        return Stream.of(
                arguments("", ""),
                arguments("  ", "  "),
                arguments("a", "a"),
                arguments("abc", "abc"),
                arguments("Z", "Z"),
                arguments("Hello~", "Hello~"),
                arguments("This & That", "This &amp; That"),
                arguments("10 < 20", "10 &lt; 20"),
                arguments("20 > 10", "20 &gt; 10"),
                arguments("\n", "\n"),
                arguments("\t", "\t"),
                arguments("\r", "\r"),
                arguments("a\u0000b", "ab"),
                arguments("a\u001Fb", "ab"),
                arguments("a\uFFFFb", "ab"),
                arguments("a\u007Fb", "a&#x7F;b"),
                arguments("a\uD7FFb", "a&#xD7FF;b"),
                arguments("a\uE000b", "a&#xE000;b"),
                arguments("a\uFFFDb", "a&#xFFFD;b"),
                arguments("a\uD83D\uDE03b", "a&#x1F603;b")
        );
    }

    @ParameterizedTest
    @MethodSource("writeEscapedProvider")
    @DisplayName("Write a character with escaping")
    void testWriteEscapedChar(final String input, final String expected) {
        assertThat(HtmlReport.escape(input)).hasToString(expected);
    }
}
