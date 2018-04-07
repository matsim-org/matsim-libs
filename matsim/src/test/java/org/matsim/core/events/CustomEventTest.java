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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;

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
	public void testCustomEventCanBeWrittenAndRead() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		EventsManager eventsManager1 = EventsUtils.createEventsManager();
		EventWriterXML handler = new EventWriterXML(ps);
		eventsManager1.addHandler(handler);
		eventsManager1.processEvent(new RainOnPersonEvent(0, Id.createPersonId("wurst")));
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
		EventsReaderXMLv1 eventsReaderXMLv1 = new EventsReaderXMLv1(eventsManager2);
		eventsReaderXMLv1.addCustomEventMapper("rain", new EventsReaderXMLv1.CustomEventMapper() {
			@Override
			public Event apply(GenericEvent event) {
				return new RainOnPersonEvent(event.getTime(), Id.createPersonId(event.getAttributes().get("person")));
			}
		});
		eventsReaderXMLv1.parse(new ByteArrayInputStream(buf));
		Assert.assertEquals(1, oneEvent.size());
		Event event = oneEvent.get(0);
		Assert.assertTrue(event instanceof RainOnPersonEvent);
		RainOnPersonEvent ropEvent = ((RainOnPersonEvent) event);
		Assert.assertEquals(0.0, ropEvent.getTime(), 1e-7);
		Assert.assertEquals(Id.createPersonId("wurst"), ropEvent.getPersonId());
	}

}
