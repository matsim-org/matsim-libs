/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonDepartureEvent;

/**
 * The micro-simulation internal handler for starting a leg.
 * 
 * @author rashid_waraich
 */
public class StartingLegMessage extends EventMessage {

	public StartingLegMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
		priority = JDEQSimConfigGroup.PRIORITY_DEPARTUARE_MESSAGE;
	}

	@Override
	public void handleMessage() {
		// if current leg is in car mode, then enter request in first road
		if (vehicle.getCurrentLeg().getMode().equals(TransportMode.car)) {

			// if empty leg, then end leg, else simulate leg
			if (vehicle.getCurrentLinkRoute().length == 0) {
				// move to first link in next leg and schedule an end leg
				// message
				// duration of leg = 0 (departure and arrival time is the same)
				scheduleEndLegMessage(getMessageArrivalTime());

			} else {
				// start the new leg
				Road road = Road.getRoad(vehicle.getCurrentLinkId());
				road.enterRequest(vehicle, getMessageArrivalTime());
			}

		} else {
			scheduleEndLegMessage(getMessageArrivalTime() + vehicle.getCurrentLeg().getTravelTime());
		}
	}

	private void scheduleEndLegMessage(double time) {
		// move to first link in next leg and schedule an end leg message
		vehicle.moveToFirstLinkInNextLeg();
		Road road = Road.getRoad(vehicle.getCurrentLinkId());
		vehicle.scheduleEndLegMessage(time, road);
	}

	@Override
	public void processEvent() {
		Event event;

		// schedule ActEndEvent
		event = new ActivityEndEvent(this.getMessageArrivalTime(), vehicle.getOwnerPerson().getId(), vehicle.getCurrentLinkId(), vehicle
				.getPreviousActivity().getFacilityId(), vehicle.getPreviousActivity().getType());
		eventsManager.processEvent(event);

		// schedule AgentDepartureEvent
		event = new PersonDepartureEvent(this.getMessageArrivalTime(), vehicle.getOwnerPerson().getId(), vehicle.getCurrentLinkId(),
				vehicle.getCurrentLeg().getMode());

		eventsManager.processEvent(event);

	}

}
