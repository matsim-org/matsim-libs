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

package org.matsim.contrib.freight.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.FreightConstants;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.carrier.Tour.Delivery;
import org.matsim.contrib.freight.carrier.Tour.TourElement;
import org.matsim.vehicles.Vehicle;

/*package-private*/  final class FreightShipmentDeliveryEndEventCreator implements FreightEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour, int activityCounter, Id<Vehicle> vehicleId) {
		if(event instanceof ActivityEndEvent endEvent && FreightConstants.DELIVERY.equals(endEvent.getActType()) ) {
			TourElement element = scheduledTour.getTour().getTourElements().get(activityCounter);
			if (element instanceof Delivery deliveryActivity) {
				return new FreightShipmentDeliveryEndEvent(event.getTime(), carrier.getId(), deliveryActivity.getShipment(), vehicleId );
			}
		}
		return null;
	}
}
