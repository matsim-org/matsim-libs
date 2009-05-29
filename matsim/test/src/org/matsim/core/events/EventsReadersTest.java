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

import org.matsim.api.basic.v01.events.BasicActivityEndEvent;
import org.matsim.api.basic.v01.events.BasicActivityStartEvent;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.BasicAgentStuckEvent;
import org.matsim.api.basic.v01.events.BasicAgentWait2LinkEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicActivityEndEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicActivityStartEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentDepartureEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentStuckEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentWait2LinkEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;
import org.xml.sax.SAXException;

public class EventsReadersTest extends MatsimTestCase {


	static class TestHandler implements BasicActivityEndEventHandler, BasicAgentDepartureEventHandler, BasicAgentWait2LinkEventHandler,
			BasicLinkLeaveEventHandler, BasicLinkEnterEventHandler, BasicAgentArrivalEventHandler, BasicActivityStartEventHandler,
			BasicAgentStuckEventHandler {

		public int eventCounter = 0;

		public void reset(final int iteration) {
			this.eventCounter = 0;
		}

		public void handleEvent(final BasicActivityEndEvent event) {
			this.eventCounter++;
			assertEquals("expected activity-End-Event to be event #1", 1, this.eventCounter);
			assertEquals(21610.0, event.getTime(), 0.0);
			assertEquals("1", event.getPersonId().toString());
			assertEquals(new IdImpl("2"), event.getLinkId());
		}

		public void handleEvent(final BasicAgentDepartureEvent event) {
			this.eventCounter++;
			assertEquals("expected agentDeparture-Event to be event #2", 2, this.eventCounter);
			assertEquals(21620.0, event.getTime(), 0.0);
			assertEquals("2", event.getPersonId().toString());
			assertEquals("3", event.getLinkId().toString());
		}

		public void handleEvent(final BasicAgentWait2LinkEvent event) {
			this.eventCounter++;
			assertEquals("expected wait2link-Event to be event #3", 3, this.eventCounter);
			assertEquals(21630.0, event.getTime(), 0.0);
			assertEquals("3", event.getPersonId().toString());
			assertEquals("4", event.getLinkId().toString());
		}

		public void handleEvent(final BasicLinkLeaveEvent event) {
			this.eventCounter++;
			assertEquals("expected linkleave-Event to be event #4", 4, this.eventCounter);
			assertEquals(21640.0, event.getTime(), 0.0);
			assertEquals("4", event.getPersonId().toString());
			assertEquals("5", event.getLinkId().toString());
		}

		public void handleEvent(final BasicLinkEnterEvent event) {
			this.eventCounter++;
			assertEquals("expected linkleave-Event to be event #5", 5, this.eventCounter);
			assertEquals(21650.0, event.getTime(), 0.0);
			assertEquals("5", event.getPersonId().toString());
			assertEquals("6", event.getLinkId().toString());
		}

		public void handleEvent(final BasicAgentArrivalEvent event) {
			this.eventCounter++;
			assertEquals("expected agentArrival-Event to be event #6", 6, this.eventCounter);
			assertEquals(21660.0, event.getTime(), 0.0);
			assertEquals("6", event.getPersonId().toString());
			assertEquals("7", event.getLinkId().toString());
		}

		public void handleEvent(final BasicActivityStartEvent event) {
			this.eventCounter++;
			assertEquals("expected activityStart-Event to be event #7", 7, this.eventCounter);
			assertEquals(21670.0, event.getTime(), 0.0);
			assertEquals("7", event.getPersonId().toString());
			assertEquals(new IdImpl("8"), event.getLinkId());
		}

		public void handleEvent(final BasicAgentStuckEvent event) {
			this.eventCounter++;
			assertEquals("expected agentStuck-Event to be event #8", 8, this.eventCounter);
			assertEquals(21680.0, event.getTime(), 0.0);
			assertEquals("8", event.getPersonId().toString());
			assertEquals("9", event.getLinkId().toString());
		}

	}

	public final void testTxtReader() {
		Events events = new Events();
		TestHandler handler = new TestHandler();
		events.addHandler(handler);
		EventsReaderTXTv1 reader = new EventsReaderTXTv1(events);
		reader.readFile(getClassInputDirectory() + "events.txt");
		assertEquals("number of read events", 8, handler.eventCounter);
	}

	public final void testXmlReader() throws SAXException, ParserConfigurationException, IOException {
		Events events = new Events();
		TestHandler handler = new TestHandler();
		events.addHandler(handler);
		EventsReaderXMLv1 reader = new EventsReaderXMLv1(events);
		reader.parse(getClassInputDirectory() + "events.xml");
		assertEquals("number of read events", 8, handler.eventCounter);
	}

	public final void testAutoFormatReaderTxt() {
		Events events = new Events();
		TestHandler handler = new TestHandler();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(getClassInputDirectory() + "events.txt");
		assertEquals("number of read events", 8, handler.eventCounter);
	}

	public final void testAutoFormatReaderXml() {
		Events events = new Events();
		TestHandler handler = new TestHandler();
		events.addHandler(handler);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(getClassInputDirectory() + "events.xml");
		assertEquals("number of read events", 8, handler.eventCounter);
	}
}