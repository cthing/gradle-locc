# ![C Thing Software](https://www.cthing.com/branding/CThingSoftware-57x60.png "C Thing Software") gradle-locc

A Gradle plugin for counting lines of code in a project.

## Features

* Counts code lines, comment lines and blank lines
* Highly accurate without the expense of full parsing
* Supports over 250 computer languages
* Detects embedded languages (e.g. CSS in HTML)
* Accommodates nested comments
* Ability to associate custom file extensions with languages and remove unwanted associations
* Uses the [locc4j](https://github.com/cthing/locc4j) library, which is modeled after the
  [tokei](https://github.com/XAMPPRocky/tokei) line counting tool

## Counting Lines of Code

At first glance, counting lines of source code appears to be a relatively straightforward task. One could
detect blank lines and assume every other line contains source code. That would count lines containing
comments as source code, which is typically a bad assumption. Now begins a slippery slope of naively
applying regular expressions to detect line comments and block comments. Initial success is short-lived
once nested comments and languages that can embed other languages (e.g. CSS in HTML) are encountered. At
that point, it is tempting to employ language specific full lexing and parsing to ensure an accurate
count. While this achieves high accuracy it is at a high performance code and spends most of that performance
on needless work.

As a compromise between the naive and exhaustive approaches described above, the current state-of-the-art in
counting lines of code employs a data-driven selective matching approach. A given computer language is
described by a set of characteristics relevant to line counting. These characteristics include the file
extension, line and block comment delimiters, and regular expressions to detect important syntax that
indicates the need for more detailed parsing. Highly accurate and performant line counting tools such as
[tokei](https://github.com/XAMPPRocky/tokei), [scc](https://github.com/boyter/scc) and the
[locc4j](https://github.com/cthing/locc4j) library used by this plugin use this approach. The
plugin is able to detect and count over [250 computer languages](TODO),
and can accommodate languages embedded in languages.

This plugin counts four types of lines:

* **Total lines**: All lines in the file.
* **Code lines**: Lines considered source code. Note that a line of code with a trailing comment is considered
  a code line.
* **Comment lines**: Lines consisting solely of comments. Note that a line of code with a trailing comment is
  not counted as a comment line.
* **Blank lines**: Lines consisting solely of zero or more whitespace characters.

## Usage

The plugin is available from the [Gradle Plugin Portal](TODO) and can be applied to a Gradle project using the
`plugins` block:

```kotlin
plugins {
  id("org.cthing.locc") version "1.0.0"
}
```

The plugin creates a task called `countCodeLines` which counts all source code in all projects. Specifically, the
files in the following Gradle constructs are counted:

* All [SourceSets](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/SourceSetContainer.html),
  which includes JVM-based languages (e.g. Java, Kotlin, Groovy)
* The C++ [CppApplication](https://docs.gradle.org/current/javadoc/org/gradle/language/cpp/CppApplication.html),
  [CppLibrary](https://docs.gradle.org/current/javadoc/org/gradle/language/cpp/CppLibrary.html),
  and [CppTestSuite](https://docs.gradle.org/current/javadoc/org/gradle/nativeplatform/test/cpp/CppTestSuite.html)
  components
* The Switch [SwiftApplication](https://docs.gradle.org/current/javadoc/org/gradle/language/swift/SwiftApplication.html),
  [SwiftLibrary](https://docs.gradle.org/current/javadoc/org/gradle/language/swift/SwiftLibrary.html), and
  [SwiftXCTestSuite](https://docs.gradle.org/current/javadoc/org/gradle/nativeplatform/test/xctest/SwiftXCTestSuite.html)
  components

### Counting Additional or Different Files

The `countCodeLines` task is an instance of an `LoccTask` which is derived from
[SourceTask](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/SourceTask.html). Therefore, all
properties and methods of that task can be used to add or replace files to be counted. For example, to count
additional files, configure the `countCodeLines` task:

```groovy
tasks.countCodeLines {
    source(project.buildFile, new File(project.rootDir, 'dev/checkstyle.xml'))
}
```

The following example, completely replaces the default set of files counted with those specified:

```groovy
tasks.countCodeLines {
    source = new File(project.rootDir, 'dev/checkstyle.xml')
}
```

### File Extension Mapping

The plugin uses a built-in map of file extensions to computer languages. The complete list of supported languages
and their file extensions is available in the Javadoc for the [Language](TODO) enum. File extensions can be
added or removed from the mapping. The following example removes the association of the `css` file extension with
any language and associates the `foo` file extension with the Java programming language:

```groovy
tasks.countCodeLines {
    removeExtension("css")
    addExtension("foo", Language.Java)
}
```

### Doc Strings

Languages such as Python have a dedicated syntax for embedding documentation in source code. By default, the plugin
will count those lines as comments. To count those lines as code, configure the `locc` extension:

```groovy
locc {
    countDocStrings = false
}
```

## Reports

The plugin is capable of generating a line count report in a number of formats. Note that different formats
provide different amounts of information as described in the following table.

| Format | Project Information | Counts Per Language | Counts Per File | Counts Per Language Per File | Schema                                     |
|--------|---------------------|---------------------|-----------------|------------------------------|--------------------------------------------|
| CSV    |                     | &#x2705;            |                 |                              |                                            |
| HTML   | &#x2705;            | &#x2705;            | &#x2705;        |                              |                                            | 
| JSON   | &#x2705;            | &#x2705;            | &#x2705;        | &#x2705;                     | https://www.cthing.com/schemas/locc-1.json |
| Text   | &#x2705;            | &#x2705;            | &#x2705;        |                              |                                            |
| XML    | &#x2705;            | &#x2705;            | &#x2705;        | &#x2705;                     | https://www.cthing.com/schemas/locc-1.xsd  |       
| YAML   | &#x2705;            | &#x2705;            | &#x2705;        | &#x2705;                     | https://www.cthing.com/schemas/locc-1.json |      

The report for each format is generated as `build/reports/locc/locc.{csv, html, json, txt, xml, yaml}`. By default,
the plugin will generate a report in the HTML and XML formats. To configure which file formats are generated,
configure the task reports. For example, to output all formats:

```groovy
tasks.countCodeLines {
    reports {
        xml.required = true
        html.required = true
        yaml.required = true
        json.required = true
        csv.required = true
        text.required = true
    }
}
```

To output only a JSON format line count report:

```groovy
tasks.countCodeLines {
    reports {
        xml.required = false
        html.required = false
        json.required = true
    }
}
```

## Compatibility

The plugin requires Java 17 or greater. The following gradle versions are supported:

| Plugin Version | Gradle Version |
|----------------|----------------|
| 1.0.0          | 8.3+           |

## Building
The plugin is compiled for Java 17. If a Java 17 toolchain is not available, one will be downloaded.

Gradle is used to build the plugin:
```bash
./gradlew build
```
The Javadoc for the plugin can be generated by running:
```bash
./gradlew javadoc
```

## Releasing
TODO

Note publishing schema if changed
