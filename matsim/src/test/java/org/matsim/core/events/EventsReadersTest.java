/* *********************************************************************** *
 * project: org.matsim.*
 * EventsReadersTest.java
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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.Wait2LinkEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.Wait2LinkEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

public class EventsReadersTest extends MatsimTestCase {


	static class TestHandler implements ActivityEndEventHandler, PersonDepartureEventHandler, Wait2LinkEventHandler,
			LinkLeaveEventHandler, LinkEnterEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler,
			PersonStuckEventHandler {

		public int eventCounter = 0;

		@Override
		public void reset(final int iteration) {
			this.eventCounter = 0;
		}

		@Override
		public void handleEvent(final ActivityEndEvent event) {
			this.eventCounter++;
			assertEquals("expected activity-End-Event to be event #1", 1, this.eventCounter);
			assertEquals(21610.0, event.getTime(), 0.0);
			assertEquals("1", event.getPersonId().toString());
			assertEquals(Id.create("2", Link.class), event.getLinkId());
		}

		@Override
		public void handleEvent(final PersonDepartureEvent event) {
			this.eventCounter++;
			assertEquals("expected agentDeparture-Event to be event #2", 2, this.eventCounter);
			assertEquals(21620.0, event.getTime(), 0.0);
			assertEquals("2", event.getPersonId().toString());
			assertEquals("3", event.getLinkId().toString());
		}

		@Override
		public void handleEvent(final Wait2LinkEvent event) {
			this.eventCounter++;
			assertEquals("expected wait2link-Event to be event #3", 3, this.eventCounter);
			assertEquals(21630.0, event.getTime(), 0.0);
			assertEquals("3", event.getPersonId().toString());
			assertEquals("4", event.getLinkId().toString());
		}

		@Override
		public void handleEvent(final LinkLeaveEvent event) {
			this.eventCounter++;
			assertEquals("expected linkleave-Event to be event #4", 4, this.eventCounter);
			assertEquals(21640.0, event.getTime(), 0.0);
			assertEquals("4", event.getDriverId().toString());
			assertEquals("5", event.getLinkId().toString());
		}

		@Override
		public void handleEvent(final LinkEnterEvent event) {
			this.eventCounter++;
			assertEquals("expected linkleave-Event to be event #5", 5, this.eventCounter);
			assertEquals(21650.0, event.getTime(), 0.0);
			assertEquals("5", event.getDriverId().toString());
			assertEquals("6", event.getLinkId().toString());
		}

		@Override
		public void handleEvent(final PersonArrivalEvent event) {
			this.eventCounter++;
			assertEquals("expected agentArrival-Event to be event #6", 6, this.eventCounter);
			assertEquals(21660.0, event.getTime(), 0.0);
			assertEquals("6", event.getPersonId().toString());
			assertEquals("7", event.getLinkId().toString());
		}

		@Override
		public void handleEvent(final ActivityStartEvent event) {
			this.eventCounter++;
			assertEquals("expected activityStart-Event to be event #7", 7, this.eventCounter);
			assertEquals(21670.0, event.getTime(), 0.0);
			assertEquals("7", event.getPersonId().toString());
			assertEquals(Id.create("8", Link.class), event.getLinkId());
		}

		@Override
		public void handleEvent(final PersonStuckEvent event) {
			this.eventCounter++;
			assertEquals("expected agentStuck-Event to be event #8", 8, this.eventCounter);
			assertEquals(21680.0, event.getTime(), 0.0);
			assertEquals("8", event.getPersonId().toString());
			assertEquals("9", event.getLinkId().toString());
		}

	}

	public final void testXmlReader() throws SAXException, ParserConfigurationException, IOException {
		EventsManager events = EventsUtils.createEventsManager();
		TestHandler handler = new TestHandler();
		events.addHandler(handler);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(getClassInputDirectory() + "events.xml");
		assertEquals("number of read events", 8, handler.eventCounter);
	}

	public final void testAutoFormatReaderXml() {
		EventsManager events = EventsUtils.createEventsManager();
		TestHandler handler = new TestHandler();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(getClassInputDirectory() + "events.xml");
		assertEquals("number of read events", 8, handler.eventCounter);
	}
}