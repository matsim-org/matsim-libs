
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
import org.matsim.core.events.handler.EventHandler;
import org.matsim.testcases.utils.EventsCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

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
		manager.processEvent(new GenericEvent(2, "some"));
		manager.processEvent(new GenericEvent(1, "some"));
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
		manager.processEvent(new GenericEvent(2,  "some"));
		manager.afterSimStep(2);
		manager.finishProcessing();
	}

	@Test
	public void testWaitingWhenTimestepIsIncreased() {

		var manager = new SimStepParallelEventsManagerImpl(1);
		manager.addHandler(new SlowHandler(manager, "afterSimStep"));
		var assertionHandler = new CounterHandler(SlowHandler.EVENT_TYPE_THROWN);
		manager.addHandler(assertionHandler);

		manager.initProcessing();
		manager.processEvent(new GenericEvent(2, "afterSimStep"));
		manager.processEvent(new GenericEvent(3, "some"));
		assertEquals(1, assertionHandler.counter);
		manager.finishProcessing();
	}

	@Test
	public void testWaitingWhenTimestepIsIncreasedAsync() {

		var manager = new SimStepParallelEventsManagerImpl(1);
		manager.addHandler(new SlowHandler(manager, "afterSimStep"));
		EventHandler handler = (BasicEventHandler) event -> {
			if (event.getEventType().equals(SlowHandler.EVENT_TYPE_THROWN)) {
				// trigger update of manager's timestep while main thread is awaiting this thread to finish
				manager.processEvent(new GenericEvent(event.getTime() + 1, "some"));
			}
		};
		manager.addHandler(handler);

		manager.initProcessing();
		manager.processEvent(new GenericEvent(2, "afterSimStep"));

		// increase timestep from main thread
		manager.processEvent(new GenericEvent(3, "some"));
		manager.finishProcessing();
	}

	@Test
	public void testWaitingWhenFinishProcessing() {

		var manager = new SimStepParallelEventsManagerImpl(1);
		manager.addHandler(new SlowHandler(manager, "some-event"));
		var handlerOfSlowHandlersEvents = new CounterHandler(SlowHandler.EVENT_TYPE_THROWN);
		var handlerOfSyncEvents = new CounterHandler("synchronous-event");
		manager.addHandler(handlerOfSlowHandlersEvents);
		manager.addHandler(handlerOfSyncEvents);

		manager.initProcessing();
		manager.processEvent(new GenericEvent(1, "some-event"));

		// the slow handler will issue an event to 'processEvent' while finishing
		// is awaited. The finish method should also wait for this event
		manager.finishProcessing();

		// after finish processing events are directed to a synchronous manager therefore
		// the following event should be caught without awaiting
		manager.processEvent(new GenericEvent(1,"synchronous-event"));

		assertEquals(1, handlerOfSlowHandlersEvents.counter);
		assertEquals(1, handlerOfSyncEvents.counter);
	}

	@Test
	public void testWaitingWhenTimeIsIncreased() {

		var manager = new SimStepParallelEventsManagerImpl(1);
		manager.addHandler(new SlowHandler(manager, "some-event"));
		var counterHandler = new CounterHandler(SlowHandler.EVENT_TYPE_THROWN);
		manager.addHandler(counterHandler);

		manager.initProcessing();
		manager.processEvent(new GenericEvent(1, "some-event"));
		manager.processEvent(new GenericEvent(2, "event-from-next-timestep"));

		assertEquals(1, counterHandler.counter);
	}

	@Test
	public void testWaitingWhenAfterSimStep() {

		var manager = new SimStepParallelEventsManagerImpl(1);
		manager.addHandler(new SlowHandler(manager, "some-event"));
		var counterHandler = new CounterHandler(SlowHandler.EVENT_TYPE_THROWN);
		manager.addHandler(counterHandler);

		manager.initProcessing();
		manager.processEvent(new GenericEvent(1, "some-event"));
		manager.afterSimStep(1);

		assertEquals(1, counterHandler.counter);
	}

	@Test
	public void testOrderOfEventsIsKept() {

		var manager = new SimStepParallelEventsManagerImpl(3);
		var events = List.of(
				new GenericEvent(1,"first"),
				new GenericEvent(1, "second"),
				new GenericEvent(1, "third"));

		var handler = new BasicEventHandler() {

			private final List<GenericEvent> caughtEvents = new ArrayList<>();
			@Override
			public void handleEvent(Event event) {
				if (event instanceof GenericEvent) {
					caughtEvents.add((GenericEvent) event);
				}
			}
		};
		manager.addHandler(handler);

		manager.initProcessing();
		for (var event : events) {
			manager.processEvent(event);
		}
		manager.finishProcessing();

		assertEquals(events.size(), handler.caughtEvents.size());
		for (var i = 0; i < events.size(); i++) {
			var expected = events.get(i);
			var actual = handler.caughtEvents.get(i);
			assertEquals(expected.type, actual.type);
		}
	}

	private static class SlowHandler implements BasicEventHandler {

		static final String EVENT_TYPE_THROWN = "fromSlowHandler";

		final EventsManager manager;
		final String eventType;

		private SlowHandler(EventsManager manager, String eventType) {
			this.manager = manager;
			this.eventType = eventType;
		}

		@Override
		public void handleEvent(Event event) {

			if (event.getEventType().equals(eventType)) {
				try {
					Thread.sleep(1000); // long computation
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				manager.processEvent(new GenericEvent(event.getTime(), EVENT_TYPE_THROWN));
			}
		}
	}

	private static class CounterHandler implements BasicEventHandler {

		private final String eventType;

		private int counter;

		private CounterHandler(String eventType) {
			this.eventType = eventType;
		}

		@Override
		public void handleEvent(Event event) {
			if (event.getEventType().equals(eventType))
				counter++;
		}
	}

	private static class GenericEvent extends Event {

		private final String type;

		private GenericEvent(double time, String type) {
			super(time);
			this.type = type;
		}

		@Override
		public String getEventType() {
			return type;
		}
	}
}
