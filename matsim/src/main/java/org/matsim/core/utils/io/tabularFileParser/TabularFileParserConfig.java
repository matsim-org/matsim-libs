/* *********************************************************************** *
 * project: org.matsim.*
 * TabularFileParserConfig.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.utils.io.tabularFileParser;

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Configuration for a <code>TabularFileParser</code>.
 *
 * @author gunnar
 *
 */
public class TabularFileParserConfig {

    // -------------------- CLASS VARIABLES --------------------

    private static final String ANYTHING = ".*";

    private static final String ANY_SPACE = "\\s*";

    // -------------------- INSTANCE VARIABLES --------------------

    private String file = null;

    private URL url = null;

    private String startRegex = null;

    private String endRegex = null;

    private String commentRegex = null;

    private String delimiterRegex = null;

    private Charset charset = StandardCharsets.UTF_8;

    // -------------------- CONSTRUCTION --------------------

    /**
     * Empty default constructor.
     */
    public TabularFileParserConfig() {
    }

    // -------------------- SETTERS --------------------

    /**
     * Sets the file to be parsed.
     *
     * @param file
     *            the file to be parsed
     */
    public void setFileName(String file) {
        this.file = file;
    }

    /**
     * Sets the url to be parsed.
     *
     * @param url
     *            the url to be parsed
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * Sets the charset for the file. Defaults to UTF-8
     *
     * @param charset the charset used to reade the file
     */
    public void setCharset(Charset charset) {
    	this.charset = charset;
    }

    // ---------- DIRECT SETTING OF REGULAR EXPRESSIONS ----------

    /**
     * Sets the regular expression that identifies the the first line of the
     * file section to be parsed.
     *
     * @param regex
     *            the regular expression that identifies the the first line of
     *            the file section to be parsed
     */
    public void setStartRegex(String regex) {
        this.startRegex = regex;
    }

    /**
     * Sets the regular expression that identifies the first line <em>after</em>
     * the file section to be parsed.
     *
     * @param regex
     *            the regular expression that identifies the first line
     *            <em>after</em> the file section to be parsed
     */
    public void setEndRegex(String regex) {
        this.endRegex = regex;
    }

    /**
     * Sets the regular expression that identifies lines to be ignored during
     * parsing.
     *
     * @param regex
     *            the regular expression that identifies lines to be ignored
     *            during parsing
     */
    public void setCommentRegex(String regex) {
        this.commentRegex = regex;
    }

    /**
     * Sets the regular expression that identifies splitting locations in a
     * parsed line.
     *
     * @param regex
     *            the regular expression that identifies splitting locations in
     *            a parsed line
     */
    public void setDelimiterRegex(String regex) {
        this.delimiterRegex = regex;
    }

    // ---------- CREATION OF REGULAR EXPRESSIONS FROM TAGS ----------

    /**
     * Parsing starts with the first occurrence of <code>tag</code> at the
     * beginning of a line.
     *
     * @param tag
     *            the first line to be parsed begins with this
     *            <code>String</code>
     */
    public void setStartTag(String tag) {
        if (tag != null)
            startRegex = quote(tag) + ANYTHING;
    }

    /**
     * Parsing ends with the first occurence of <code>tag</code> at the
     * beginning of a line.
     *
     * @param tag
     *            the line before which parsing stops begins with this
     *            <code>String</code>
     */
    public void setEndTag(String tag) {
        if (tag != null)
            endRegex = quote(tag) + ANYTHING;
    }

    /**
     * All lines that begin with an element in the <code>tags</code> array are
     * ignored
     *
     * @param tags
     *            an array of line beginnings that are to be ignored
     */
    public void setCommentTags(String[] tags) {
        commentRegex = alternativeExpr(tags) + ANYTHING;
    }

    /**
     * A line is split into seperate rows wherever the parser encounters an
     * element of the <code>tags</code> array, which is be preceeded and
     * succeeded by zero or more whitespaces.
     *
     * @param tags
     *            an array of <code>String</code>s denoting column delimiters
     */
    public void setDelimiterTags(String[] tags) {
        if (tags == null || tags.length == 0)
            delimiterRegex = null;
        else
            delimiterRegex = ANY_SPACE + alternativeExpr(tags) + ANY_SPACE;
    }

    // ---------- GENERATION OF REGULAR EXPRESSIONS ----------

    private String alternativeExpr(String[] alternatives) {
        StringBuilder result = new StringBuilder();

        if (alternatives != null)
            for (int i = 0; i < alternatives.length; i++) {
                result.append(quote(alternatives[i]));
                if (i < alternatives.length - 1)
                    result.append('|');
            }
        return result.toString();
    }

    private String quote(String expr) {
        return "\\Q" + expr + "\\E";
    }

    // -------------------- DATA ACCESS --------------------

    public String getFile() {
        return file;
    }

    public URL getUrl() {
        return this.url;
    }

    public String getStartRegex() {
        return startRegex;
    }

    public String getEndRegex() {
        return endRegex;
    }

    public String getCommentRegex() {
        return commentRegex;
    }

    public String getDelimiterRegex() {
        return delimiterRegex;
    }

    public Charset getCharset() {
    	return this.charset;
    }

    // MISC

    @Override
		public String toString() {
        StringBuilder result = new StringBuilder(100);

        result.append("TabularFileParserConfig:\n\tfile=");
        result.append(file);
        result.append("\n\tstartRegex=");
        result.append(startRegex);
        result.append("\n\tendRegex=");
        result.append(endRegex);
        result.append("\n\tcommentRegex=");
        result.append(commentRegex);
        result.append("\n\tdelimiterRegex=");
        result.append(delimiterRegex);

        return result.toString();
    }

}
