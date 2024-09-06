/* *********************************************************************** *
 * project: org.matsim.*
 * EventsHandlerHierarchy.java
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

public class EventsHandlerHierarchyTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	int eventHandled = 0;
	int resetCalled = 0;

	class A implements BasicEventHandler, LinkLeaveEventHandler {

		@Override
		public void handleEvent(final Event event) {
			System.out.println("Event handled");
			EventsHandlerHierarchyTest.this.eventHandled++;
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
		}

		@Override
		public void reset(final int iteration) {
			EventsHandlerHierarchyTest.this.resetCalled++;
		}
	}

	class B extends A {}

	@SuppressWarnings("unused")
	class C extends A implements BasicEventHandler, LinkLeaveEventHandler {}

	@Test
	final void testHandlerHierarchy() {
		EventsManager events = EventsUtils.createEventsManager();
		Id<Link> linkId = Id.create("1", Link.class);
		Id<Vehicle> vehId = Id.create("1", Vehicle.class);
		EventHandler cc = new B();
		events.initProcessing();
		events.processEvent(new LinkLeaveEvent(0., vehId, linkId));
		events.finishProcessing();
		assertEquals(this.eventHandled, 0);
		events.addHandler(cc);
		events.initProcessing();
		events.processEvent(new LinkLeaveEvent(0., vehId, linkId));
		events.finishProcessing();
		assertEquals(this.eventHandled, 1);
	}

	@Test
	final void testHierarchicalReset() {
		EventsManager events = EventsUtils.createEventsManager();
		Id<Link> linkId = Id.create("1", Link.class);
		Id<Vehicle> vehId = Id.create("1", Vehicle.class);
		//first test if handleEvent is not called twice for A and for C
		C cc = new C();
		events.initProcessing();
		events.processEvent(new LinkLeaveEvent(0., vehId, linkId));
		events.finishProcessing();
		assertEquals(this.eventHandled, 0);
		events.addHandler(cc);
		events.initProcessing();
		events.processEvent(new LinkLeaveEvent(0., vehId, linkId));
		events.finishProcessing();
		assertEquals(this.eventHandled, 1);
		//then test the reset
		events.resetHandlers(0);
		assertEquals(1, this.resetCalled);
	}

}
