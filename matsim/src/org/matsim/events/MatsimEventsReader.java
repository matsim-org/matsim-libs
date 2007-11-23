/* *********************************************************************** *
 * project: org.matsim.*
 * MatsimEventsReader.java
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

package org.matsim.events;

import java.io.IOException;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for events-files of MATSim. This reader recognizes the format of the events-file and uses
 * the correct reader for the specific events-version, without manual setting.
 *
 * @author mrieser
 */
public class MatsimEventsReader {

	private final Events events;

	/**
	 * Creates a new reader for MATSim events files.
	 *
	 * @param events The Events-object that handles the events.
	 */
	public MatsimEventsReader(final Events events) {
		this.events = events;
	}

	/**
	 * Parses the specified events file.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) {
		if (filename.endsWith(".txt") || filename.endsWith(".txt.gz")) {
			new EventsReaderTXTv1(this.events).readFile(filename);
		} else if (filename.endsWith(".xml") || filename.endsWith(".xml.gz")) {
			new XmlEventsReader(this.events).readFile(filename);
		} else {
			throw new IllegalArgumentException("Cannot recognize the format of the events-file " + filename);
		}
	}

	private static class XmlEventsReader extends MatsimXmlParser {

		final Events events;
		private final static String EVENTS_V1 = "events_v1.dtd";
		private MatsimXmlParser delegate = null;

		public XmlEventsReader(final Events events) {
			this.events = events;
			this.setValidating(false); // events-files have no DTD, thus they cannot validate
			setDoctype("events_v1.dtd"); // manually set a doctype, otherwise delegate would not be initialized
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			this.delegate.startTag(name, atts, context);
		}

		@Override
		public void endTag(final String name, final String content, final Stack<String> context) {
			this.delegate.endTag(name, content, context);
		}

		/**
		 * Parses the specified events file. This method calls {@link #parse(String)}, but handles all
		 * possible exceptions on its own.
		 *
		 * @param filename The name of the file to parse.
		 */
		public void readFile(final String filename) {
			try {
				parse(filename);
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		protected void setDoctype(final String doctype) {
			super.setDoctype(doctype);
			// Currently the only events-type is v1
			if (EVENTS_V1.equals(doctype)) {
				this.delegate = new EventsReaderXMLv1(this.events);
				System.out.println("using events_v1-reader.");
			} else {
				throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
			}
		}
	}
}
