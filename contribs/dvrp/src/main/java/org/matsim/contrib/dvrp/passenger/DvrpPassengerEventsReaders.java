/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.passenger;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;

import com.google.common.collect.ImmutableMap;

public class DvrpPassengerEventsReaders {
	public static final ImmutableMap<String, MatsimEventsReader.CustomEventMapper> CUSTOM_EVENT_MAPPERS = ImmutableMap.of(
			PassengerRequestSubmittedEvent.EVENT_TYPE, PassengerRequestSubmittedEvent::convert,//
			PassengerRequestScheduledEvent.EVENT_TYPE, PassengerRequestScheduledEvent::convert,//
			PassengerRequestRejectedEvent.EVENT_TYPE, PassengerRequestRejectedEvent::convert,//
			PassengerPickedUpEvent.EVENT_TYPE, PassengerPickedUpEvent::convert, //
			PassengerDroppedOffEvent.EVENT_TYPE, PassengerDroppedOffEvent::convert);

	public static MatsimEventsReader createEventsReader(EventsManager eventsManager) {
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		CUSTOM_EVENT_MAPPERS.forEach(reader::addCustomEventMapper);
		return reader;
	}
}
