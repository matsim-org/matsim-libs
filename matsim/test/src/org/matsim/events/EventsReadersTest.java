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

package org.matsim.events;

import org.matsim.events.EventActivityEnd;
import org.matsim.events.EventActivityStart;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventAgentStuck;
import org.matsim.events.EventAgentWait2Link;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.events.EventsReaderTXTv1;
import org.matsim.events.EventsReaderXMLv1;
import org.matsim.events.MatsimEventsReader;
import org.matsim.events.handler.EventHandlerActivityEndI;
import org.matsim.events.handler.EventHandlerActivityStartI;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.events.handler.EventHandlerAgentWait2LinkI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.testcases.MatsimTestCase;

public class EventsReadersTest extends MatsimTestCase {


	static class TestHandler implements EventHandlerActivityEndI, EventHandlerAgentDepartureI, EventHandlerAgentWait2LinkI,
			EventHandlerLinkLeaveI, EventHandlerLinkEnterI, EventHandlerAgentArrivalI, EventHandlerActivityStartI,
			EventHandlerAgentStuckI {

		public int eventCounter = 0;

		public void reset(final int iteration) {
			this.eventCounter = 0;
		}

		public void handleEvent(final EventActivityEnd event) {
			this.eventCounter++;
			assertEquals("expected activity-End-Event to be event #1", 1, this.eventCounter);
			assertEquals(21610.0, event.time, 0.0);
			assertEquals("1", event.agentId);
			assertEquals("2", event.linkId);
		}

		public void handleEvent(final EventAgentDeparture event) {
			this.eventCounter++;
			assertEquals("expected agentDeparture-Event to be event #2", 2, this.eventCounter);
			assertEquals(21620.0, event.time, 0.0);
			assertEquals("2", event.agentId);
			assertEquals("3", event.linkId);
		}

		public void handleEvent(final EventAgentWait2Link event) {
			this.eventCounter++;
			assertEquals("expected wait2link-Event to be event #3", 3, this.eventCounter);
			assertEquals(21630.0, event.time, 0.0);
			assertEquals("3", event.agentId);
			assertEquals("4", event.linkId);
		}

		public void handleEvent(final EventLinkLeave event) {
			this.eventCounter++;
			assertEquals("expected linkleave-Event to be event #4", 4, this.eventCounter);
			assertEquals(21640.0, event.time, 0.0);
			assertEquals("4", event.agentId);
			assertEquals("5", event.linkId);
		}

		public void handleEvent(final EventLinkEnter event) {
			this.eventCounter++;
			assertEquals("expected linkleave-Event to be event #5", 5, this.eventCounter);
			assertEquals(21650.0, event.time, 0.0);
			assertEquals("5", event.agentId);
			assertEquals("6", event.linkId);
		}

		public void handleEvent(final EventAgentArrival event) {
			this.eventCounter++;
			assertEquals("expected agentArrival-Event to be event #6", 6, this.eventCounter);
			assertEquals(21660.0, event.time, 0.0);
			assertEquals("6", event.agentId);
			assertEquals("7", event.linkId);
		}

		public void handleEvent(final EventActivityStart event) {
			this.eventCounter++;
			assertEquals("expected activityStart-Event to be event #7", 7, this.eventCounter);
			assertEquals(21670.0, event.time, 0.0);
			assertEquals("7", event.agentId);
			assertEquals("8", event.linkId);
		}

		public void handleEvent(final EventAgentStuck event) {
			this.eventCounter++;
			assertEquals("expected agentStuck-Event to be event #8", 8, this.eventCounter);
			assertEquals(21680.0, event.time, 0.0);
			assertEquals("8", event.agentId);
			assertEquals("9", event.linkId);
		}

	};

	public final void testTxtReader() {
		Events events = new Events();
		TestHandler handler = new TestHandler();
		events.addHandler(handler);
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/events.txt");
		assertEquals("number of read events", 8, handler.eventCounter);
	}

	public final void testXmlReader() {
		Events events = new Events();
		TestHandler handler = new TestHandler();
		events.addHandler(handler);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.readFile("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/events.xml");
		assertEquals("number of read events", 8, handler.eventCounter);
	}

	public final void testAutoFormatReaderTxt() {
		Events events = new Events();
		TestHandler handler = new TestHandler();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/events.txt");
		assertEquals("number of read events", 8, handler.eventCounter);
	}

	public final void testAutoFormatReaderXml() {
		Events events = new Events();
		TestHandler handler = new TestHandler();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile("test/input/" + this.getClass().getCanonicalName().replace('.', '/') + "/events.xml");
		assertEquals("number of read events", 8, handler.eventCounter);
	}
}