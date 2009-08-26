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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentMoneyEventImpl;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterTXT;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.EventsCollector;

/**
 * @author mrieser
 */
public class AgentMoneyEventIntegrationTest extends MatsimTestCase {

	public void testWriteReadTxt() {
		final AgentMoneyEventImpl event1 = new AgentMoneyEventImpl(7.0*3600, new IdImpl(1), 2.34);
		final AgentMoneyEventImpl event2 = new AgentMoneyEventImpl(8.5*3600, new IdImpl(2), -3.45);

		// write some events to file

		final String eventsFilename = getOutputDirectory() + "events.txt";

		EventsImpl writeEvents = new EventsImpl();
		EventWriterTXT writer = new EventWriterTXT(eventsFilename);
		writeEvents.addHandler(writer);

		writeEvents.processEvent(event1);
		writeEvents.processEvent(event2);

		writer.closeFile();

		// read the events from file

		EventsImpl readEvents = new EventsImpl();
		EventsCollector collector = new EventsCollector();
		readEvents.addHandler(collector);
		MatsimEventsReader reader = new MatsimEventsReader(readEvents);
		reader.readFile(eventsFilename);

		// compare the read events with the one written

		assertEquals(2, collector.getEvents().size());

		assertTrue(collector.getEvents().get(0) instanceof AgentMoneyEventImpl);
		AgentMoneyEventImpl e1 = (AgentMoneyEventImpl) collector.getEvents().get(0);
		assertEquals(event1.getTime(), e1.getTime(), EPSILON);
		assertEquals(event1.getPersonId().toString(), e1.getPersonId().toString());
		assertEquals(event1.getAmount(), e1.getAmount(), EPSILON);

		assertTrue(collector.getEvents().get(1) instanceof AgentMoneyEventImpl);
		AgentMoneyEventImpl e2 = (AgentMoneyEventImpl) collector.getEvents().get(1);
		assertEquals(event2.getTime(), e2.getTime(), EPSILON);
		assertEquals(event2.getPersonId().toString(), e2.getPersonId().toString());
		assertEquals(event2.getAmount(), e2.getAmount(), EPSILON);
	}

	public void testWriteReadXxml() {
		final AgentMoneyEventImpl event1 = new AgentMoneyEventImpl(7.0*3600, new IdImpl(1), 2.34);
		final AgentMoneyEventImpl event2 = new AgentMoneyEventImpl(8.5*3600, new IdImpl(2), -3.45);

		// write some events to file

		final String eventsFilename = getOutputDirectory() + "events.xml";

		EventsImpl writeEvents = new EventsImpl();
		EventWriterXML writer = new EventWriterXML(eventsFilename);
		writeEvents.addHandler(writer);

		writeEvents.processEvent(event1);
		writeEvents.processEvent(event2);

		writer.closeFile();

		// read the events from file

		EventsImpl readEvents = new EventsImpl();
		EventsCollector collector = new EventsCollector();
		readEvents.addHandler(collector);
		MatsimEventsReader reader = new MatsimEventsReader(readEvents);
		reader.readFile(eventsFilename);

		// compare the read events with the one written

		assertEquals(2, collector.getEvents().size());

		assertTrue(collector.getEvents().get(0) instanceof AgentMoneyEventImpl);
		AgentMoneyEventImpl e1 = (AgentMoneyEventImpl) collector.getEvents().get(0);
		assertEquals(event1.getTime(), e1.getTime(), EPSILON);
		assertEquals(event1.getPersonId().toString(), e1.getPersonId().toString());
		assertEquals(event1.getAmount(), e1.getAmount(), EPSILON);

		assertTrue(collector.getEvents().get(1) instanceof AgentMoneyEventImpl);
		AgentMoneyEventImpl e2 = (AgentMoneyEventImpl) collector.getEvents().get(1);
		assertEquals(event2.getTime(), e2.getTime(), EPSILON);
		assertEquals(event2.getPersonId().toString(), e2.getPersonId().toString());
		assertEquals(event2.getAmount(), e2.getAmount(), EPSILON);
	}

}
