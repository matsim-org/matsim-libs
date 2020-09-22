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

import static com.google.common.collect.ImmutableMap.toImmutableMap;

import java.util.List;
import java.util.Map;

import org.matsim.contrib.dvrp.util.DvrpEventsReaders;
import org.matsim.contrib.etaxi.ETaxiChargingTask;
import org.matsim.contrib.etaxi.ETaxiScheduler;
import org.matsim.contrib.taxi.schedule.TaxiDropoffTask;
import org.matsim.contrib.taxi.schedule.TaxiEmptyDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiOccupiedDriveTask;
import org.matsim.contrib.taxi.schedule.TaxiPickupTask;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.taxi.schedule.TaxiTaskType;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.MatsimEventsReader.CustomEventMapper;

public final class TaxiEventsReaders {
	public static final Map<String, TaxiTaskType> TASK_TYPE_MAP = //
			List.of(TaxiEmptyDriveTask.TYPE, TaxiPickupTask.TYPE, TaxiOccupiedDriveTask.TYPE, TaxiDropoffTask.TYPE,
					TaxiStayTask.TYPE, ETaxiScheduler.DRIVE_TO_CHARGER, ETaxiChargingTask.TYPE)//
					.stream().collect(toImmutableMap(TaxiTaskType::name, type -> type));

	public static final Map<String, CustomEventMapper> CUSTOM_EVENT_MAPPERS = DvrpEventsReaders.createCustomEventMappers(
			TASK_TYPE_MAP::get);

	public static MatsimEventsReader createEventsReader(EventsManager eventsManager) {
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		CUSTOM_EVENT_MAPPERS.forEach(reader::addCustomEventMapper);
		return reader;
	}
}
