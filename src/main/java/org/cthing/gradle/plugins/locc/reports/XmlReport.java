/*
 * Copyright 2024 C Thing Software
 * SPDX-License-Identifier: Apache-2.0
 */

package org.cthing.gradle.plugins.locc.reports;

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

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.xml.XMLConstants;

import org.cthing.gradle.plugins.locc.CountsCache;
import org.cthing.locc4j.Counts;
import org.cthing.locc4j.Language;
import org.cthing.xmlwriter.XmlWriter;
import org.gradle.api.Task;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.TaskExecutionException;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;


/**
 * Generates a line count report in the <a href="https://www.w3.org/XML/">Extensible Markup Language</a>.
 */
@SuppressWarnings("HttpUrlsUsage")
public final class XmlReport extends AbstractLoccReport {

    private static final int FORMAT_VERSION = 1;
    private static final String NAMESPACE = "http://www.cthing.com/locc";
    private static final String SCHEMA_FILENAME = "locc-1.xsd";
    private static final String SCHEMA_URL = "https://www.cthing.com/schemas/" + SCHEMA_FILENAME;

    @Nullable
    private Counts totalCounts;

    @Inject
    public XmlReport(final Task task, final DirectoryProperty reportsDir) {
        super(task, "xml", "Report in XML format", true);
        getOutputLocation().value(reportsDir.file(REPORT_BASE_NAME + ".xml"));
    }

    @Override
    public void generateReport(final CountsCache countsCache) {
        this.totalCounts = countsCache.getTotalCounts();

        final File destination = getOutputLocation().getAsFile().get();
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(destination.toPath()),
                                                    StandardCharsets.UTF_8)) {
            final XmlWriter xmlWriter = new XmlWriter(writer);
            xmlWriter.setPrettyPrint(true);

            xmlWriter.startDocument();
            xmlWriter.addNSPrefix("", NAMESPACE);

            final AttributesImpl attrs = new AttributesImpl();
            addAttribute(attrs, "xmlns:xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
            addAttribute(attrs, "xsi:schemaLocation", SCHEMA_URL + " " + SCHEMA_FILENAME);
            addAttribute(attrs, "formatVersion", FORMAT_VERSION);
            addAttribute(attrs, "date", timestamp());
            addAttribute(attrs, "projectName", this.task.getProject().getName());
            addAttribute(attrs, "projectVersion", this.task.getProject().getVersion().toString());
            xmlWriter.startElement(NAMESPACE, "locc", attrs);

            writeLanguages(xmlWriter, countsCache);
            writeFiles(xmlWriter, countsCache);

            xmlWriter.endElement();
            xmlWriter.endDocument();
        } catch (final IOException | SAXException ex) {
            throw new TaskExecutionException(this.task, ex);
        }
    }

    private void writeLanguages(final XmlWriter xmlWriter, final CountsCache countsCache) throws SAXException {
        assert this.totalCounts != null;
        final Map<Language, Counts> langCounts = countsCache.getLanguageCounts();

        final AttributesImpl langsAttrs = new AttributesImpl();
        addAttribute(langsAttrs, "numLanguages", langCounts.size());
        addCountAttributes(langsAttrs, this.totalCounts);
        xmlWriter.startElement(NAMESPACE, "languages", langsAttrs);

        final List<Language> languages = new ArrayList<>(langCounts.keySet());
        languages.sort(Comparator.comparing(Language::getDisplayName));
        for (final Language language : languages) {
            writeLanguage(xmlWriter, language, langCounts.get(language));
        }

        xmlWriter.endElement();
    }

    private void writeFiles(final XmlWriter xmlWriter, final CountsCache countsCache) throws SAXException {
        assert this.totalCounts != null;
        final int numFiles = countsCache.getPathCounts().size();
        final Set<Path> unrecognized = countsCache.getUnrecognized();

        final AttributesImpl filesAttrs = new AttributesImpl();
        addAttribute(filesAttrs, "numFiles", numFiles);
        addAttribute(filesAttrs, "numUnrecognized", unrecognized.size());
        addCountAttributes(filesAttrs, this.totalCounts);
        xmlWriter.startElement(NAMESPACE, "files", filesAttrs);

        final Map<Path, Counts> pathTotals = countsCache.getFileCounts();

        final List<Path> paths = new ArrayList<>(countsCache.getPathCounts().keySet());
        paths.sort(Path::compareTo);
        for (final Path path : paths) {
            final Map<Language, Counts> langCounts = countsCache.getPathCounts().get(path);

            final AttributesImpl fileAttrs = new AttributesImpl();
            addAttribute(fileAttrs, "pathname", preparePathname(path).toString());
            if (unrecognized.contains(path)) {
                addAttribute(fileAttrs, "unrecognized", "true");
            }
            addAttribute(fileAttrs, "numLanguages", langCounts.size());
            addCountAttributes(fileAttrs, pathTotals.getOrDefault(path, Counts.ZERO));
            xmlWriter.startElement(NAMESPACE, "file", fileAttrs);

            final List<Language> languages = new ArrayList<>(langCounts.keySet());
            languages.sort(Comparator.comparing(Language::getDisplayName));
            for (final Language language : languages) {
                writeLanguageRef(xmlWriter, language, langCounts.get(language));
            }

            xmlWriter.endElement();
        }

        xmlWriter.endElement();
    }

    private void writeLanguage(final XmlWriter xmlWriter, final Language language, final Counts counts)
            throws SAXException {
        final AttributesImpl langAttrs = new AttributesImpl();
        addAttribute(langAttrs, "name", language.name());
        addAttribute(langAttrs, "displayName", language.getDisplayName());
        addAttribute(langAttrs, "description", language.getDescription());
        addAttribute(langAttrs, "website", language.getWebsite());
        addCountAttributes(langAttrs, counts);
        xmlWriter.startElement(NAMESPACE, "language", langAttrs);
        xmlWriter.endElement();
    }

    private void writeLanguageRef(final XmlWriter xmlWriter, final Language language, final Counts counts)
            throws SAXException {
        final AttributesImpl langAttrs = new AttributesImpl();
        addAttribute(langAttrs, "name", language.name());
        addCountAttributes(langAttrs, counts);
        xmlWriter.startElement(NAMESPACE, "language", langAttrs);
        xmlWriter.endElement();
    }

    private void addCountAttributes(final AttributesImpl attrs, final Counts counts) {
        addAttribute(attrs, "totalLines", counts.getTotalLines());
        addAttribute(attrs, "codeLines", counts.getCodeLines());
        addAttribute(attrs, "commentLines", counts.getCommentLines());
        addAttribute(attrs, "blankLines", counts.getBlankLines());
    }

    private void addAttribute(final AttributesImpl attrs, final String name, @Nullable final String value) {
        if (value != null) {
            attrs.addAttribute("", "", name, "CDATA", value);
        }
    }

    private void addAttribute(final AttributesImpl attrs, final String name, final int value) {
        addAttribute(attrs, name, Integer.toString(value, 10));
    }
}
