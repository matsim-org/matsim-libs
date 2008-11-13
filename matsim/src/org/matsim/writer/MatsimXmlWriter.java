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

package org.matsim.writer;

import java.io.IOException;
import java.util.List;

import org.matsim.utils.collections.Tuple;
import org.matsim.utils.misc.Time;

/**
 * A simple abstract class to write XML files.
 *
 * @author mrieser
 */
public abstract class MatsimXmlWriter extends MatsimWriter {

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
	private String indentationString = "\t".intern();
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
	public void setPrettyPrint(boolean doPrettyPrint){
		this.doPrettyPrint = doPrettyPrint;
	}
	/**
	 * Set the String used for indentation in the pretty print xml mode.
	 * @param indentationString
	 */
	public void setIndentationString(String indentationString) {
		this.indentationString = indentationString;
	}
	
	/**
	 * Set the indentation level of the writer for 
	 * pretty print option
	 * @param level
	 */
	protected void setIndentationLevel(int level){
		this.indentationLevel = level;
	}
	
	protected int getIndentationLevel(){
		return this.indentationLevel;
	}
	
	/**
	 * Writes the standard xml 1.0 header to the output.
	 *
	 * @throws IOException
	 */
	protected void writeXmlHead() throws IOException {
		this.writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		if (doPrettyPrint) {
			this.writer.write(NL);
		}
	}

	/**
	 * Writes the doctype declaration to the output.
	 *
	 * @param rootTag The root tag of the written XML document.
	 * @param dtdUrl The location of the document type definition of this XML document.
	 * @throws IOException
	 */
	protected void writeDoctype(String rootTag, String dtdUrl) throws IOException {
		this.writer.write("<!DOCTYPE " + rootTag + " SYSTEM \"" + dtdUrl + "\">\n");
	}
	
	/**
	 * Convenience method to create XML Attributes written by startTag()
	 */
	protected Tuple<String, String> createTuple(String one, String two){
		return new Tuple<String, String>(one, two);
	}
	/**
	 * Convenience method to create XML Attributes written by startTag()
	 */	
	protected Tuple<String, String> createTuple(String one, int two) {
		return this.createTuple(one, Integer.toString(two));
	}

	/**
	 * Convenience method to create XML Attributes written by startTag()
	 */
	protected Tuple<String, String> createTuple(String one, double two) {
		return this.createTuple(one, Double.toString(two));
	}
	
	/**
	 * Convenience method to create XML Attributes written by startTag()
	 */
	protected Tuple<String, String> createTuple(String one, boolean two) {
		return this.createTuple(one, Boolean.toString(two));
	}

	/**
	 * Convenience method to create XML Attributes written by startTag()
	 */
	protected Tuple<String, String> createTimeTuple(String one, double sec) {
		return this.createTuple(one, Time.writeTime(sec));
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
	 * @param out
	 * @throws IOException
	 */
	protected void writeStartTag(String tagname, List<Tuple<String, String>> attributes) throws IOException{
		this.writeStartTag(tagname, attributes, false);
	}
	
	protected void writeStartTag(String tagname, List<Tuple<String, String>> attributes, boolean closeElement) throws IOException {
		if (doPrettyPrint) {
			this.writer.write(NL);
			indent();
			this.indentationLevel++;
		}
		this.writer.write("<" + tagname);
		if (attributes != null) {
			int length = 0;
			for (Tuple<String, String> t : attributes){
				if (doPrettyPrint) {
					length = length + t.getFirst().length() + t.getSecond().length();
					if (length > 80) {
						this.writer.write(NL);
						length = 0;
					}
				}
				this.writer.write(" " + t.getFirst() + "=\"" + t.getSecond() + "\"");
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
	}

	
	protected void writeContent(String content, boolean allowWhitespaces) throws IOException{
		if (doPrettyPrint) {
			this.noWhitespaces = !allowWhitespaces;
			if (!this.noWhitespaces) {
				this.writer.write(NL);
				this.indentationLevel++;
				indent();				
			}
		}
		writer.write(content);
		if (!this.noWhitespaces) {
			this.indentationLevel--;
		}
	}
	
	/**
	 * Writes a XML end tag with the given name to the given writer instance
	 * @param tagname
	 * @param out
	 * @throws IOException
	 */
	protected void writeEndTag(String tagname) throws IOException {
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
	}
	
}
