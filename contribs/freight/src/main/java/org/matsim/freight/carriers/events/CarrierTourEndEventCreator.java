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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.CarrierConstants;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.vehicles.Vehicle;

/*package-private*/ final class CarrierTourEndEventCreator implements CarrierEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour, int activityCounter, Id<Vehicle> vehicleId) {
		if(event instanceof ActivityStartEvent startEvent && CarrierConstants.END.equals(startEvent.getActType()) ) {
				return new CarrierTourEndEvent(startEvent.getTime(), carrier.getId(), scheduledTour.getTour().getEndLinkId(), // TODO: If we have the tourId, we do not need to store the link here, kmt sep 22
						vehicleId, scheduledTour.getTour().getId());
		}
		return null;
	}
}
