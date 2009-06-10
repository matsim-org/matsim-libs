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

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.handler.BasicLinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.testcases.MatsimTestCase;

public class EventsHandlerHierarchyTest extends MatsimTestCase {

	int eventHandled = 0;
	int resetCalled = 0;

	class A implements BasicEventHandler, BasicLinkLeaveEventHandler {

		public void handleEvent(final BasicEvent event) {
			System.out.println("Event handled");
			EventsHandlerHierarchyTest.this.eventHandled++;
		}

		public void handleEvent(BasicLinkLeaveEvent event) {
		}

		public void reset(final int iteration) {
			EventsHandlerHierarchyTest.this.resetCalled++;
		}
	}

	class B extends A {}

	@SuppressWarnings("unused")
	class C extends A implements BasicEventHandler, BasicLinkLeaveEventHandler {}

	public final void testHandlerHierarchy() {
		Events events = new Events();
		Id dummy = new IdImpl("");
		EventHandler cc = new B();
		events.processEvent(new LinkLeaveEvent(0., dummy, dummy));
		assertEquals(this.eventHandled, 0);
		events.addHandler(cc);
		events.processEvent(new LinkLeaveEvent(0., dummy, dummy));
		assertEquals(this.eventHandled, 1);
	}

	public final void testHierarchicalReset() {
		Events events = new Events();
		Id dummy = new IdImpl("");
		//first test if handleEvent is not called twice for A and for C
		C cc = new C();
		events.processEvent(new LinkLeaveEvent(0., dummy, dummy));
		assertEquals(this.eventHandled, 0);
		events.addHandler(cc);
		events.processEvent(new LinkLeaveEvent(0., dummy, dummy));
		assertEquals(this.eventHandled, 1);
		//then test the reset
		events.resetHandlers(0);
		assertEquals(1, this.resetCalled);
	}

}
