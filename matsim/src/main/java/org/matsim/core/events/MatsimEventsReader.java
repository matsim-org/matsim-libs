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
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A reader for events-files of MATSim. This reader recognizes the format of the events-file and uses
 * the correct reader for the specific events-version, without manual setting.
 *
 * @author mrieser
 */
public final class MatsimEventsReader implements MatsimReader {

	private final static Logger log = LogManager.getLogger(MatsimEventsReader.class);
	private final EventsManager events;

	private final Map<String, CustomEventMapper> customEventMappers = new LinkedHashMap<>();

	public interface CustomEventMapper {
		Event apply(GenericEvent event);
	}

	public void addCustomEventMapper(String eventType, CustomEventMapper mapper) {
		this.customEventMappers.put(eventType, mapper);
	}

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
	@Override
	public void readFile(final String filename) {
		String lcFilename = filename.toLowerCase(Locale.ROOT);
		if (lcFilename.endsWith(".xml") || lcFilename.endsWith(".xml.gz") || lcFilename.endsWith(".xml.zst") || lcFilename.endsWith(".xml.lz4")) {
			new XmlEventsReader(this.events, this.customEventMappers).readFile(filename );
		} else if (lcFilename.endsWith(".ndjson") || lcFilename.endsWith(".ndjson.gz") || lcFilename.endsWith(".ndjson.zst")) {
			EventsReaderJson reader = new EventsReaderJson(this.events);
			customEventMappers.forEach(reader::addCustomEventMapper);
			reader.parse(filename);
		} else if (lcFilename.endsWith(".txt") || lcFilename.endsWith(".txt.gz")) {
			throw new RuntimeException("text events are no longer supported. Please use MATSim 0.6.1 or earlier to read text events.");
		} else {
			throw new IllegalArgumentException("Cannot recognize the format of the events-file " + filename);
		}
	}

	public void readStream(final InputStream stream, final ControllerConfigGroup.EventsFileFormat format) {
		switch (format) {
			case xml:
				new XmlEventsReader(this.events, this.customEventMappers).parse(stream);
				break;
			case pb:
				throw new UnsupportedOperationException(
						"PB (Protobuf) is currently not supported to read from a stream");
			case json:
				EventsReaderJson reader = new EventsReaderJson(this.events);
				customEventMappers.forEach(reader::addCustomEventMapper);
				reader.parse(stream);
				break;
		}
	}

	@Override
	public void readURL( final URL url ) {
		if (url.getFile().contains(".xml")) {
			new XmlEventsReader( this.events, this.customEventMappers).readURL( url );
		} else if (url.getFile().contains(".ndjson")) {
			EventsReaderJson reader = new EventsReaderJson(this.events);
			customEventMappers.forEach(reader::addCustomEventMapper);
			reader.parse(url);
		}
	}

	private static class XmlEventsReader extends MatsimXmlParser {

		final EventsManager events;
		private final static String EVENTS_V1 = "events_v1.dtd";
		private MatsimXmlEventsParser delegate = null;

		private final Map<String, CustomEventMapper> map ;

		private XmlEventsReader( final EventsManager events, Map<String, CustomEventMapper> map ) {
			super(ValidationType.NO_VALIDATION);
			this.events = events;
			this.map = map;
			this.setValidating(false); // events-files have no DTD, thus they cannot validate
			setDoctype("events_v1.dtd"); // manually set a doctype, otherwise delegate would not be initialized
		}

		@Override
		public void startTag(final String name, final Attributes atts, final Stack<String> context) {
			this.delegate.startTag(name, atts, context);
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
//			this.delegate.characters(ch, start, length);
			// ignore characters to prevent OutOfMemoryExceptions
			/* the events-file only contains empty tags with attributes,
			 * but without the dtd or schema, all whitespace between tags is handled
			 * by characters and added up by super.characters, consuming huge
			 * amount of memory when large events-files are read in.
			 */
		}

		@Override
		public void endTag(final String name, final String content, final Stack<String> context) {
			this.delegate.endTag(name, content, context);
		}

		@Override
		protected void setDoctype(final String doctype) {
			super.setDoctype(doctype);
			// Currently the only events-type is v1
			if (EVENTS_V1.equals(doctype)) {
				this.delegate = new EventsReaderXMLv1(this.events);
				map.forEach(delegate::addCustomEventMapper);
				log.info("using events_v1-reader.");
			} else {
				throw new IllegalArgumentException("Doctype \"" + doctype + "\" not known.");
			}
		}
	}
}
