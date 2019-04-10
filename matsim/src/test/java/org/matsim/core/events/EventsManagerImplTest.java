/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

/**
 * @author mrieser
 */
public class EventsManagerImplTest {

	private final static Logger log = Logger.getLogger(EventsManagerImplTest.class);

	/**
	 * @author mrieser
	 */
	@Test
	public void testProcessEvent_CustomEventHandler() {
		EventsManager manager = EventsUtils.createEventsManager();
		CountingMyEventHandler handler = new CountingMyEventHandler();
		manager.addHandler(handler);
		manager.processEvent(new MyEvent(123.45));
		Assert.assertEquals("EventHandler was not called.", 1, handler.counter);
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testProcessEvent_ExceptionInEventHandler() {
		EventsManager manager = EventsUtils.createEventsManager();
		CrashingMyEventHandler handler = new CrashingMyEventHandler();
		manager.addHandler(handler);
		try {
			manager.processEvent(new MyEvent(123.45));
			// Even this even handling will fail, we won't see any log, because we log only first exception from the handler
			manager.processEvent(new MyEvent(123.45));
		} catch (final RuntimeException e) {
			Assert.fail("No exception must be thrown from processEvent.");
		}
	}

	/*package*/ static class MyEvent extends Event {
		public MyEvent(final double time) {
			super(time);
		}
		@Override
		public String getEventType() {
			return "myEvent";
		}
	}

	/*package*/ static interface MyEventHandler extends EventHandler {
		public void handleEvent(final MyEvent e);
	}

	/*package*/ static class CountingMyEventHandler implements MyEventHandler {
		/*package*/ int counter = 0;
		@Override
		public void reset(final int iteration) {
			this.counter = 0;
		}
		@Override
		public void handleEvent(final MyEvent e) {
			this.counter++;
		}
	}

	/*package*/ static class CrashingMyEventHandler implements MyEventHandler {
		/*package*/ int counter = 0;
		@Override
		public void reset(final int iteration) {
			this.counter = 0;
		}
		@Override
		public void handleEvent(final MyEvent e) {
			this.counter++;
			int i = 1 / 0; // produce ArithmeticException
			System.out.println(i);
		}
	}
}
