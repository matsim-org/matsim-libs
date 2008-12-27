/* *********************************************************************** *
 * project: org.matsim.*
 * XmlEventsTester.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import junit.framework.TestCase;

import org.matsim.events.algorithms.EventWriterXML;
import org.matsim.events.handler.BasicEventHandler;

/**
 * An abstract class providing static methods to verify that events are correctly
 * handled with the XML-events-file-format.
 *
 * @author mrieser
 */
public abstract class XmlEventsTester extends TestCase {

	/**
	 * Writes out the given event to the specified file in the XML format,
	 * then reads the file again and makes sure the freshly read in event
	 * has the same attributes as the original event.
	 *
	 * @param <T> the type/class of the event
	 * @param eventsFile filename where to write the event into
	 * @param event the event to test
	 * @return the read-in event
	 */
	public static <T extends BasicEvent> T testWriteReadXml(final String eventsFile, final T event) {
		EventWriterXML writer = new EventWriterXML(eventsFile);
		writer.handleEvent(event);
		writer.closeFile();
		assertTrue(new File(eventsFile).exists());

		Events events = new Events();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		new MatsimEventsReader(events).readFile(eventsFile);

		assertEquals("there must be 1 event.", 1, collector.events.size());
		BasicEvent readEvent = collector.events.iterator().next();
		assertEquals("event has wrong class.", event.getClass(), readEvent.getClass());

		Map<String, String> writtenAttributes = event.getAttributes();
		Map<String, String> readAttributes = readEvent.getAttributes();
		for (Map.Entry<String, String> attribute : writtenAttributes.entrySet()) {
			assertEquals("attribute '" + attribute.getKey() + "' is different after reading the event.",
					attribute.getValue(), readAttributes.get(attribute.getKey()));
		}

		return (T) readEvent;
	}

	/**
	 * Helper class that stores all handled events in a collection.
	 *
	 * @author mrieser
	 */
	/*package*/ static class EventsCollector implements BasicEventHandler {
		/*package*/ final Collection<BasicEvent> events = new LinkedList<BasicEvent>();

		public void handleEvent(final BasicEvent event) {
			this.events.add(event);
		}

		public void reset(final int iteration) {
			this.events.clear();
		}
	}
}
