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

package org.matsim.contrib.taxi.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
import org.matsim.contrib.etaxi.ETaxiChargingTask;
import org.matsim.contrib.etaxi.ETaxiScheduler;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiOccupiedDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTaskType;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TaxiEventsReadersTest {
	private final String mode = "mode_1";
	private final Id<Link> link1 = Id.createLinkId("link_1");
	private final Id<Link> link2 = Id.createLinkId("link_2");
	private final Id<DvrpVehicle> vehicle = Id.create("vehicle_1", DvrpVehicle.class);
	private final Id<Person> driver = Id.create("driver_1", Person.class);

	//standard dvrp events are tested in DvrpEventsReadersTest
	private final List<Event> taxiEvents = List.of(taskStarted(10, TaxiEmptyDriveTask.TYPE, 0, link1),//
			taskEnded(30, TaxiPickupTask.TYPE, 1, link2), //
			taskStarted(50, TaxiOccupiedDriveTask.TYPE, 2, link1),//
			taskEnded(70, TaxiDropoffTask.TYPE, 3, link1),//
			taskStarted(90, TaxiStayTask.TYPE, 4, link1),//
			taskEnded(110, ETaxiScheduler.DRIVE_TO_CHARGER, 5, link2),//
			taskStarted(130, ETaxiChargingTask.TYPE, 6, link2)//
	);

	@Test
	void testReader() {
		var outputStream = new ByteArrayOutputStream();
		EventWriterXML writer = new EventWriterXML(outputStream);
		taxiEvents.forEach(writer::handleEvent);
		writer.closeFile();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		TestEventHandler handler = new TestEventHandler();
		eventsManager.addHandler(handler);
		eventsManager.initProcessing();
		TaxiEventsReaders.createEventsReader(eventsManager)
				.readStream(new ByteArrayInputStream(outputStream.toByteArray()),
						ControllerConfigGroup.EventsFileFormat.xml);
		eventsManager.finishProcessing();

		assertThat(handler.handledEvents).usingRecursiveFieldByFieldElementComparator()
				.containsExactlyElementsOf(taxiEvents);
	}

	private TaskStartedEvent taskStarted(double time, TaxiTaskType taskType, int taskIndex, Id<Link> linkId) {
		return new TaskStartedEvent(time, mode, vehicle, driver, taskType, taskIndex, linkId);
	}

	private TaskEndedEvent taskEnded(double time, TaxiTaskType taskType, int taskIndex, Id<Link> linkId) {
		return new TaskEndedEvent(time, mode, vehicle, driver, taskType, taskIndex, linkId);
	}

	private static class TestEventHandler implements TaskStartedEventHandler, TaskEndedEventHandler {
		private final List<Event> handledEvents = new ArrayList<>();

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
