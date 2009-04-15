/* *********************************************************************** *
 * project: org.matsim.*
 * AgentMoneyEventTest.java
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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentMoneyEvent;
import org.matsim.core.events.BasicEventImpl;
import org.matsim.core.events.Events;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class AgentMoneyEventIntegrationTest extends MatsimTestCase {

	public void testWriteReadTxt() {
		final AgentMoneyEvent event1 = new AgentMoneyEvent(7.0*3600, new IdImpl(1), 2.34);
		final AgentMoneyEvent event2 = new AgentMoneyEvent(8.5*3600, new IdImpl(2), -3.45);

		// write some events to file

		final String eventsFilename = getOutputDirectory() + "events.txt";

		Events writeEvents = new Events();
		EventWriterTXT writer = new EventWriterTXT(eventsFilename);
		writeEvents.addHandler(writer);

		writeEvents.processEvent(event1);
		writeEvents.processEvent(event2);

		writer.closeFile();

		// read the events from file

		Events readEvents = new Events();
		EventCollector collector = new EventCollector();
		readEvents.addHandler(collector);
		MatsimEventsReader reader = new MatsimEventsReader(readEvents);
		reader.readFile(eventsFilename);

		// compare the read events with the one written

		assertEquals(2, collector.events.size());

		assertTrue(collector.events.get(0) instanceof AgentMoneyEvent);
		AgentMoneyEvent e1 = (AgentMoneyEvent) collector.events.get(0);
		assertEquals(event1.getTime(), e1.getTime(), EPSILON);
		assertEquals(event1.getPersonId().toString(), e1.getPersonId().toString());
		assertEquals(event1.getAmount(), e1.getAmount(), EPSILON);

		assertTrue(collector.events.get(1) instanceof AgentMoneyEvent);
		AgentMoneyEvent e2 = (AgentMoneyEvent) collector.events.get(1);
		assertEquals(event2.getTime(), e2.getTime(), EPSILON);
		assertEquals(event2.getPersonId().toString(), e2.getPersonId().toString());
		assertEquals(event2.getAmount(), e2.getAmount(), EPSILON);
	}

	public void testWriteReadXxml() {
		final AgentMoneyEvent event1 = new AgentMoneyEvent(7.0*3600, new IdImpl(1), 2.34);
		final AgentMoneyEvent event2 = new AgentMoneyEvent(8.5*3600, new IdImpl(2), -3.45);

		// write some events to file

		final String eventsFilename = getOutputDirectory() + "events.xml";

		Events writeEvents = new Events();
		EventWriterXML writer = new EventWriterXML(eventsFilename);
		writeEvents.addHandler(writer);

		writeEvents.processEvent(event1);
		writeEvents.processEvent(event2);

		writer.closeFile();

		// read the events from file

		Events readEvents = new Events();
		EventCollector collector = new EventCollector();
		readEvents.addHandler(collector);
		MatsimEventsReader reader = new MatsimEventsReader(readEvents);
		reader.readFile(eventsFilename);

		// compare the read events with the one written

		assertEquals(2, collector.events.size());

		assertTrue(collector.events.get(0) instanceof AgentMoneyEvent);
		AgentMoneyEvent e1 = (AgentMoneyEvent) collector.events.get(0);
		assertEquals(event1.getTime(), e1.getTime(), EPSILON);
		assertEquals(event1.getPersonId().toString(), e1.getPersonId().toString());
		assertEquals(event1.getAmount(), e1.getAmount(), EPSILON);

		assertTrue(collector.events.get(1) instanceof AgentMoneyEvent);
		AgentMoneyEvent e2 = (AgentMoneyEvent) collector.events.get(1);
		assertEquals(event2.getTime(), e2.getTime(), EPSILON);
		assertEquals(event2.getPersonId().toString(), e2.getPersonId().toString());
		assertEquals(event2.getAmount(), e2.getAmount(), EPSILON);
	}

	/*package*/ static class EventCollector implements BasicEventHandler {
		final protected List<BasicEventImpl> events = new ArrayList<BasicEventImpl>();

		public void handleEvent(BasicEventImpl event) {
			this.events.add(event);
		}

		public void reset(int iteration) {
		}

	}
}
