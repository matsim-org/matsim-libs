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
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.events.LSPFreightLinkLeaveEvent;

/*package-private*/  final class LSPFreightLinkLeaveEventCreator implements LSPEventCreator {

	@Override
	public Event createEvent(Event event, Carrier carrier, Activity activity, ScheduledTour scheduledTour, Id<Person> driverId,int activityCounter) {
		if((event instanceof LinkLeaveEvent)) {
			LinkLeaveEvent  leaveEvent = (LinkLeaveEvent) event;
			return new LSPFreightLinkLeaveEvent(carrier.getId(), scheduledTour.getVehicle().getId(), driverId, leaveEvent.getLinkId(), leaveEvent.getTime(), scheduledTour.getVehicle());
		}	
		return null;
	}
}
