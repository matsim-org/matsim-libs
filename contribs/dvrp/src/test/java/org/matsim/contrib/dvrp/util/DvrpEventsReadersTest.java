/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.core.config.groups.ControllerConfigGroup.EventsFileFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpEventsReadersTest {
	private final String mode = "mode_1";
	private final Id<Person> person = Id.createPersonId("person_1");
	private final Id<Request> request = Id.create("request_1", Request.class);
	private final Id<Link> fromLink = Id.createLinkId("link_1");
	private final Id<Link> toLink = Id.createLinkId("link_2");
	private final Id<DvrpVehicle> vehicle = Id.create("vehicle_1", DvrpVehicle.class);
	private final Id<Person> driver = Id.create("driver_1", Person.class);

	private enum TestTaskType implements Task.TaskType {
		DRIVE_TASK
	}

	private final List<Event> dvrpEvents = List.of(
			new PassengerRequestSubmittedEvent(0, mode, request, List.of(person), fromLink, toLink),
			new PassengerRequestScheduledEvent(1, mode, request, List.of(person), vehicle, 100, 200),
			new PassengerRequestRejectedEvent(2, mode, request, List.of(person), "cause_1"),
			new PassengerPickedUpEvent(111, mode, request, person, vehicle),
			new PassengerDroppedOffEvent(222, mode, request, person, vehicle),
			new TaskStartedEvent(300, mode, vehicle, driver, TestTaskType.DRIVE_TASK, 0, fromLink),
			new TaskEndedEvent(333, mode, vehicle, driver, TestTaskType.DRIVE_TASK, 0, toLink));

	@Test
	public void testReader() {
		var outputStream = new ByteArrayOutputStream();
		EventWriterXML writer = new EventWriterXML(outputStream);
		dvrpEvents.forEach(writer::handleEvent);
		writer.closeFile();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TestEventHandler handler = new TestEventHandler();
		eventsManager.addHandler(handler);
		eventsManager.initProcessing();
		DvrpEventsReaders.createEventsReader(eventsManager, TestTaskType::valueOf)
				.readStream(new ByteArrayInputStream(outputStream.toByteArray()), EventsFileFormat.xml);
		eventsManager.finishProcessing();

		assertThat(handler.handledEvents).usingRecursiveFieldByFieldElementComparator()
				.containsExactlyElementsOf(dvrpEvents);
	}

	private static class TestEventHandler
			implements PassengerRequestSubmittedEventHandler, PassengerRequestScheduledEventHandler,
			PassengerRequestRejectedEventHandler, PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler,
			TaskStartedEventHandler, TaskEndedEventHandler {
		private final List<Event> handledEvents = new ArrayList<>();

		@Override
		public void handleEvent(PassengerRequestSubmittedEvent event) {
			handledEvents.add(event);
		}

		@Override
		public void handleEvent(PassengerRequestScheduledEvent event) {
			handledEvents.add(event);
		}

		@Override
		public void handleEvent(PassengerRequestRejectedEvent event) {
			handledEvents.add(event);
		}

		@Override
		public void handleEvent(PassengerPickedUpEvent event) {
			handledEvents.add(event);
		}

		@Override
		public void handleEvent(PassengerDroppedOffEvent event) {
			handledEvents.add(event);
		}

		@Override
		public void handleEvent(TaskStartedEvent event) {
			handledEvents.add(event);
		}

		@Override
		public void handleEvent(TaskEndedEvent event) {
			handledEvents.add(event);
		}
	}
}
