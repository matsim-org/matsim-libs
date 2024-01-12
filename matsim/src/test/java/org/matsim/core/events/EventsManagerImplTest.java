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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;

/**
 * @author mrieser
 */
public class EventsManagerImplTest {

	private final static Logger log = LogManager.getLogger(EventsManagerImplTest.class);

	/**
	 * @author mrieser
	 */
	@Test
	void testProcessEvent_CustomEventHandler() {
		EventsManager manager = EventsUtils.createEventsManager();
		CountingMyEventHandler handler = new CountingMyEventHandler();
		manager.addHandler(handler);
		manager.initProcessing();
		manager.processEvent(new MyEvent(123.45));
		manager.finishProcessing();
		Assertions.assertEquals(1, handler.counter, "EventHandler was not called.");
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testProcessEvent_ExceptionInEventHandler() {
		EventsManager manager = EventsUtils.createEventsManager();
		CrashingMyEventHandler handler = new CrashingMyEventHandler();
		manager.addHandler(handler);
		manager.initProcessing();
		try {
			manager.processEvent(new MyEvent(123.45));
			manager.finishProcessing();
			Assertions.fail("expected exception, but got none.");
		} catch (final RuntimeException e) {
			log.info("Catched expected exception.", e);

			Assertions.assertEquals(1, handler.counter);
			Assertions.assertTrue(e.getCause() instanceof ArithmeticException);
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
			//noinspection divzero
			int i = 1 / 0; // produce ArithmeticException
			System.out.println(i);
		}
	}
}
