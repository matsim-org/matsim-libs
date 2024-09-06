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

package org.matsim.core.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.testcases.utils.EventsCollector;

/**
 * An abstract class providing static methods to verify that events are correctly
 * handled with the XML-events-file-format.
 *
 * @author mrieser
 */
public abstract class XmlEventsTester {

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
	public static <T extends Event> T testWriteReadXml(final String eventsFile, final T event) {
		EventWriterXML writer = new EventWriterXML(eventsFile);
		writer.handleEvent(event);
		writer.closeFile();
		assertTrue(new File(eventsFile).exists());

		EventsManager events = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		events.initProcessing();
		new MatsimEventsReader(events).readFile(eventsFile);
		events.finishProcessing();

		assertEquals(1, collector.getEvents().size(), "there must be 1 event.");
		Event readEvent = collector.getEvents().iterator().next();
		assertEquals(event.getClass(), readEvent.getClass(), "event has wrong class.");

		Map<String, String> writtenAttributes = event.getAttributes();
		Map<String, String> readAttributes = readEvent.getAttributes();
		for (Map.Entry<String, String> attribute : writtenAttributes.entrySet()) {
			assertEquals(attribute.getValue(), readAttributes.get(attribute.getKey()), "attribute '" + attribute.getKey() + "' is different after reading the event.");
		}

		return (T) readEvent;
	}

}
