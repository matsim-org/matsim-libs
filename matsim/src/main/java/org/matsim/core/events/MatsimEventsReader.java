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

package org.matsim.core.events;

import java.io.InputStream;
import java.util.Locale;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.MatsimSomeReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for events-files of MATSim. This reader recognizes the format of the events-file and uses
 * the correct reader for the specific events-version, without manual setting.
 *
 * @author mrieser
 */
public class MatsimEventsReader implements MatsimSomeReader {

	private final static Logger log = Logger.getLogger(MatsimEventsReader.class);
	private final EventsManager events;

	/**
	 * Creates a new reader for MATSim events files.
	 *
	 * @param events The Events-object that handles the events.
	 */
	public MatsimEventsReader(final EventsManager events) {
		this.events = events;
	}

	/**
	 * Parses the specified events file.
	 *
	 * @param filename The name of the file to parse.
	 */
	public void readFile(final String filename) {
		String lcFilename = filename.toLowerCase(Locale.ROOT);
		if (lcFilename.endsWith(".xml") || lcFilename.endsWith(".xml.gz")) {
			new XmlEventsReader(this.events).readFile(filename);
		} else if (lcFilename.endsWith(".txt") || lcFilename.endsWith(".txt.gz")) {
			throw new RuntimeException("text events are no longer supported. Please use MATSim 0.6.1 or earlier to read text events.");
		} else {
			throw new IllegalArgumentException("Cannot recognize the format of the events-file " + filename);
		}
	}

	public void readStream(final InputStream stream) {
		new XmlEventsReader(this.events).parse(stream);
	}

	private static class XmlEventsReader extends MatsimXmlParser {

		final EventsManager events;
		private final static String EVENTS_V1 = "events_v1.dtd";
		private MatsimXmlParser delegate = null;

		public XmlEventsReader(final EventsManager events) {
			this.events = events;
			this.setValidating(false); // events-files have no DTD, thus they cannot validate
			setDoctype("events_v1.dtd"); // manually set a doctype, otherwise delegate would not be initialized
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			this.delegate.startTag(name, atts, context);
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			this.delegate.characters(ch, start, length);
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
		 * @throws UncheckedIOException
		 */
		public void readFile(final String filename) throws UncheckedIOException {
			parse(filename);
		}

		@Override
		protected void setDoctype(final String doctype) {
			super.setDoctype(doctype);
			// Currently the only events-type is v1
			if (EVENTS_V1.equals(doctype)) {
				this.delegate = new EventsReaderXMLv1(this.events);
				log.info("using events_v1-reader.");
			} else {
				throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
			}
		}
	}
}
