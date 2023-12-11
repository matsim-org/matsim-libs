/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * CustomEventTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.events;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.core.events.algorithms.EventWriterJson;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;

public class CustomEventTest {

	static class RainOnPersonEvent extends Event implements HasPersonId {

		private Id<Person> personId;

		public RainOnPersonEvent(double time, Id<Person> personId) {
			super(time);
			this.personId = personId;
		}

		@Override
		public Id<Person> getPersonId() {
			return personId;
		}

		@Override
		public String getEventType() {
			return "rain";
		}

		@Override
		public Map<String, String> getAttributes() {
			final Map<String, String> attributes = super.getAttributes();
			attributes.put("person", getPersonId().toString());
			return attributes;
		}

	}

	@Test
	void testCustomEventCanBeWrittenAndRead_XML() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		EventsManager eventsManager1 = EventsUtils.createEventsManager();
		EventWriterXML handler = new EventWriterXML(ps);
		eventsManager1.addHandler(handler);
		eventsManager1.initProcessing();
		eventsManager1.processEvent(new RainOnPersonEvent(0, Id.createPersonId("wurst")));
		eventsManager1.finishProcessing();
		handler.closeFile();
		byte[] buf = baos.toByteArray();
		final ArrayList<Event> oneEvent = new ArrayList<>();
		EventsManager eventsManager2 = EventsUtils.createEventsManager();
		eventsManager2.addHandler(new BasicEventHandler() {
			@Override
			public void handleEvent(Event event) {
				oneEvent.add(event);
			}

			@Override
			public void reset(int iteration) {

			}
		});
		eventsManager2.initProcessing();
		EventsReaderXMLv1 eventsReaderXMLv1 = new EventsReaderXMLv1(eventsManager2);
		eventsReaderXMLv1.addCustomEventMapper("rain", event -> new RainOnPersonEvent(event.getTime(),
				Id.createPersonId(event.getAttributes().get("person"))));
		eventsReaderXMLv1.parse(new ByteArrayInputStream(buf));
		eventsManager2.finishProcessing();
		Assertions.assertEquals(1, oneEvent.size());
		Event event = oneEvent.get(0);
		Assertions.assertTrue(event instanceof RainOnPersonEvent);
		RainOnPersonEvent ropEvent = ((RainOnPersonEvent)event);
		Assertions.assertEquals(0.0, ropEvent.getTime(), 1e-7);
		Assertions.assertEquals(Id.createPersonId("wurst"), ropEvent.getPersonId());
	}

	@Test
	void testCustomEventCanBeWrittenAndRead_Json() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		EventsManager eventsManager1 = EventsUtils.createEventsManager();
		EventWriterJson handler = new EventWriterJson(ps);
		eventsManager1.addHandler(handler);
		eventsManager1.initProcessing();
		eventsManager1.processEvent(new RainOnPersonEvent(0, Id.createPersonId("wurst")));
		eventsManager1.finishProcessing();
		handler.closeFile();
		byte[] buf = baos.toByteArray();
		final ArrayList<Event> oneEvent = new ArrayList<>();
		EventsManager eventsManager2 = EventsUtils.createEventsManager();
		eventsManager2.addHandler(new BasicEventHandler() {
			@Override
			public void handleEvent(Event event) {
				oneEvent.add(event);
			}

			@Override
			public void reset(int iteration) {

			}
		});
		eventsManager2.initProcessing();
		EventsReaderJson eventsReader = new EventsReaderJson(eventsManager2);
		eventsReader.addCustomEventMapper("rain", event -> new RainOnPersonEvent(event.getTime(),
				Id.createPersonId(event.getAttributes().get("person"))));
		eventsReader.parse(new ByteArrayInputStream(buf));
		eventsManager2.finishProcessing();
		Assertions.assertEquals(1, oneEvent.size());
		Event event = oneEvent.get(0);
		Assertions.assertTrue(event instanceof RainOnPersonEvent);
		RainOnPersonEvent ropEvent = ((RainOnPersonEvent)event);
		Assertions.assertEquals(0.0, ropEvent.getTime(), 1e-7);
		Assertions.assertEquals(Id.createPersonId("wurst"), ropEvent.getPersonId());
	}

}
