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

package org.matsim.contrib.drt.util;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtEventsReadersTest {
	private final String mode = "mode_1";
	private final Id<Person> person = Id.createPersonId("person_1");
	private final Id<Request> request = Id.create("request_1", Request.class);
	private final Id<Link> link1 = Id.createLinkId("link_1");
	private final Id<Link> link2 = Id.createLinkId("link_2");
	private final Id<DvrpVehicle> vehicle = Id.create("vehicle_1", DvrpVehicle.class);
	private final Id<Person> driver = Id.create("driver_1", Person.class);

	//standard dvrp events are tested in DvrpEventsReadersTest
	private final List<Event> drtEvents = List.of(
			new DrtRequestSubmittedEvent(0, mode, request, List.of(person), link1, link2, 111, 222, 0.0, 412.0, 512.0),//
			taskStarted(10, DrtDriveTask.TYPE, 0, link1),//
			taskEnded(30, DefaultDrtStopTask.TYPE, 1, link2), //
			taskStarted(50, DrtStayTask.TYPE, 2, link1),//
			taskEnded(70, EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE, 3, link2));

	@Test
	public void testReader() {
		var outputStream = new ByteArrayOutputStream();
		EventWriterXML writer = new EventWriterXML(outputStream);
		drtEvents.forEach(writer::handleEvent);
		writer.closeFile();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TestEventHandler handler = new TestEventHandler();
		eventsManager.addHandler(handler);
		eventsManager.initProcessing();
		DrtEventsReaders.createEventsReader(eventsManager)
				.readStream(new ByteArrayInputStream(outputStream.toByteArray()),
						ControllerConfigGroup.EventsFileFormat.xml);
		eventsManager.finishProcessing();

		assertThat(handler.handledEvents).usingRecursiveFieldByFieldElementComparator()
				.containsExactlyElementsOf(drtEvents);
	}

	private TaskStartedEvent taskStarted(double time, DrtTaskType taskType, int taskIndex, Id<Link> linkId) {
		return new TaskStartedEvent(time, mode, vehicle, driver, taskType, taskIndex, linkId);
	}

	private TaskEndedEvent taskEnded(double time, DrtTaskType taskType, int taskIndex, Id<Link> linkId) {
		return new TaskEndedEvent(time, mode, vehicle, driver, taskType, taskIndex, linkId);
	}

	private static class TestEventHandler
			implements DrtRequestSubmittedEventHandler, TaskStartedEventHandler, TaskEndedEventHandler {
		private final List<Event> handledEvents = new ArrayList<>();

		@Override
		public void handleEvent(DrtRequestSubmittedEvent event) {
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
