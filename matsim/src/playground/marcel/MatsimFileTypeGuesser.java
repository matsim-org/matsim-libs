/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimFileTypeGuesser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.matsim.utils.io.IOUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Tries to figure out the file format of the given file.
 *
 * @author mrieser
 */
public class MatsimFileTypeGuesser extends DefaultHandler {

	public enum FileType {Config, Network, Facilities, Population, World, Counts, Events, Households, TransimsVehicle, OTFVis}

	private FileType fileType = null;
	private String xmlPublicId = null;
	private String xmlSystemId = null;

	public MatsimFileTypeGuesser(final String fileName) throws IOException {

		String name = fileName.toLowerCase();
		if (name.endsWith(".xml.gz") || name.toLowerCase().endsWith(".xml")) {
			guessFileTypeXml(fileName);
			// I think the following would also be useful for the API, but with which name?
			String shortSystemId = null;
			if (this.xmlSystemId != null) {
				 shortSystemId = this.xmlSystemId.substring(this.xmlSystemId.replace('\\', '/').lastIndexOf("/") + 1);
			}
			if (shortSystemId != null) {
				if (shortSystemId.startsWith("network_")) {
					this.fileType = FileType.Network;
				} else if (shortSystemId.startsWith("world_")) {
					this.fileType = FileType.World;
				} else if (shortSystemId.startsWith("plans_")) {
					this.fileType = FileType.Population;
				} else if (shortSystemId.startsWith("population_")) {
					this.fileType = FileType.Population;
				} else if (shortSystemId.startsWith("facilities_")) {
					this.fileType = FileType.Facilities;
				} else if (shortSystemId.startsWith("config_")) {
					this.fileType = FileType.Config;
				} else if (shortSystemId.startsWith("counts_")) {
					this.fileType = FileType.Counts;
				}
			}

		} else if (name.endsWith(".txt.gz") || name.toLowerCase().endsWith(".txt")) {
			this.fileType = FileType.Events;
		} else if (name.endsWith(".mvi.gz") || name.toLowerCase().endsWith(".mvi")) {
			this.fileType = FileType.OTFVis;
		} else if (name.endsWith(".veh.gz") || name.toLowerCase().endsWith(".veh")) {
			this.fileType = FileType.TransimsVehicle;
		}
	}

	public FileType getGuessedFileType() {
		return this.fileType;
	}

	/**
	 * @return if the file is an XML file, this returns the public-id of the declared type,
	 *  <code>null</code> if no type is declared or if it is not an XML file.
	 */
	public String getPublicId() {
		return this.xmlPublicId;
	}

	/**
	 * @return if the file is an XML file, this returns the system-id of the declared type,
	 *  <code>null</code> if no type is declared or if it is not an XML file.
	 */
	public String getSystemId() {
		return this.xmlSystemId;
	}

	private void guessFileTypeXml(final String fileName) throws IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		try {
			XmlHandler handler = new XmlHandler();
			InputSource input = new InputSource(IOUtils.getBufferedReader(fileName));
			factory.setFeature("http://apache.org/xml/features/validation/schema", true);
			SAXParser parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(handler);
			reader.setErrorHandler(handler);
			reader.setEntityResolver(handler);
			reader.parse(input);
			parser.parse(input, new XmlHandler());
		} catch (SAXException e) {
			throw new IOException("SAXException: " + e.getMessage());
		} catch (ParserConfigurationException e) {
			throw new IOException("ParserConfigurationException: " + e.getMessage());
		} catch (EntityException e) {
			this.xmlPublicId = e.publicId;
			this.xmlSystemId = e.systemId;
		} catch (RootTagException e) {
			if ("events".equals(e.rootTag)) {
				this.fileType = FileType.Events;
			} else {
				System.out.println("got unexpected rootTag: " + e.rootTag);
			}
		}
	}

	private final static class XmlHandler extends DefaultHandler {

		public XmlHandler() {
			// public constructor for private inner class
		}

		@Override
		public InputSource resolveEntity(final String publicId, final String systemId) {
			throw new EntityException(publicId, systemId);
		}

		@Override
		public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {
			String tag = (uri.length() == 0) ? qName : localName;
			throw new RootTagException(tag);
		}
	}

	/**
	 * Used to return the declared type encountered in the XML file and stop parsing the file.
	 *
	 * @author mrieser
	 */
	private final static class EntityException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public final String publicId;
		public final String systemId;

		public EntityException(final String publicId, final String systemId) {
			this.publicId = publicId;
			this.systemId = systemId;
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this; // optimization, as we're never interested in that stack trace
		}
	}

	/**
	 * Used to return the first tag encountered in the XML file and stop parsing the file.
	 *
	 * @author mrieser
	 */
	private final static class RootTagException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public final String rootTag;

		public RootTagException(final String rootTag) {
			this.rootTag = rootTag;
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this; // optimization, as we're never interested in that stack trace
		}
	}

}
