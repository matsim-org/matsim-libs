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

package org.matsim.core.utils.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

	private static final Logger log = LogManager.getLogger(MatsimFileTypeGuesser.class);
	/**
	 * This enum only informs about the correct container, not about the version of the input file.
	 */
	public enum FileType {Config, Network, Facilities, Population, World,
		Counts, Events, Households, TransimsVehicle, OTFVis, SignalSystems, LaneDefinitions, SignalGroups, SignalControl, AmberTimes,
		TransitSchedule, Vehicles, ObjectAttributes}

	public static final String SYSTEMIDNOTFOUNDMESSAGE = "System Id of xml document couldn't be detected. " +
	"Make sure that you try to read a xml document with a valid header. " +
	"If your header seems to be ok, make shure that there is no / at the " +
	"end of the first part of the tuple used as value for xsi:schemaLocation.";


	private FileType fileType = null;
	private String xmlPublicId = null;
	private String xmlSystemId = null;

	public MatsimFileTypeGuesser(final String fileName) throws UncheckedIOException {
		String name = fileName.toLowerCase(Locale.ROOT);
		if (name.endsWith(".xml.gz") || name.endsWith(".xml")) {
			guessFileTypeXml(fileName);
			// I think the following would also be useful for the API, but with which name?
			String shortSystemId = null;
			if (this.xmlSystemId != null) {
				 shortSystemId = this.xmlSystemId.substring(this.xmlSystemId.replace('\\', '/').lastIndexOf('/') + 1);
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
				} else if (shortSystemId.startsWith("vehicleDefinitions_")) {
					this.fileType = FileType.Vehicles;
				} else if (shortSystemId.startsWith("transitSchedule_")) {
					this.fileType = FileType.TransitSchedule;
				} else if (shortSystemId.startsWith("objectattributes_")) {
					this.fileType = FileType.ObjectAttributes;
				}
			}

		} else if (name.endsWith(".txt.gz") || name.endsWith(".txt")) {
			this.fileType = FileType.Events;
		} else if (name.endsWith(".mvi.gz") || name.endsWith(".mvi")) {
			this.fileType = FileType.OTFVis;
		} else if (name.endsWith(".veh.gz") || name.endsWith(".veh")) {
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

	private void guessFileTypeXml(final String fileName) throws UncheckedIOException {
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
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (SAXException e) {
			throw new UncheckedIOException(new IOException(e));
		} catch (ParserConfigurationException e) {
			throw new UncheckedIOException(new IOException (e));
		} catch (XMLTypeDetectionException e) {
			this.xmlPublicId = e.publicId;
			this.xmlSystemId = e.systemId;
			log.debug("Detected public id: " + this.xmlPublicId);
			log.debug("Detected system Id: " + this.xmlSystemId);
			if (e.rootTag != null) {
				log.debug("Detected root tag: " +  e.rootTag);
				if 	("events".equals(e.rootTag)) {
					this.fileType = FileType.Events;
				} else if ("signalGroups".equals(e.rootTag)) {
					this.fileType = FileType.SignalGroups;
				} else if ("signalSystems".equals(e.rootTag)) {
					this.fileType = FileType.SignalSystems;
				} else if ("signalControl".equals(e.rootTag)) {
					this.fileType = FileType.SignalControl;
				} else if ("laneDefinitions".equals(e.rootTag)) {
					this.fileType = FileType.LaneDefinitions;
				}	else if ("counts".equals(e.rootTag)) {
					this.fileType = FileType.Counts;
				} else if ("transitSchedule".equals(e.rootTag)) {
					this.fileType = FileType.TransitSchedule;
				} else if ("objectAttributes".equals(e.rootTag)) {
					this.fileType = FileType.ObjectAttributes;
				} else {
					log.warn("got unexpected rootTag: " + e.rootTag);
				}
			}
		}
	}

	private final static class XmlHandler extends DefaultHandler {

		private XMLTypeDetectionException exception;

		private boolean detectedFirstEntity = false;

		public XmlHandler() {
			// public constructor for private inner class
		}

		@Override
		public InputSource resolveEntity(final String publicId, final String systemId) {
			/*
			 * As the xml schema of interest may be derived from other schema instances we
			 * are only interested in the first entity resolved.
			 */
			if (! this.detectedFirstEntity){
				this.exception  = new XMLTypeDetectionException(publicId, systemId);
				this.detectedFirstEntity = true;
			}
			if (systemId.endsWith(".dtd")){
				throw this.exception;
			}
			return null;
		}

		@Override
		public void startElement(final String uri, final String localName, final String qName, final Attributes atts) throws SAXException {
			String tag = (uri.length() == 0) ? qName : localName;
			if (this.exception == null) {
				this.exception = new XMLTypeDetectionException(null, null);
			}
			this.exception.rootTag = tag;
			throw this.exception;
		}
	}

	/**
	 * Used to return the declared type encountered in the XML file, the root tag
	 * and to stop parsing the file
	 * @author dgrether
	 *
	 */
	private final static class XMLTypeDetectionException extends RuntimeException {

		private static final long serialVersionUID = 1L;

		public final String publicId;
		public final String systemId;
		public String rootTag;

		public XMLTypeDetectionException(final String publicId, final String systemId){
			this.publicId = publicId;
			this.systemId = systemId;
		}

		@Override
		public synchronized Throwable fillInStackTrace() {
			return this; // optimization, as we're never interested in that stack trace
		}

	}
}
