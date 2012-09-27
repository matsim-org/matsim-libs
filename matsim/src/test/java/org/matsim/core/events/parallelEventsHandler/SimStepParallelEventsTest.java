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
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.SimStepParallelEventsManagerImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author rashid_waraich
 */
public class SimStepParallelEventsTest extends MatsimTestCase {

	private final static Logger log = Logger.getLogger(SimStepParallelEventsTest.class);

	/**
	 * Tests if the right number of events were processed by the handler(s)
	 * for different number of threads, events, handlers and different
	 * constructors.
	 */
	public void testEventCount() {
		processEvents(new SimStepParallelEventsManagerImpl(1), 100, 1, 1);
		processEvents(new SimStepParallelEventsManagerImpl(2), 100, 10, 2);
		processEvents(new SimStepParallelEventsManagerImpl(4), 100, 1, 10);
		processEvents(new SimStepParallelEventsManagerImpl(2), 1500, 2, 1);
		processEvents(new SimStepParallelEventsManagerImpl(2), 3000, 3, 1);
	}

	/** 
	 * Test, if adding and removing a handler works.
	 */
	public void testAddAndRemoveHandler() {
		EventsManager events = new SimStepParallelEventsManagerImpl(2);

		// start iteration
		events.initProcessing();

		Handler1 handler = new Handler1();
		events.addHandler(handler);
		events.removeHandler(handler);

		LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(0, new IdImpl(""), new IdImpl(""), null);

		for (int i = 0; i < 100; i++) {
			events.processEvent(linkLeaveEvent);
		}

		events.finishProcessing();

		assertEquals(0, handler.getNumberOfProcessedMessages());
	}

	private void processEvents(final EventsManager events, final int eventCount,
			final int numberOfHandlers, final int numberOfIterations) {

		Handler1[] handlers = new Handler1[numberOfHandlers];

		for (int i = 0; i < numberOfHandlers; i++) {
			handlers[i] = new Handler1();
			events.addHandler(handlers[i]);
		}

		LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(0, new IdImpl(""), new IdImpl(""), null);

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

	public void testCheckChronologicalEventsOrder() {
		SimStepParallelEventsManagerImpl events = new SimStepParallelEventsManagerImpl(2);
		LinkLeaveEvent linkLeaveEvent;

		/*
		 * Use valid order
		 */
		events.initProcessing();
		
		linkLeaveEvent = new LinkLeaveEvent(1.0, new IdImpl(""), new IdImpl(""), null);
		events.processEvent(linkLeaveEvent);
		linkLeaveEvent = new LinkLeaveEvent(1.0, new IdImpl(""), new IdImpl(""), null);
		events.processEvent(linkLeaveEvent);
		events.afterSimStep(1.0);
		
		linkLeaveEvent = new LinkLeaveEvent(2.0, new IdImpl(""), new IdImpl(""), null);
		events.processEvent(linkLeaveEvent);
		events.afterSimStep(2.0);
		
		events.finishProcessing();
		
		/*
		 * Use invalid order
		 */
		try {
			events.initProcessing();
			
			linkLeaveEvent = new LinkLeaveEvent(1.0, new IdImpl(""), new IdImpl(""), null);
			events.processEvent(linkLeaveEvent);
			events.afterSimStep(1.0);
			
			linkLeaveEvent = new LinkLeaveEvent(2.0, new IdImpl(""), new IdImpl(""), null);
			events.processEvent(linkLeaveEvent);
			events.afterSimStep(2.0);
			
			linkLeaveEvent = new LinkLeaveEvent(1.0, new IdImpl(""), new IdImpl(""), null);
			events.processEvent(linkLeaveEvent);
			
			events.finishProcessing();
			
			fail("Expected a RuntimeException to occur.");
		} catch (RuntimeException E) {
			// everything is fine
		}
	}
	
	/**
	 * Test whether all events of a time step are processed before the
	 * next time step is started. This includes events that are created 
	 * by events handlers based on events which they just handled.
	 * 
	 * @author cdobler
	 */
	public void testSyncToTimeSteps() {

		// Initialize events manager and events handler
		SimStepParallelEventsManagerImpl events = new SimStepParallelEventsManagerImpl(2);
		SyncToTimeStepHandler handler = new SyncToTimeStepHandler(events);
		events.addHandler(handler);
		
		/*
		 * First scenario:
		 * Cause an exception in the EventsManager because the events are not
		 * synchronized at the end of each time step and therefore are added
		 * not chronologically to the EventsManager.
		 */
		try {
			// initialize events handling for new iteration
			events.initProcessing();
			
			for (int time = 0; time < 5; time++) {
				// process event
				LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(time, 
						new IdImpl(""), new IdImpl(""), null);
				events.processEvent(linkLeaveEvent);
				
				// step ahead in time
				log.info("switched time step");
			}
			
			/*
			 * Wait until the handler has created the additional events
			 */
			Thread.sleep(2000);
		
			// wait on all event handler threads
			// very important for the functionality of parallelEvents class
			events.finishProcessing();
			
			fail("Expected a RuntimeException to occur.");
		} catch (Exception E) {
			// everything is fine
		}

		
		/*
		 * Second scenario: 
		 * Everything should be fine.
		 */
		// initialize events handling for new iteration
		events.initProcessing();
		
		for (int time = 0; time < 5; time++) {
			// process event
			LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(time, 
					new IdImpl(""), new IdImpl(""), null);
			events.processEvent(linkLeaveEvent);
			
			// step ahead in time
			events.afterSimStep(time);
			log.info("switched time step");
		}
		
		// wait on all event handler threads
		// very important for the functionality of parallelEvents class
		events.finishProcessing();
	}
	
	private static class SyncToTimeStepHandler implements LinkLeaveEventHandler, LinkEnterEventHandler {

		private final EventsManager eventsManager;
		private double timeStep = 0.0;
		
		public SyncToTimeStepHandler(EventsManager eventsManager) {
			this.eventsManager = eventsManager;
		}
		
		@Override
		public void reset(int iteration) {
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			log.info("leave " + event.getTime());
			
			timeStep = event.getTime();
			/*
			 * Wait for 500ms. In the meantime, the main thread goes on and ends the
			 * current time step. Then, this thread goes on and processes additional events
			 * for the time step that has just ended. 
			 */
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				Gbl.errorMsg(e);
			}
			
			for (int i = 0; i < 100; i++) {
				LinkEnterEvent linkEnterEvent = new LinkEnterEvent(event.getTime(), 
						event.getPersonId(), event.getLinkId(), event.getVehicleId());
				eventsManager.processEvent(linkEnterEvent);
			}
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			log.info("enter " + event.getTime());
			
			/*
			 * Check time: 
			 * If the events are not chronologically, the EventsManager should
			 * create an exception, therefore we do not expect the code to
			 * reach this place.
			 */
			if (event.getTime() < timeStep) {
				fail("Events are not chronologically ordered. " +
						"This should have been caught by the EventsManager.");
			}
		}
	}
	
	/**
	 * @author mrieser
	 */
	public void testCrashingHandler() {
		EventsManager events = new SimStepParallelEventsManagerImpl(2);

		// start iteration
		events.initProcessing();

		CrashingHandler handler1 = new CrashingHandler();
		CrashingHandler handler2 = new CrashingHandler();
		events.addHandler(handler1);
		events.addHandler(handler2);

		LinkLeaveEvent linkLeaveEvent = new LinkLeaveEvent(0, new IdImpl(""), new IdImpl(""), null);
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
