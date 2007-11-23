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

package org.matsim.events;

import org.matsim.events.BasicEvent;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.Events;
import org.matsim.events.handler.BasicEventHandlerI;
import org.matsim.events.handler.EventHandlerI;
import org.matsim.testcases.MatsimTestCase;

public class EventsHandlerHierarchy extends MatsimTestCase {

	int eventHandled = 0;

	class A implements BasicEventHandlerI {

		public void handleEvent(final BasicEvent event) {
			System.out.println("Event handled");
			EventsHandlerHierarchy.this.eventHandled++;
		}

		public void reset(final int iteration) {}

	};

	class B extends A {};

	public final void testHandlerHierarchy() {
		Events events = new Events();

		EventHandlerI cc = new B();
		events.computeEvent(new EventLinkLeave(0.,"",""));
		assertEquals(this.eventHandled, 0);
		events.addHandler(cc);
		events.computeEvent(new EventLinkLeave(0.,"",""));
		assertEquals(this.eventHandled, 1);
	}
}
