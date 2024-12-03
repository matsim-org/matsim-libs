/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.logistics.events;

import java.util.Map;
import java.util.TreeMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.freight.carriers.events.CarrierEventsReaders;
import org.matsim.freight.logistics.LSP;

/**
 * Creates an {@link MatsimEventsReader} that also handles the {@link
 * LSP} specific events.
 *
 * @author kturner (Kai Martins-Turner)
 */
public class LspEventsReader {

  public static Map<String, MatsimEventsReader.CustomEventMapper> createCustomEventMappers() {
    Map<String, MatsimEventsReader.CustomEventMapper> map =
        new TreeMap<>(
            CarrierEventsReaders
                .createCustomEventMappers()); // also get all the Carrier-related EventMapper
    map.put(HandlingInHubStartsEvent.EVENT_TYPE, HandlingInHubStartsEvent::convert);
    return map;
  }

  public static MatsimEventsReader createEventsReader(EventsManager eventsManager) {
    MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
    createCustomEventMappers().forEach(reader::addCustomEventMapper);
    return reader;
  }
}
