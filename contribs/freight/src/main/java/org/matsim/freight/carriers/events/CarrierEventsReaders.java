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

package org.matsim.freight.carriers.events;

import java.util.Map;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;

/**
 *  Creates an {@link MatsimEventsReader} that also handles the carrier specific events.
 *  <p>
 *  This is a quickfix for teaching and thus _not_ complete. Needs to get completed and secured by unit-testing later (KMT, feb'23)
 *
 * @author kturner (Kai Martins-Turner)
 */
public class CarrierEventsReaders {

	public static Map<String, MatsimEventsReader.CustomEventMapper> createCustomEventMappers() {
		return Map.of(
				CarrierServiceStartEvent.EVENT_TYPE, CarrierServiceStartEvent::convert,
				CarrierServiceEndEvent.EVENT_TYPE, CarrierServiceEndEvent::convert,
				CarrierShipmentDeliveryStartEvent.EVENT_TYPE, CarrierShipmentDeliveryStartEvent::convert,
				CarrierShipmentDeliveryEndEvent.EVENT_TYPE, CarrierShipmentDeliveryEndEvent::convert,
				CarrierShipmentPickupStartEvent.EVENT_TYPE, CarrierShipmentPickupStartEvent::convert,
				CarrierShipmentPickupEndEvent.EVENT_TYPE, CarrierShipmentPickupEndEvent::convert,
				CarrierTourStartEvent.EVENT_TYPE, CarrierTourStartEvent::convert, //
				CarrierTourEndEvent.EVENT_TYPE, CarrierTourEndEvent::convert
		);
	}

	public static MatsimEventsReader createEventsReader(EventsManager eventsManager) {
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		createCustomEventMappers().forEach(reader::addCustomEventMapper);
		return reader;
	}
}
