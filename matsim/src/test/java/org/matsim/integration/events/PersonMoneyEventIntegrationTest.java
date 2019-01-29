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

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.utils.EventsCollector;

/**
 * @author mrieser
 */
public class PersonMoneyEventIntegrationTest extends MatsimTestCase {

	public void testWriteReadXxml() {
		final PersonMoneyEvent event1 = new PersonMoneyEvent(7.0*3600, Id.create(1, Person.class), 2.34);
		final PersonMoneyEvent event2 = new PersonMoneyEvent(8.5*3600, Id.create(2, Person.class), -3.45);

		// write some events to file

		final String eventsFilename = getOutputDirectory() + "events.xml";

		EventsManager writeEvents = EventsUtils.createEventsManager();
		EventWriterXML writer = new EventWriterXML(eventsFilename);
		writeEvents.addHandler(writer);

		writeEvents.processEvent(event1);
		writeEvents.processEvent(event2);

		writer.closeFile();

		// read the events from file

		EventsManager readEvents = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		readEvents.addHandler(collector);
		MatsimEventsReader reader = new MatsimEventsReader(readEvents);
		reader.readFile(eventsFilename);

		// compare the read events with the one written

		assertEquals(2, collector.getEvents().size());

		assertTrue(collector.getEvents().get(0) instanceof PersonMoneyEvent);
		PersonMoneyEvent e1 = (PersonMoneyEvent) collector.getEvents().get(0);
		assertEquals(event1.getTime(), e1.getTime(), EPSILON);
		assertEquals(event1.getPersonId().toString(), e1.getPersonId().toString());
		assertEquals(event1.getAmount(), e1.getAmount(), EPSILON);

		assertTrue(collector.getEvents().get(1) instanceof PersonMoneyEvent);
		PersonMoneyEvent e2 = (PersonMoneyEvent) collector.getEvents().get(1);
		assertEquals(event2.getTime(), e2.getTime(), EPSILON);
		assertEquals(event2.getPersonId().toString(), e2.getPersonId().toString());
		assertEquals(event2.getAmount(), e2.getAmount(), EPSILON);
	}
	
	/**
	 * Originally, the events were called AgentMoneyEvents and not PersonMoneyEvents (before Oct'13).
	 * This test checks that old event files can still be parsed.
	 * @throws IOException 
	 */
	public void testWriteReadXml_oldName() throws IOException {
		
		// write some events to file
		
		final String eventsFilename = getOutputDirectory() + "events.xml";
		BufferedWriter writer = IOUtils.getBufferedWriter(eventsFilename);
		
		writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		writer.write("<events version=\"1.0\">");
		writer.write("<event time=\"25200.0\" type=\"agentMoney\" amount=\"2.34\" person=\"1\"  />");
		writer.write("<event time=\"30600.0\" type=\"agentMoney\" amount=\"-3.45\" person=\"2\"  />");
		writer.write("</events>");
		
		writer.close();

		// read the events from file
		
		EventsManager readEvents = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		readEvents.addHandler(collector);
		MatsimEventsReader reader = new MatsimEventsReader(readEvents);
		reader.readFile(eventsFilename);
		
		// compare the read events with the one written
		
		assertEquals(2, collector.getEvents().size());
		
		assertTrue(collector.getEvents().get(0) instanceof PersonMoneyEvent);
		PersonMoneyEvent e1 = (PersonMoneyEvent) collector.getEvents().get(0);
		assertEquals(25200.0, e1.getTime(), EPSILON);
		assertEquals("1", e1.getPersonId().toString());
		assertEquals(2.34, e1.getAmount(), EPSILON);
		
		assertTrue(collector.getEvents().get(1) instanceof PersonMoneyEvent);
		PersonMoneyEvent e2 = (PersonMoneyEvent) collector.getEvents().get(1);
		assertEquals(30600.0, e2.getTime(), EPSILON);
		assertEquals("2", e2.getPersonId().toString());
		assertEquals(-3.45, e2.getAmount(), EPSILON);
	}

}
