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

import java.util.Map;
import java.util.function.Function;

import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerWaitingEvent;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.VehicleCapacityChangedEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;

public class DvrpEventsReaders {

	public static Map<String, MatsimEventsReader.CustomEventMapper> createCustomEventMappers(
			Function<String, Task.TaskType> stringToTaskTypeConverter) {
		return Map.of(PassengerRequestSubmittedEvent.EVENT_TYPE, PassengerRequestSubmittedEvent::convert,//
				PassengerRequestScheduledEvent.EVENT_TYPE, PassengerRequestScheduledEvent::convert,//
				PassengerRequestRejectedEvent.EVENT_TYPE, PassengerRequestRejectedEvent::convert,//
				PassengerWaitingEvent.EVENT_TYPE, PassengerWaitingEvent::convert,//
				PassengerPickedUpEvent.EVENT_TYPE, PassengerPickedUpEvent::convert, //
				PassengerDroppedOffEvent.EVENT_TYPE, PassengerDroppedOffEvent::convert,//
				TaskStartedEvent.EVENT_TYPE, e -> TaskStartedEvent.convert(e, stringToTaskTypeConverter),
				TaskEndedEvent.EVENT_TYPE, e -> TaskEndedEvent.convert(e, stringToTaskTypeConverter),
				VehicleCapacityChangedEvent.EVENT_TYPE, VehicleCapacityChangedEvent::convert);
	}

	public static MatsimEventsReader createEventsReader(EventsManager eventsManager,
			Function<String, Task.TaskType> stringToTaskTypeConverter) {
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		createCustomEventMappers(stringToTaskTypeConverter).forEach(reader::addCustomEventMapper);
		return reader;
	}
}
