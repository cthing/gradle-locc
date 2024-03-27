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

import java.io.IOException;
import java.io.StringWriter;
import java.util.stream.Stream;

import org.cthing.gradle.plugins.locc.reports.YamlReport.Quoting;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;


@SuppressWarnings("UnnecessaryUnicodeEscape")
public class YamlReportTest {

    public static Stream<Arguments> quotesProvider() {
        return Stream.of(
                arguments("a", Quoting.None),
                arguments("abc", Quoting.None),
                arguments("ab c", Quoting.None),
                arguments("", Quoting.Single),
                arguments(" ", Quoting.Single),
                arguments(" abc", Quoting.Single),
                arguments("abc ", Quoting.Single),
                arguments("---", Quoting.Single),
                arguments("...", Quoting.Single),
                arguments("ab#c", Quoting.Single),
                arguments("ab,c", Quoting.Single),
                arguments("ab[c", Quoting.Single),
                arguments("ab]c", Quoting.Single),
                arguments("ab{c", Quoting.Single),
                arguments("ab}c", Quoting.Single),
                arguments("ab&c", Quoting.Single),
                arguments("ab*c", Quoting.Single),
                arguments("ab!c", Quoting.Single),
                arguments("ab|c", Quoting.Single),
                arguments("ab>c", Quoting.Single),
                arguments("ab\"c", Quoting.Double),
                arguments("ab\\c", Quoting.Double),
                arguments("ab/c", Quoting.Single),
                arguments("ab%c", Quoting.Single),
                arguments("ab@c", Quoting.Single),
                arguments("ab?c", Quoting.Single),
                arguments("ab:c", Quoting.Single),
                arguments("ab\n", Quoting.Double),
                arguments("\u0123", Quoting.Double),
                arguments("\u1F603", Quoting.Double)
        );
    }

    @ParameterizedTest
    @MethodSource("quotesProvider")
    public void testRequiresQuotes(final String str, final Quoting quoting) {
        assertThat(YamlReport.requiresQuotes(str)).isEqualTo(quoting);
    }

    public static Stream<Arguments> escapeProvider() {
        return Stream.of(
                arguments("a", "a"),
                arguments("abc", "abc"),
                arguments("ab c", "ab c"),
                arguments(" abc", "' abc'"),
                arguments("abc ", "'abc '"),
                arguments("---", "'---'"),
                arguments("...", "'...'"),
                arguments("", "''"),
                arguments("  ", "'  '"),
                arguments("a\u0000b", "\"a\\0b\""),
                arguments("a\u0007b", "\"a\\ab\""),
                arguments("a\u0008b", "\"a\\bb\""),
                arguments("a\tb", "\"a\\tb\""),
                arguments("a\nb", "\"a\\nb\""),
                arguments("a\u000Bb", "\"a\\vb\""),
                arguments("a\rb", "\"a\\rb\""),
                arguments("a\"b", "\"a\\\"b\""),
                arguments("a\\b", "\"a\\\\b\""),
                arguments("a/b", "'a/b'"),
                arguments("a\u005Cb", "\"a\\b\""),
                arguments("a\u0085b", "\"a\\Nb\""),
                arguments("a\u00A0b", "\"a\\_b\""),
                arguments("a\u2028b", "\"a\\Lb\""),
                arguments("a\u2029b", "\"a\\Pb\""),
                arguments("a\u0001", "\"a\\x01\""),
                arguments("a\u00FF", "\"a\\xFF\""),
                arguments("a\u2030", "\"a\\u2030\""),
                arguments("a\uD83D\uDE03", "\"a\\U0001F603\"")
        );
    }

    @ParameterizedTest
    @MethodSource("escapeProvider")
    public void testWriteEscaped(final String input, final String output) throws IOException {
        final StringWriter writer = new StringWriter();
        YamlReport.writeEscaped(writer, input);
        assertThat(writer.toString()).isEqualTo(output);
    }
}
