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

package org.matsim.core.events.parallelEventsHandler;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.LinkLeaveEventImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author rashid_waraich
 */
public class ParallelEventsTest extends MatsimTestCase {

	private final static Logger log = Logger.getLogger(ParallelEventsTest.class);

	/** Tests if the right number of events were processed by the handler(s)
	 * for different number of threads, events, handlers and different
	 * constructors */
	public void testEventCount() {
		processEvents(new ParallelEventsManagerImpl(1), 100, 1, 1);
		processEvents(new ParallelEventsManagerImpl(2), 100, 10, 2);
		processEvents(new ParallelEventsManagerImpl(4), 100, 1, 10);
		processEvents(new ParallelEventsManagerImpl(2), 1500, 2, 1);
		processEvents(new ParallelEventsManagerImpl(2), 3000, 3, 1);
		processEvents(new ParallelEventsManagerImpl(1, 100), 100, 1, 1);
		processEvents(new ParallelEventsManagerImpl(1, 100), 1000, 1, 1);
		processEvents(new ParallelEventsManagerImpl(1, 1000), 100, 1, 1);
		processEvents(new ParallelEventsManagerImpl(2, 100), 100, 1, 1);
		processEvents(new ParallelEventsManagerImpl(2, 100), 1000, 2, 1);
		processEvents(new ParallelEventsManagerImpl(2, 1000), 1000, 2, 1);
		processEvents(new ParallelEventsManagerImpl(2, 5000), 100, 3, 1);
	}

	/** test, if adding and removing a handler works */
	public void testAddAndRemoveHandler() {
		EventsManagerImpl events = new ParallelEventsManagerImpl(2);

		// start iteration
		events.initProcessing();

		Handler1 handler = new Handler1();
		events.addHandler(handler);
		events.removeHandler(handler);

		LinkLeaveEventImpl linkLeaveEvent = new LinkLeaveEventImpl(0, new IdImpl(""), new IdImpl(""));

		for (int i = 0; i < 100; i++) {
			events.processEvent(linkLeaveEvent);
		}

		events.finishProcessing();

		assertEquals(0, handler.getNumberOfProcessedMessages());
	}

	private void processEvents(final EventsManagerImpl events, final int eventCount,
			final int numberOfHandlers, final int numberOfIterations) {

		Handler1[] handlers = new Handler1[numberOfHandlers];

		for (int i = 0; i < numberOfHandlers; i++) {
			handlers[i] = new Handler1();
			events.addHandler(handlers[i]);
		}

		LinkLeaveEventImpl linkLeaveEvent = new LinkLeaveEventImpl(0, new IdImpl(""), new IdImpl(""));

		for (int j = 0; j < numberOfIterations; j++) {

			// initialize events handling for new iteration
			events.initProcessing();

			for (int i = 0; i < eventCount; i++) {
				events.processEvent(linkLeaveEvent);
			}

			// wait on all event handler threads
			// very important for the functionality of parallelEvents class
			events.finishProcessing();

			for (int i = 0; i < numberOfHandlers; i++) {
				assertEquals(eventCount, handlers[i]
						.getNumberOfProcessedMessages());
				handlers[i].resetNumberOfProcessedMessages();
			}
		}
	}

	/**
	 * @author mrieser
	 */
	public void testCrashingHandler() {
		EventsManagerImpl events = new ParallelEventsManagerImpl(2);

		// start iteration
		events.initProcessing();

		CrashingHandler handler1 = new CrashingHandler();
		CrashingHandler handler2 = new CrashingHandler();
		events.addHandler(handler1);
		events.addHandler(handler2);

		LinkLeaveEventImpl linkLeaveEvent = new LinkLeaveEventImpl(0, new IdImpl(""), new IdImpl(""));
		try {
			for (int i = 0; i < 10; i++) {
				events.processEvent(linkLeaveEvent);
			}
			events.finishProcessing();
			fail("Expected Exception, but got none!");
		} catch (RuntimeException e) {
			log.info("Catched expected exception.", e);
		}

	}

	private static class Handler1 implements LinkLeaveEventHandler {

		private int numberOfProcessedMessages = 0;

		public int getNumberOfProcessedMessages() {
			return this.numberOfProcessedMessages;
		}

		public void resetNumberOfProcessedMessages() {
			this.numberOfProcessedMessages = 0;
		}

		@Override
		public void handleEvent(final LinkLeaveEvent event) {
			this.numberOfProcessedMessages++;
		}

		@Override
		public void reset(final int iteration) {
		}

		public Handler1() {
		}

	}

	/**
	 * @author mrieser
	 */
	private static class CrashingHandler implements LinkLeaveEventHandler {
		@Override
		public void reset(int iteration) {
		}
		@Override
		public void handleEvent(LinkLeaveEvent event) {
			// just some random exception to crash this thread
			throw new IllegalArgumentException("just some exception to crash this thread.");
		}
	}

}
