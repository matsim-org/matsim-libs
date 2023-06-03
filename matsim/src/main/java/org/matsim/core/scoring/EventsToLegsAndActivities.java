
/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToLegsAndActivities.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.scoring;

import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;

import jakarta.inject.Inject;

/**
 * For TripScoring to work correctly, activities and legs must be created in the correct order.
 * Thus combine the two EventHandlers {@link EventsToLegs} and {@link EventsToActivities} in one, so it will run in the same thread when handling events.
 *
 * @author mrieser / Simunto GmbH
 */
public class EventsToLegsAndActivities implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, LinkEnterEventHandler,
		TeleportationArrivalEventHandler, TransitDriverStartsEventHandler, PersonEntersVehicleEventHandler, VehicleArrivesAtFacilityEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private final EventsToLegs legsDelegate;
	private final EventsToActivities actsDelegate;

	@Inject
	public EventsToLegsAndActivities(EventsToLegs eventsToLegs, EventsToActivities eventsToActivities) {
		this.legsDelegate = eventsToLegs;
		this.actsDelegate = eventsToActivities;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		this.actsDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.actsDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.legsDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		this.legsDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		this.legsDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		this.legsDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		this.legsDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.legsDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.legsDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		this.legsDelegate.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		this.legsDelegate.handleEvent(event);
	}

	@Override
	public void reset(int iteration) {
		this.actsDelegate.reset(iteration);
		this.legsDelegate.reset(iteration);
	}
}
