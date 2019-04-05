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

/**
 * The micro-simulation internal handler for preventig deadlocks.
 *
 * @author rashid_waraich
 */
public class DeadlockPreventionMessage extends EventMessage {

	@Override
	// let enter the car into the road immediately
	public void handleMessage() {

		Road road = (Road) this.getReceivingUnit();

		road.incrementPromisedToEnterRoad(); // this will be decremented in
												// enter road
		road.setTimeOfLastEnteringVehicle(getMessageArrivalTime());
		road.removeFirstDeadlockPreventionMessage(this);
		road.removeFromInterestedInEnteringRoad();

		vehicle.scheduleEnterRoadMessage(getMessageArrivalTime(), road);
	}

	public DeadlockPreventionMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
	}

	@Override
	public void processEvent() {
		// don't do anything
	}

}
