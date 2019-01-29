/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.pt;

import java.util.List;

import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser
 */
public class SimpleTransitStopHandler implements TransitStopHandler {

	private TransitStopFacility lastHandledStop = null;

	public SimpleTransitStopHandler() {
	}

	@Override
	public double handleTransitStop(TransitStopFacility stop, double now, List<PTPassengerAgent> leavingPassengers,
			List<PTPassengerAgent> enteringPassengers, PassengerAccessEgress accessEgress, MobsimVehicle vehicle) {
		int cntEgress = leavingPassengers.size();
		int cntAccess = enteringPassengers.size();
		double stopTime = 0;
		if ((cntAccess > 0) || (cntEgress > 0)) {
			stopTime = cntAccess * 4 + cntEgress * 2;
			if (this.lastHandledStop != stop) {
				stopTime += 15.0; // add fixed amount of time for door-operations and similar stuff
			}
			for (PTPassengerAgent passenger : leavingPassengers) {
				accessEgress.handlePassengerLeaving(passenger, vehicle, stop.getLinkId() , now);
			}
			for (PTPassengerAgent passenger : enteringPassengers) {
				accessEgress.handlePassengerEntering(passenger, vehicle, stop.getId(), now);
			}
		}
		this.lastHandledStop = stop;
		return stopTime;
	}

}
