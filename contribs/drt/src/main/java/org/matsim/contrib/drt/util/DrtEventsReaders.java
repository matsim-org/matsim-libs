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

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import java.util.List;
import java.util.Map;

import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.util.DvrpEventsReaders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.MatsimEventsReader.CustomEventMapper;

import com.google.common.collect.ImmutableMap;

import one.util.streamex.StreamEx;

public final class DrtEventsReaders {
	public static final List<DrtTaskType> STANDARD_TASK_TYPES = List.of(DrtDriveTask.TYPE, DefaultDrtStopTask.TYPE,
			DrtStayTask.TYPE, EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE);

	public static MatsimEventsReader createEventsReader(EventsManager eventsManager,
			DrtTaskType... nonStandardTaskTypes) {
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);

		var taskTypeByString = StreamEx.of(STANDARD_TASK_TYPES)
				.append(nonStandardTaskTypes)
				.collect(toImmutableMap(DrtTaskType::name, type -> type));

		Map<String, CustomEventMapper> customEventMappers = ImmutableMap.<String, CustomEventMapper>builder()
				.putAll(DvrpEventsReaders.createCustomEventMappers(taskTypeByString::get))
				.put(DrtRequestSubmittedEvent.EVENT_TYPE, DrtRequestSubmittedEvent::convert)
				.build();

		customEventMappers.forEach(reader::addCustomEventMapper);
		return reader;
	}
}
