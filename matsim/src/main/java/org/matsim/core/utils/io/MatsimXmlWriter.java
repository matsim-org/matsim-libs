/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimXmlWriter.java
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

package org.matsim.core.utils.io;

import java.io.IOException;
import java.util.List;

import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.misc.Time;

/**
 * A simple abstract class to write XML files.
 *
 * @author mrieser
 */
public abstract class MatsimXmlWriter extends AbstractMatsimWriter {

	/** The default location where dtds are stored on the internet. */
	public static final String DEFAULT_DTD_LOCATION = "http://www.matsim.org/files/dtd/";
	/**
	 * The namespace used in the matsim xml formats
	 */
	public static final String MATSIM_NAMESPACE = "http://www.matsim.org/files/dtd";
	/**
	 * Constant for the xml namespace attribute
	 */
	public static final String XMLNS = "xmlns";
	/**
	 * Default location of the namespace of xml schema
	 */
	public static final String DEFAULTSCHEMANAMESPACELOCATION = "http://www.w3.org/2001/XMLSchema-instance";

	/**
	 * Stores the current level of indentation
	 */
	private int indentationLevel = 0;
	/**
	 * The String used for indentation
	 */
	private String indentationString = "\t";
	/**
	 * Set this flag to true for pretty print xml output, set it to false if
	 * no indentation, whitespaces and newlines shall be printed.
	 */
	private boolean doPrettyPrint = true;
	private boolean noWhitespaces = false;

	/**
	 * Set the pretty print flag of the XMLWriter, see comment of flag.
	 * @param doPrettyPrint
	 */
	public final void setPrettyPrint(boolean doPrettyPrint){
		this.doPrettyPrint = doPrettyPrint;
	}
	/**
	 * Set the String used for indentation in the pretty print xml mode.
	 * @param indentationString
	 */
	public final void setIndentationString(String indentationString) {
		this.indentationString = indentationString;
	}

	/**
	 * Set the indentation level of the writer for
	 * pretty print option
	 * @param level
	 */
	protected final void setIndentationLevel(int level){
		this.indentationLevel = level;
	}

//	protected int getIndentationLevel(){
//		return this.indentationLevel;
//	}

	/**
	 * Writes the standard xml 1.0 header to the output.
	 *
	 * @throws UncheckedIOException
	 */
	protected final void writeXmlHead() throws UncheckedIOException {
		try {
			this.writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			if (doPrettyPrint) {
				this.writer.write(NL);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Writes the doctype declaration to the output.
	 *
	 * @param rootTag The root tag of the written XML document.
	 * @param dtdUrl The location of the document type definition of this XML document.
	 * @throws UncheckedIOException
	 */
	protected final void writeDoctype(String rootTag, String dtdUrl) throws UncheckedIOException {
		try {
			this.writer.write("<!DOCTYPE " + rootTag + " SYSTEM \"" + dtdUrl + "\">\n");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Convenience method to create XML Attributes written by startTag()
	 */
	protected static Tuple<String, String> createTuple(String one, String two){
		return new Tuple<>(one, two);
	}
	/**
	 * Convenience method to create XML Attributes written by startTag()
	 */
	protected static Tuple<String, String> createTuple(String one, int two) {
		return MatsimXmlWriter.createTuple(one, Integer.toString(two));
	}

	/**
	 * Convenience method to create XML Attributes written by startTag()
	 */
	protected static Tuple<String, String> createTuple(String one, double two) {
		return MatsimXmlWriter.createTuple(one, Double.toString(two));
	}

	/**
	 * Convenience method to create XML Attributes written by startTag()
	 */
	protected static Tuple<String, String> createTuple(String one, boolean two) {
		return MatsimXmlWriter.createTuple(one, Boolean.toString(two));
	}

	/**
	 * Convenience method to create XML Attributes written by startTag()
	 */
	protected static Tuple<String, String> createTimeTuple(String one, double sec) {
		return MatsimXmlWriter.createTuple(one, Time.writeTime(sec));
	}


	private void indent() throws IOException{
		for (int i = 0; i < this.indentationLevel; i++) {
			this.writer.write(this.indentationString);
		}
	}
	/**
	 * Writes a start tag with all attributes on the writer
	 * @param tagname
	 * @param attributes
	 * @throws UncheckedIOException
	 */
	protected final void writeStartTag(String tagname, List<Tuple<String, String>> attributes) throws UncheckedIOException{
		this.writeStartTag(tagname, attributes, false);
	}

	protected final void writeStartTag(String tagname, List<Tuple<String, String>> attributes, boolean closeElement) throws UncheckedIOException {
		try {
		if (doPrettyPrint) {
			this.writer.write(NL);
			indent();
			this.indentationLevel++;
		}
			this.writer.write("<" + tagname);
			if (attributes != null) {
				for (Tuple<String, String> t : attributes){
					this.writer.write(" " + t.getFirst() + "=\"" + encodeAttributeValue(t.getSecond()) + "\"");
				}
			}
			if (closeElement) {
				this.writer.write("/>");
				if (doPrettyPrint) {
					this.indentationLevel--;
				}
			}
			else {
				this.writer.write(">");
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}


	protected final void writeContent(String content, boolean allowWhitespaces) throws UncheckedIOException{
		try {
			if (doPrettyPrint) {
				this.noWhitespaces = !allowWhitespaces;
				if (!this.noWhitespaces) {
					this.writer.write(NL);
					this.indentationLevel++;
					indent();
				}
			}
			writer.write(encodeContent(content));
			if (!this.noWhitespaces) {
				this.indentationLevel--;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Writes a XML end tag with the given name to the given writer instance
	 * @param tagname
	 * @throws UncheckedIOException
	 */
	protected final void writeEndTag(String tagname) throws UncheckedIOException {
		try {
			if (doPrettyPrint) {
				this.indentationLevel--;
				if (!this.noWhitespaces) {
					this.writer.write(NL);
					indent();
				}
				else {
					this.noWhitespaces = false;
				}
			}
			this.writer.write("</" + tagname + ">");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Encodes the given string in such a way that it no longer contains
	 * characters that have a special meaning in xml.
	 * 
	 * @see <a href="http://www.w3.org/International/questions/qa-escapes#use">http://www.w3.org/International/questions/qa-escapes#use</a>
	 * @param attributeValue
	 * @return String with some characters replaced by their xml-encoding.
	 */
	protected static String encodeAttributeValue(final String attributeValue) {
		if (attributeValue.contains("&") || attributeValue.contains("\"") || attributeValue.contains("<") || attributeValue.contains(">")) {
			return attributeValue.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
		}
		return attributeValue;
	}

	protected static String encodeContent(final String content) {
		if (content.contains("&") || content.contains("<") || content.contains(">")) {
			return content.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		}
		return content;
	}

}
