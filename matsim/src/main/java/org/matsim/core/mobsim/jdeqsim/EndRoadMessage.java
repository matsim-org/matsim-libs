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
 * The micro-simulation internal handler, when the end of a road is reached.
 *
 * @author rashid_waraich
 */
public class EndRoadMessage extends EventMessage {

	@Override
	public void handleMessage() {
		if (vehicle.isCurrentLegFinished()) {
			/*
			 * the leg is completed, try to enter the last link but do not enter
			 * it (just wait, until you have clearance for enter and then leave
			 * the road)
			 */

			vehicle.initiateEndingLegMode();
			vehicle.moveToFirstLinkInNextLeg();
			Road road = Road.getRoad(vehicle.getCurrentLinkId());
			road.enterRequest(vehicle, getMessageArrivalTime());
		} else if (!vehicle.isCurrentLegFinished()) {
			// if leg is not finished yet
			vehicle.moveToNextLinkInLeg();

			Road nextRoad = Road.getRoad(vehicle.getCurrentLinkId());
			nextRoad.enterRequest(vehicle, getMessageArrivalTime());
		}
	}

	public EndRoadMessage(Scheduler scheduler, Vehicle vehicle) {
		super(scheduler, vehicle);
	}

	@Override
	public void processEvent() {
		// don't need to output any event
	}

}
