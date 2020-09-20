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

package org.matsim.contrib.drt.passenger.events;

import java.util.Map;

import org.matsim.contrib.dvrp.passenger.DvrpPassengerEventsReaders;
import org.matsim.core.events.MatsimEventsReader.CustomEventMapper;

import com.google.common.collect.ImmutableMap;

public final class DrtPassengerEventsReader {
	public static final Map<String, CustomEventMapper> CUSTOM_EVENT_MAPPERS = ImmutableMap.<String, CustomEventMapper>builder()
			.putAll(DvrpPassengerEventsReaders.CUSTOM_EVENT_MAPPERS)
			.put(DrtRequestSubmittedEvent.EVENT_TYPE, DrtRequestSubmittedEvent::convert)
			.build();
}
