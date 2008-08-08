/* *********************************************************************** *
 * project: org.matsim.*
 * AgentUtilityEventTest.java
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

package org.matsim.integration.events;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentUtilityEvent;
import org.matsim.events.BasicEvent;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.algorithms.EventWriterTXT;
import org.matsim.events.algorithms.EventWriterXML;
import org.matsim.events.handler.BasicEventHandler;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class AgentUtilityEventIntegrationTest extends MatsimTestCase {

	public void testWriteReadTXT() {
		final AgentUtilityEvent event1 = new AgentUtilityEvent(7.0*3600, new IdImpl(1), 2.34);
		final AgentUtilityEvent event2 = new AgentUtilityEvent(8.5*3600, new IdImpl(2), -3.45);

		// write some events to file

		final String eventsFilename = getOutputDirectory() + "events.txt";

		Events writeEvents = new Events();
		EventWriterTXT writer = new EventWriterTXT(eventsFilename);
		writeEvents.addHandler(writer);

		writeEvents.processEvent(event1);
		writeEvents.processEvent(event2);

		writer.closefile();

		// read the events from file

		Events readEvents = new Events();
		EventCollector collector = new EventCollector();
		readEvents.addHandler(collector);
		MatsimEventsReader reader = new MatsimEventsReader(readEvents);
		reader.readFile(eventsFilename);

		// compare the read events with the one written

		assertEquals(2, collector.events.size());

		assertTrue(collector.events.get(0) instanceof AgentUtilityEvent);
		AgentUtilityEvent e1 = (AgentUtilityEvent) collector.events.get(0);
		assertEquals(event1.time, e1.time, EPSILON);
		assertEquals(event1.agentId, e1.agentId);
		assertEquals(event1.amount, e1.amount, EPSILON);

		assertTrue(collector.events.get(1) instanceof AgentUtilityEvent);
		AgentUtilityEvent e2 = (AgentUtilityEvent) collector.events.get(1);
		assertEquals(event2.time, e2.time, EPSILON);
		assertEquals(event2.agentId, e2.agentId);
		assertEquals(event2.amount, e2.amount, EPSILON);
	}

	public void testWriteReadXML() {
		final AgentUtilityEvent event1 = new AgentUtilityEvent(7.0*3600, new IdImpl(1), 2.34);
		final AgentUtilityEvent event2 = new AgentUtilityEvent(8.5*3600, new IdImpl(2), -3.45);

		// write some events to file

		final String eventsFilename = getOutputDirectory() + "events.xml";

		Events writeEvents = new Events();
		EventWriterXML writer = new EventWriterXML(eventsFilename);
		writeEvents.addHandler(writer);

		writeEvents.processEvent(event1);
		writeEvents.processEvent(event2);

		writer.closefile();

		// read the events from file

		Events readEvents = new Events();
		EventCollector collector = new EventCollector();
		readEvents.addHandler(collector);
		MatsimEventsReader reader = new MatsimEventsReader(readEvents);
		reader.readFile(eventsFilename);

		// compare the read events with the one written

		assertEquals(2, collector.events.size());

		assertTrue(collector.events.get(0) instanceof AgentUtilityEvent);
		AgentUtilityEvent e1 = (AgentUtilityEvent) collector.events.get(0);
		assertEquals(event1.time, e1.time, EPSILON);
		assertEquals(event1.agentId, e1.agentId);
		assertEquals(event1.amount, e1.amount, EPSILON);

		assertTrue(collector.events.get(1) instanceof AgentUtilityEvent);
		AgentUtilityEvent e2 = (AgentUtilityEvent) collector.events.get(1);
		assertEquals(event2.time, e2.time, EPSILON);
		assertEquals(event2.agentId, e2.agentId);
		assertEquals(event2.amount, e2.amount, EPSILON);
	}

	private static class EventCollector implements BasicEventHandler {
		final public List<BasicEvent> events = new ArrayList<BasicEvent>();

		public EventCollector() {
			// empty, but public constructor for private class
		}

		public void handleEvent(BasicEvent event) {
			this.events.add(event);
		}

		public void reset(int iteration) {
		}

	}
}
