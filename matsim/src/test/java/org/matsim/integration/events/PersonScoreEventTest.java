/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup.EventsFileFormat;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterJson;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsCollector;

/**
 * @author mrieser / Simunto
 */
public class PersonScoreEventTest {

	@Test
	void testWriteReadXml() {
		final PersonScoreEvent event1 = new PersonScoreEvent(7.0*3600, Id.create(1, Person.class), 2.34, "act");
		final PersonScoreEvent event2 = new PersonScoreEvent(8.5*3600, Id.create(2, Person.class), -3.45, "leg");

		// write some events to stream

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		EventsManager writeEvents = EventsUtils.createEventsManager();
		EventWriterXML writer = new EventWriterXML(baos);
		writeEvents.addHandler(writer);
		writeEvents.initProcessing();

		writeEvents.processEvent(event1);
		writeEvents.processEvent(event2);

		writeEvents.finishProcessing();
		writer.closeFile();

		// read the events from stream

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

		EventsManager readEvents = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		readEvents.addHandler(collector);
		readEvents.initProcessing();
		MatsimEventsReader reader = new MatsimEventsReader(readEvents);
		reader.readStream(bais, EventsFileFormat.xml);
		readEvents.finishProcessing();

		// compare the read events with the one written

		assertEquals(2, collector.getEvents().size());

		assertTrue(collector.getEvents().get(0) instanceof PersonScoreEvent);
		PersonScoreEvent e1 = (PersonScoreEvent) collector.getEvents().get(0);
		assertEquals(event1.getTime(), e1.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(event1.getPersonId().toString(), e1.getPersonId().toString());
		assertEquals(event1.getAmount(), e1.getAmount(), MatsimTestUtils.EPSILON);
		assertEquals(event1.getKind(), e1.getKind());

		assertTrue(collector.getEvents().get(1) instanceof PersonScoreEvent);
		PersonScoreEvent e2 = (PersonScoreEvent) collector.getEvents().get(1);
		assertEquals(event2.getTime(), e2.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(event2.getPersonId().toString(), e2.getPersonId().toString());
		assertEquals(event2.getAmount(), e2.getAmount(), MatsimTestUtils.EPSILON);
		assertEquals(event2.getKind(), e2.getKind());
	}

	@Test
	void testWriteReadJson() {
		final PersonScoreEvent event1 = new PersonScoreEvent(7.0*3600, Id.create(1, Person.class), 2.34, "act");
		final PersonScoreEvent event2 = new PersonScoreEvent(8.5*3600, Id.create(2, Person.class), -3.45, "leg");

		// write some events to stream

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		EventsManager writeEvents = EventsUtils.createEventsManager();
		EventWriterJson writer = new EventWriterJson(baos);
		writeEvents.addHandler(writer);
		writeEvents.initProcessing();

		writeEvents.processEvent(event1);
		writeEvents.processEvent(event2);

		writeEvents.finishProcessing();
		writer.closeFile();

		// read the events from stream

		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

		EventsManager readEvents = EventsUtils.createEventsManager();
		EventsCollector collector = new EventsCollector();
		readEvents.addHandler(collector);
		readEvents.initProcessing();
		MatsimEventsReader reader = new MatsimEventsReader(readEvents);
		reader.readStream(bais, EventsFileFormat.json);
		readEvents.finishProcessing();

		// compare the read events with the one written

		assertEquals(2, collector.getEvents().size());

		assertTrue(collector.getEvents().get(0) instanceof PersonScoreEvent);
		PersonScoreEvent e1 = (PersonScoreEvent) collector.getEvents().get(0);
		assertEquals(event1.getTime(), e1.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(event1.getPersonId().toString(), e1.getPersonId().toString());
		assertEquals(event1.getAmount(), e1.getAmount(), MatsimTestUtils.EPSILON);
		assertEquals(event1.getKind(), e1.getKind());

		assertTrue(collector.getEvents().get(1) instanceof PersonScoreEvent);
		PersonScoreEvent e2 = (PersonScoreEvent) collector.getEvents().get(1);
		assertEquals(event2.getTime(), e2.getTime(), MatsimTestUtils.EPSILON);
		assertEquals(event2.getPersonId().toString(), e2.getPersonId().toString());
		assertEquals(event2.getAmount(), e2.getAmount(), MatsimTestUtils.EPSILON);
		assertEquals(event2.getKind(), e2.getKind());
	}

}
