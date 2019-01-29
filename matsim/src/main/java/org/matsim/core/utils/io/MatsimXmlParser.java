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

package org.matsim.core.utils.io;

import org.apache.log4j.Logger;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.gbl.Gbl;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Stack;
import java.util.zip.GZIPInputStream;

/**
 * An abstract XML-Parser which can be easily extended for reading custom XML-formats. This class handles all the low level
 * functionality required to parse xml-files. Extending classes have only to implement {@link #startTag} and {@link #endTag}
 * to implement a custom parser.<br>
 * The parser implements a custom <code>EntityResolver</code> to look for DTDs in the MATSim world.
 * <p></p>
 * Notes:<ul>
 * <li> If implementing classes want to override the final methods, the will have to resort to delegation.   
 * </ul>
 *
 * @author mrieser
 */
public abstract class MatsimXmlParser extends DefaultHandler implements MatsimReader {

	private static final Logger log = Logger.getLogger(MatsimXmlParser.class);

	private final Stack<StringBuffer> buffers = new Stack<>();
	private final Stack<String> theContext = new Stack<>();

	private boolean isValidating = true;
	private boolean isNamespaceAware = true;

	private String localDtdBase = null;
	// yy this is NOT working for me with "dtd", but it IS working with null. 
	// Note that I am typically NOT running java from the root of the classpath. kai, mar'15
	
	private boolean preferLocalDtds = false;

	private String doctype = null;
	/**
	 * As the mechanism implemented in InputSource is not really working for error handling
	 * the source to be parsed is stored here for error handling.
	 */
	private String theSource;

	/**
	 * Creates a validating XML-parser.
	 */
	public MatsimXmlParser() {
		String localDtd = System.getProperty("matsim.preferLocalDtds");
		if (localDtd != null) {
			this.preferLocalDtds = Boolean.parseBoolean(localDtd);
		}
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
	public final void setValidating(final boolean validateXml) {
		this.isValidating = validateXml;
	}

	/**
	 * Specifies that the parser produced by this code will provide support for XML namespaces.
	 * By default the value of this is set to <code>false</code>.
	 *
	 * @param awareness true if the parser produced by this code will provide support for XML namespaces; false otherwise.
	 * @see{javax.xml.parsers.SAXParserFactory.setNamespaceAware(boolean)}
	 */
	public final void setNamespaceAware(final boolean awareness) {
		this.isNamespaceAware = awareness;
	}

	/**
	 * Sets the directory where to look for DTD and XSD files if they are not found
	 * at the location specified in the XML.
	 *
	 * @param localDtdDirectory
	 */
	public final void setLocalDtdDirectory(final String localDtdDirectory) {
		this.localDtdBase = localDtdDirectory;
	}

	/**
	 * Parses the specified file. The file can be gzip-compressed and is decompressed on-the-fly while parsing. A gzip-compressed
	 * file must have the ending ".gz" to be correctly recognized. The passed filename may or may not contain the ending ".gz". If
	 * no uncompressed file is found with the specified name, the ending ".gz" will be added to the filename and a compressed file
	 * will be searched for and read if found.
	 *
	 * @param filename The filename of the file to read, optionally ending with ".gz" to force reading a gzip-compressed file.
	 * @throws UncheckedIOException
	 */
	@Override
	public final void readFile(final String filename) throws UncheckedIOException {
		log.info("starting to parse xml from file " + filename + " ...");
		this.theSource = filename;
		parse(new InputSource(IOUtils.getBufferedReader(filename)));
	}

	@Override
	public final void readURL( final URL url ) throws UncheckedIOException {
		parse( url ) ;
	}

	public final void parse(final URL url) throws UncheckedIOException {
		Gbl.assertNotNull(url);
		this.theSource = url.toString();
		log.info("starting to parse xml from url " + this.theSource + " ...");
		System.out.flush();
		if (url.getFile().endsWith(".gz")) {
			try {
				parse(new InputSource(new GZIPInputStream(url.openStream())));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			parse(new InputSource(url.toExternalForm()));
		}
	}

	public final void parse(final InputStream stream) throws UncheckedIOException {
		this.theSource = "stream";
		parse(new InputSource(stream));
	}

	public final void parse(final InputSource input) throws UncheckedIOException {
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(this.isValidating);
			factory.setNamespaceAware(this.isNamespaceAware);
			if (this.isValidating) {
				// enable optional support for XML Schemas
				factory.setFeature("http://apache.org/xml/features/validation/schema", true);
				SAXParser parser = factory.newSAXParser();
				XMLReader reader = parser.getXMLReader();
				reader.setContentHandler(this);
//				reader.setErrorHandler(getErrorHandler());      // (**)
//				reader.setEntityResolver(getEntityResolver()); // (**)
				reader.setErrorHandler(this);
				reader.setEntityResolver(this);
				reader.parse(input);
			} else {
				SAXParser parser = factory.newSAXParser();
				parser.parse(input, this);
			}
		} catch (SAXException | ParserConfigurationException | IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// the following may be useful.  But it is nowhere used, so I am not sure if we fully understand its longterm maintenance implications, 
	// so I rather comment it out. If it is needed somewhere, just comment it back in (and probably (**) above) 
	// and leave a comment.  kai, jul'16
//	protected ErrorHandler getErrorHandler() {
//		return this;
//	}
//
//	protected EntityResolver getEntityResolver() {
//		return this;
//	}

	public final String getDoctype() {
		return this.doctype;
	}

	protected void setDoctype(final String doctype) {
		// implementation of this method is what reacts to the different version of the file formats, so we cannot make it final. kai, jul'16
		
		this.doctype = doctype;
	}

	/* implement EntityResolver */

	@Override
	public final InputSource resolveEntity(final String publicId, final String systemId) {
		// ConfigReader* did override this.  Not sure if it did that for good reaons.  kai, jul'16
		
		// extract the last part of the systemId
		int index = systemId.replace('\\', '/').lastIndexOf('/');
		String shortSystemId = systemId.substring(index + 1);

		if (this.doctype == null) {
			// this is the first systemId we have to resolve, assume it's the doctype
			// I haven't found any other way to determine the doctype of the currently read file
			setDoctype(shortSystemId);
		}

		InputSource source;
		if (this.preferLocalDtds) {
			source = findDtdInLocalFilesystem(shortSystemId);
			if (source == null) {
				source = findDtdInClasspath(shortSystemId);
			}
			if (source == null) {
				source = findDtdInDefaultLocation(shortSystemId);
			}
			if (source == null) {
				source = findDtdInRemoteLocation(systemId);
			}
		} else {
			source = findDtdInRemoteLocation(systemId);
			if (source == null) {
				source = findDtdInLocalFilesystem(shortSystemId);
			}
			if (source == null) {
				source = findDtdInClasspath(shortSystemId);
			}
			if (source == null) {
				source = findDtdInDefaultLocation(shortSystemId);
			}
		}

		if (source == null) {
			// We could neither get the remote nor the local version of the dtd, show a warning
			log.warn("Could neither get the DTD from the web nor a local one. " + systemId);
		} else {
            source.setSystemId(systemId);
        }
		return source;
    }

	private static InputSource findDtdInRemoteLocation(final String fullSystemId) {
		log.info("Trying to load " + fullSystemId + ". In some cases (e.g. network interface up but no connection), this may take a bit.");
		try {
			URL url = new URL(fullSystemId);
			URLConnection urlConn = url.openConnection();
			urlConn.setConnectTimeout(5000);
			urlConn.setReadTimeout(5000);
			urlConn.setAllowUserInteraction(false);         
			
			InputStream is = urlConn.getInputStream();
			/* If there was no exception until here, than the path is valid.
			 * Return the opened stream as a source. If we would return null, then the SAX-Parser
			 * would have to fetch the same file again, requiring two accesses to the webserver */
			return new InputSource(is);
		} catch (IOException e) {
			// There was a problem getting the (remote) file, just show the error as information for the user
			log.info(e.toString() + ". May not be fatal, will try to load it locally.");
		}
		return null;
	}
	
	private InputSource findDtdInLocalFilesystem(final String shortSystemId) {
		if (this.localDtdBase != null) {
			String localFileName = this.localDtdBase + "/" + shortSystemId;
			File dtdFile = new File(localFileName);
//			log.debug("dtdfile: " + dtdFile.getAbsolutePath());
			if (dtdFile.exists() && dtdFile.isFile() && dtdFile.canRead()) {
				log.info("Using the local DTD " + localFileName + " with absolute path " + dtdFile.getAbsolutePath() );
				return new InputSource(dtdFile.getAbsolutePath());
			}
		}
		return null;
	}
	
	private InputSource findDtdInClasspath(final String shortSystemId) {
		// still no success, try to load it with the ClassLoader, in case we're stuck in a jar...
		InputStream stream = this.getClass().getResourceAsStream("/dtd/" + shortSystemId);
		if (stream != null) {
			log.info("Using local DTD from classpath:dtd/" + shortSystemId);
			return new InputSource(stream);
		}
		return null;
	}
	
	private static InputSource findDtdInDefaultLocation(final String shortSystemId) {
		log.info("Trying to access local dtd folder at standard location ./dtd...");
		File dtdFile = new File("./dtd/" + shortSystemId);
		if (dtdFile.exists() && dtdFile.isFile() && dtdFile.canRead()) {
			log.info("Using the local DTD " + dtdFile.getAbsolutePath());
			return new InputSource(dtdFile.getAbsolutePath());
		}
		return null;
	}

	/* implement ContentHandler */

	@Override
	public void characters(final char[] ch, final int start, final int length) throws SAXException {
		// has to be non-final since otherwise the events parser does not work.  Probably ok (this here is just a default implementation). kai, jul'16
		
		StringBuffer buffer = this.buffers.peek();
		if (buffer != null) {
			buffer.append(ch, start, length);
		}
	}

	@Override
	public final void startElement(final String uri, final String localName, final String qName, Attributes atts) throws SAXException {
		// I have not good intuition if making this one non-final might be ok.  kai, jul'16

		String tag = (uri.length() == 0) ? qName : localName;
		this.buffers.push(new StringBuffer());
		this.startTag(tag, atts, this.theContext);
		this.theContext.push(tag);
	}

	@Override
	public final void endElement(final String uri, final String localName, final String qName) throws SAXException {
		// I have not good intuition if making this one non-final might be ok.  kai, jul'16
		
		String tag = (uri.length() == 0) ? qName : localName;
		this.theContext.pop();
		StringBuffer buffer = this.buffers.pop();
		this.endTag(tag, buffer.toString(), this.theContext);
	}

	/* implement ErrorHandler */

	@Override
	public final void error(final SAXParseException ex) throws SAXException {
		if (this.theContext.isEmpty()) {
			System.err.println("Missing DOCTYPE.");
		}
		System.err.println("XML-ERROR: " + getInputSource(ex) + ", line " + ex.getLineNumber() + ", column " + ex.getColumnNumber() + ":");
		System.err.println(ex.toString());
		throw ex;
	}

	@Override
	public final void fatalError(final SAXParseException ex) throws SAXException {
		System.err.println("XML-FATAL: " + getInputSource(ex) + ", line " + ex.getLineNumber() + ", column " + ex.getColumnNumber() + ":");
		System.err.println(ex.toString());
		throw ex;
	}

	@Override
	public final void warning(final SAXParseException ex) throws SAXException {
		System.err.println("XML-WARNING: " + getInputSource(ex) + ", line " + ex.getLineNumber() + ", column " + ex.getColumnNumber() + ":");
		System.err.println(ex.getMessage());
	}

	private String getInputSource(final SAXParseException ex) {
		System.out.println(ex.getPublicId());
		System.out.println(ex.getSystemId());
		System.out.println(ex.getCause());
		System.out.println(ex.getLocalizedMessage());
		System.out.println(ex.getMessage());
		if (ex.getSystemId() != null) {
			return ex.getSystemId();
		}
		else if (ex.getPublicId() != null) {
			return ex.getPublicId();
		}
		//try to use the locally stored inputSource
		return this.theSource;
	}

}
