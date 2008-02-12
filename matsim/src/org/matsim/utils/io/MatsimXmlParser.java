/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimXmlParser.java
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

package org.matsim.utils.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * An abstract XML-Parser which can be easily extended for reading custom XML-formats. This class handles all the low level
 * functionality required to parse xml-files. Extending classes have only to implement {@link #startTag} and {@link #endTag}
 * to implement a custom parser.<br/>
 * The parser implements a custom <code>EntityResolver</code> to look
 *
 * @author mrieser
 */
public abstract class MatsimXmlParser extends DefaultHandler {

	private final Stack<StringBuffer> buffers = new Stack<StringBuffer>();
	private final Stack<String> context = new Stack<String>();

	private boolean isValidating = true;
	private boolean isNamespaceAware = true;

	private String doctype = null;
	/**
	 * As the mechanism implemented in InputSource is not really working for error handling
	 * the source to be parsed is stored here for error handling.
	 */
	private String source;

	/**
	 * Creates a validating XML-parser.
	 */
	public MatsimXmlParser() {
		this(true);
	}

	public MatsimXmlParser(final boolean validateXml) {
		this.isValidating = validateXml;
	}

	/**
	 * Called for each opening xml-tag.
	 *
	 * @param name the name of the xml-tag
	 * @param atts the list of attributes and their values
	 * @param context a stack containing the path/hierarchy to the current tag
	 */
	public abstract void startTag(String name, Attributes atts, Stack<String> context);

	/**
	 * Called for each closing xml-tag.
	 *
	 * @param name the name of the xml-tag.
	 * @param content the character-content of the tag; any characters between <code>&lt;tag&gt;</code> and
	 * 		<code>&lt;/tag&gt;></code>, excluding other tags and their content.
	 * @param context a stack containing the path/hierarchy to the current tag
	 */
	public abstract void endTag(String name, String content, Stack<String> context);

	/**
	 * Sets, if this parser should validate the read XML or not. Not validating is sometimes useful during development or
	 * during some tests with format-extensions that are not yet part of the DTD, but it is <b>strongly discouraged</b> not
	 * to validate during production use.
	 *
	 * @param validateXml Whether the parsed XML should be validated or not.
	 */
	public void setValidating(final boolean validateXml) {
		this.isValidating = validateXml;
	}

	/**
	 * Specifies that the parser produced by this code will provide support for XML namespaces.
	 * By default the value of this is set to <code>false</code>.
	 *
	 * @param awareness true if the parser produced by this code will provide support for XML namespaces; false otherwise.
	 * @see{javax.xml.parsers.SAXParserFactory.setNamespaceAware(boolean)}
	 */
	public void setNamespaceAware(final boolean awareness) {
		this.isNamespaceAware = awareness;
	}

	/**
	 * Parses the specified file. The file can be gzip-compressed and is decompressed on-the-fly while parsing. A gzip-compressed
	 * file must have the ending ".gz" to be correctly recognized. The passed filename may or may not contain the ending ".gz". If
	 * no uncompressed file is found with the specified name, the ending ".gz" will be added  ot the filename and a compressed file
	 * will be searched for and read if found.
	 *
	 * @param filename The filename of the file to read, optionally ending with ".gz" to force reading a gzip-compressed file.
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws IOException
	 */
	public void parse(final String filename) throws SAXException, ParserConfigurationException, IOException {
		this.source = filename;
		parse(new InputSource(IOUtils.getBufferedReader(filename)));
	}

	public void parse(final URL url) throws SAXException, ParserConfigurationException, IOException {
		this.source = url.toString();
		parse(new InputSource(url.toExternalForm()));
	}

	protected void parse(final InputSource input) throws SAXException, ParserConfigurationException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(this.isValidating);
		factory.setNamespaceAware(this.isNamespaceAware);
		if (this.isValidating) {
			// enable optional support for XML Schemas
			factory.setFeature("http://apache.org/xml/features/validation/schema", true);
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(this);
			reader.setErrorHandler(getErrorHandler());
			reader.setEntityResolver(getEntityResolver());
			reader.parse(input);
		} else {
			SAXParser parser = factory.newSAXParser();
			parser.parse(input, this);
		}
	}

	protected ErrorHandler getErrorHandler() {
		return this;
	}

	protected EntityResolver getEntityResolver() {
		return this;
	}

	public final String getDoctype() {
		return this.doctype;
	}

	protected void setDoctype(final String doctype) {
		this.doctype = doctype;
	}

	/* implement EntityResolver */

	@Override
	public InputSource resolveEntity(final String publicId, final String systemId) {
		// extract the last part of the systemId
		int index = systemId.replace('\\', '/').lastIndexOf("/");
		String shortSystemId = systemId.substring(index + 1);

		if (this.doctype == null) {
			// this is the first systemId we have to resolve, assume it's the doctype
			// I haven't found any other way to determine the doctype of the currently read file
			setDoctype(shortSystemId);
		}

		// try to get the dtd from the web
		try {
			InputStream is = new URL(systemId).openStream();
			/* If there was no exception until here, than the path is valid.
			 * Return the opened stream as a source. If we would return null, then the SAX-Parser
			 * would have to fetch the same file again, requiring two accesses to the webserver */
			return new InputSource(is);
		} catch (IOException e) {
			// There was a problem getting the (remote) file, just show the error as information for the user
			Logger.getLogger(this.getClass()).error(e.toString() + ". May not be fatal." ) ;
		}

		// systemId could not be resolved, try it locally
		String localFileName = Gbl.getConfig().global().getLocalDtdBase() + shortSystemId;
		File dtdFile = new File(localFileName);
		if (dtdFile.exists() && dtdFile.isFile() && dtdFile.canRead()) {
			Logger.getLogger(this.getClass()).info("Using the local DTD " + localFileName);
			return new InputSource(localFileName);
		}

		// We could neither get the remote nor the local version of the dtd, show a warning
		Logger.getLogger(this.getClass()).warn("Could neither get the DTD from the web nor the local one. " + systemId);
		return null;
	}

	/* implement ContentHandler */

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
		StringBuffer buffer = this.buffers.peek();
		if (buffer != null) {
			buffer.append(ch, start, length);
		}
	}

	@Override
	public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {
		String tag = (uri.length() == 0) ? qName : localName;
		this.buffers.push(new StringBuffer());
		this.startTag(tag, atts, this.context);
		this.context.push(tag);
	}

	@Override
	public void endElement(final String uri, final String localName, final String qName) throws SAXException {
		String tag = (uri.length() == 0) ? qName : localName;
		this.context.pop();
		StringBuffer buffer = this.buffers.pop();
		this.endTag(tag, buffer.toString(), this.context);
	}

	/* implement ErrorHandler */

	@Override
	public void error(final SAXParseException ex) throws SAXException {
		if (this.context.isEmpty()) {
			System.err.println("Missing DOCTYPE.");
		}
		System.err.println("XML-ERROR: " + getInputSource(ex) + ", line " + ex.getLineNumber() + ", column " + ex.getColumnNumber() + ":");
		System.err.println(ex.toString());
		throw ex;
	}

	@Override
	public void fatalError(final SAXParseException ex) throws SAXException {
		System.err.println("XML-FATAL: " + getInputSource(ex) + ", line " + ex.getLineNumber() + ", column " + ex.getColumnNumber() + ":");
		System.err.println(ex.toString());
		throw ex;
	}

	@Override
	public void warning(final SAXParseException ex) throws SAXException {
		System.err.println("XML-WARNING: " + getInputSource(ex) + ", line " + ex.getLineNumber() + ", column " + ex.getColumnNumber() + ":");
		System.err.println(ex.getMessage());
	}

	private String getInputSource(SAXParseException ex) {
		if (ex.getSystemId() != null) {
			return ex.getSystemId();
		}
		else if (ex.getPublicId() != null) {
			return ex.getPublicId();
		}
		//try to use the locally stored inputSource
		return this.source;
	}

}
