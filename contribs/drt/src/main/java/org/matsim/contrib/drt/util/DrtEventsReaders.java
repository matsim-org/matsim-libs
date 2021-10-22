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
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskType;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.dvrp.util.DvrpEventsReaders;
import org.matsim.contrib.drt.extension.edrt.schedule.EDrtChargingTask;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.MatsimEventsReader.CustomEventMapper;

import com.google.common.collect.ImmutableMap;

public final class DrtEventsReaders {
	public static final Map<String, DrtTaskType> TASK_TYPE_MAP = List.of(DrtDriveTask.TYPE, DrtStopTask.TYPE,
			DrtStayTask.TYPE, EmptyVehicleRelocator.RELOCATE_VEHICLE_TASK_TYPE, EDrtChargingTask.TYPE)
			.stream()
			.collect(toImmutableMap(DrtTaskType::name, type -> type));

	public static final Map<String, CustomEventMapper> CUSTOM_EVENT_MAPPERS = ImmutableMap.<String, CustomEventMapper>builder()
			.putAll(DvrpEventsReaders.createCustomEventMappers(TASK_TYPE_MAP::get))
			.put(DrtRequestSubmittedEvent.EVENT_TYPE, DrtRequestSubmittedEvent::convert)
			.build();

	public static MatsimEventsReader createEventsReader(EventsManager eventsManager) {
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		CUSTOM_EVENT_MAPPERS.forEach(reader::addCustomEventMapper);
		return reader;
	}
}
