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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkLeaveEvent;

/**
 * The micro-simulation internal handler for leaving a road.
 *
 * @author rashid_waraich
 */
public class LeaveRoadMessage extends EventMessage {

	@Override
	public void handleMessage() {
		Road road = (Road) this.getReceivingUnit();
		road.leaveRoad(vehicle, getMessageArrivalTime());
	}

	public LeaveRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
		priority = JDEQSimConfigGroup.PRIORITY_LEAVE_ROAD_MESSAGE;
	}

	@Override
	public void processEvent() {
		Road road = (Road) this.getReceivingUnit();
		Event event = null;

		event = new LinkLeaveEvent(this.getMessageArrivalTime(), Id.create(vehicle.getOwnerPerson().getId(), org.matsim.vehicles.Vehicle.class), road.getLink().getId());

		eventsManager.processEvent(event);
	}

}
