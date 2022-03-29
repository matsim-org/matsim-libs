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

package org.matsim.contrib.freight.events.eventsCreator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.events.LSPFreightLinkEnterEvent;

/*package-private*/ final class LSPFreightLinkEnterEventCreator implements LSPEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour,
			Id<Person> driverId, int activityCounter) {
		if(event instanceof LinkEnterEvent) {
			LinkEnterEvent enterEvent = (LinkEnterEvent) event;
			return new LSPFreightLinkEnterEvent(carrier.getId(), scheduledTour.getVehicle().getId(), driverId, enterEvent.getLinkId(), enterEvent.getTime(), scheduledTour.getVehicle());
		}
		return null;
	}	
}
