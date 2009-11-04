/* *********************************************************************** *
 * project: org.matsim.*
 * EventsHierarchyTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.Map;

import org.matsim.api.basic.v01.events.BasicEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests that all implemented Event-Interfaces are recognized, however
 * the class/interface-hierarchy of events look like.
 *
 * @author mrieser
 */
public class EventsHierarchyTest extends MatsimTestCase {
	
	public void testEventsHierarchy() {
		EventsManagerImpl events = new EventsManagerImpl();
		TestEventHandler handler = new TestEventHandler();
		events.addHandler(handler);
		assertEquals("wrong number of handled events.", 0, handler.counter);
		events.processEvent(new MockEvent1());
		assertEquals("wrong number of handled events.", 1, handler.counter);
		events.processEvent(new MockEvent2());
		assertEquals("wrong number of handled events.", 2, handler.counter);
	}
	
	/* Build a hierarchy of interfaces and classes */
	
	private interface A extends BasicEvent { }

	private interface B extends BasicEvent { }
	
	private interface C extends A { }
	
	/** this interface now extends from BasicEvent on two hierarchy-paths, to test
	 * that it is still recognized as only one event-type.
	 */
	private interface D extends B, C { }
	
	private static class MockEvent1 implements D {
		public double getTime() {
			return 0;
		}
		public Map<String, String> getAttributes() {
			return null;
		}
	}
	
	/** this class only extends another event class, without specifying any more interfaces. */
	private static class MockEvent2 extends MockEvent1 { }

	/** A simple event-handler */
	private static class TestEventHandler implements BasicEventHandler {

		/*package*/ int counter = 0;
		
		public void handleEvent(BasicEvent event) {
			this.counter++;
		}

		public void reset(int iteration) {
			this.counter = 0;
		}
		
	}
	
}
