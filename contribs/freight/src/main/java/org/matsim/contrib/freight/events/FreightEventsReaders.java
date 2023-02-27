/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;

import java.util.Map;

/**
 *  Creates an {@link MatsimEventsReader} that also handles the freight specific events.
 *  <p>
 *  This is a quickfix for teaching and thus _not_ complete. Needs to get completed and secured by unit-testing later (KMT, feb'23)
 *
 * @author kturner (Kai Martins-Turner)
 */
public class FreightEventsReaders {

	public static Map<String, MatsimEventsReader.CustomEventMapper> createCustomEventMappers() {
		return Map.of(
				FreightTourStartEvent.EVENT_TYPE, FreightTourStartEvent::convert, //
				FreightTourEndEvent.EVENT_TYPE, FreightTourEndEvent::convert
				// more will follow later, KMT feb'23
		);
	}

	public static MatsimEventsReader createEventsReader(EventsManager eventsManager) {
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		createCustomEventMappers().forEach(reader::addCustomEventMapper);
		return reader;
	}
}
