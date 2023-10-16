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
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.freight.carriers.Carrier;
import org.matsim.freight.carriers.ScheduledTour;
import org.matsim.vehicles.Vehicle;

public interface CarrierEventCreator {

	Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour, int activityCounter, Id<Vehicle> vehicleId);
	// activityCounter is currently needed to get the correct "service" or "pickup" / "delivery" activity out auf the scheduled plan.
	// It is well integrated in the {@link CarrierEventTracker}.
	// Maybe it can be replaced by the correct freight-activity here --> move the getTourElement ... up
	// kmt, Jun22
}
