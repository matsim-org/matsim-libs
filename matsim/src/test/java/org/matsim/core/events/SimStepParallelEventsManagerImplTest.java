
/* *********************************************************************** *
 * project: org.matsim.*
 * SimStepParallelEventsManagerImplTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.testcases.utils.EventsCollector;

import java.util.Set;

import static org.junit.Assert.assertTrue;

public class SimStepParallelEventsManagerImplTest {

	@Test
	public void testEventHandlerCanProduceAdditionalEventLateInSimStep() {
		final SimStepParallelEventsManagerImpl events = new SimStepParallelEventsManagerImpl(8);
		events.addHandler(new LinkEnterEventHandler() {
			@Override
			public void handleEvent(LinkEnterEvent event) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				events.processEvent(new PersonStuckEvent(event.getTime(), Id.createPersonId(0), Id.createLinkId(0), "car"));
			}

			@Override
			public void reset(int iteration) {
			}
		});
		EventsCollector collector = new EventsCollector();
		events.addHandler(collector);
		events.initProcessing();
		events.processEvent(new LinkEnterEvent(0.0, Id.createVehicleId(0), Id.createLinkId(0)));
		events.processEvent(new LinkLeaveEvent(0.0, Id.createVehicleId(0), Id.createLinkId(0)));
		events.afterSimStep(0.0);
		events.processEvent(new LinkEnterEvent(1.0, Id.createVehicleId(0), Id.createLinkId(0)));
		events.processEvent(new LinkLeaveEvent(1.0, Id.createVehicleId(0), Id.createLinkId(0)));
		events.afterSimStep(1.0);
		events.finishProcessing();

		collector.getEvents().containsAll(Set.of(new LinkEnterEvent(0.0, Id.createVehicleId(0), Id.createLinkId(0)),
				new LinkLeaveEvent(0.0, Id.createVehicleId(0), Id.createLinkId(0)),
				new PersonStuckEvent(0.0, Id.createPersonId(0), Id.createLinkId(0), "car"),
				new LinkEnterEvent(1.0, Id.createVehicleId(0), Id.createLinkId(0)),
				new LinkLeaveEvent(1.0, Id.createVehicleId(0), Id.createLinkId(0)),
				new PersonStuckEvent(1.0, Id.createPersonId(0), Id.createLinkId(0), "car")));
	}

	@Test(expected = RuntimeException.class)
	public void ensureOrder() {

		var manager = new SimStepParallelEventsManagerImpl(1);
		manager.addHandler((BasicEventHandler) event -> { // nothing
		});

		manager.initProcessing();
		manager.processEvent(new SomeEvent(2));
		manager.processEvent(new SomeEvent(1));
		manager.afterSimStep(2);
		manager.finishProcessing();
	}

	@Test(expected = RuntimeException.class)
	public void testExceptionsInHandler() {

		var manager = new SimStepParallelEventsManagerImpl(1);
		manager.addHandler((BasicEventHandler) event -> {
			throw new RuntimeException("Test");
		});

		manager.initProcessing();
		manager.processEvent(new SomeEvent(2));
		manager.afterSimStep(2);
		manager.finishProcessing();
	}

	@Test
	public void simpleTest() {

		var manager = new SimStepParallelEventsManagerImpl(1);
		manager.addHandler(new HandlerListeningForAfterSimStepEvents(manager));
		var assertionHandler = new HandlerForAsyncEvents();
		manager.addHandler(assertionHandler);

		manager.initProcessing();
		manager.processEvent(new SomeEvent(1));
		manager.afterSimStep(1);
		manager.processEvent(new AfterSimStepEvent(1));

		// move on to the next timestep. The main thread should now wait for the
		manager.processEvent(new SomeEvent(2));
		manager.afterSimStep(2);
		manager.processEvent(new AfterSimStepEvent(2));
		manager.finishProcessing();

		assertTrue(assertionHandler.isCaughtEvent());
	}

	private static class SomeEvent extends Event {

		public static final String EVENT_TYPE = "someEvent";
		public SomeEvent(double time) {
			super(time);
		}

		@Override
		public String getEventType() {
			return EVENT_TYPE;
		}
	}

	private static class AfterSimStepEvent extends Event {

		public static final String EVENT_TYPE = "afterSimStep";

		public AfterSimStepEvent(double time) {
			super(time);
		}

		@Override
		public String getEventType() {
			return EVENT_TYPE;
		}
	}

	private static class HandlerListeningForAfterSimStepEvents implements BasicEventHandler {

		private final EventsManager manager;

		private HandlerListeningForAfterSimStepEvents(EventsManager manager) {
			this.manager = manager;
		}

		@Override
		public void handleEvent(Event event) {

			if (event.getEventType().equals(AfterSimStepEvent.EVENT_TYPE)) {
				try {
					Thread.sleep(1000); // long computation
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				manager.processEvent(new HandlerListeningForAfterSimStepEvents.EventThrownAsync(event.getTime()));
			}
		}

		private static class EventThrownAsync extends Event {

			public static final String EVENT_TYPE = "afterSimStepAsync";

			public EventThrownAsync(double time) {
				super(time);
			}

			@Override
			public String getEventType() {
				return EVENT_TYPE;
			}
		}
	}

	private static class HandlerForAsyncEvents implements BasicEventHandler {

		private boolean caughtEvent = false;

		@Override
		public void handleEvent(Event event) {
			if (event.getEventType().equals(HandlerListeningForAfterSimStepEvents.EventThrownAsync.EVENT_TYPE)) {

				this.caughtEvent = true;
			}
		}

		public boolean isCaughtEvent() {
			return caughtEvent;
		}
	}
}
