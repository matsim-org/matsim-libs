/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.examples.onetaxi;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.*;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;

import com.google.inject.Inject;

/**
 * @author michalm
 */
final class OneTaxiActionCreator implements VrpAgentLogic.DynActionCreator {
	private final PassengerEngine passengerEngine;
	private final MobsimTimer timer;

	@Inject
	public OneTaxiActionCreator(PassengerEngine passengerEngine, MobsimTimer mobsimTimer) {
		this.passengerEngine = passengerEngine;
		this.timer = mobsimTimer;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask(); 
		if (task instanceof DriveTask) {
			return VrpLegs.createLegWithOfflineTracker(vehicle, timer);
		} else if (task instanceof OneTaxiServeTask) { // PICKUP or DROPOFF
			final OneTaxiServeTask serveTask = (OneTaxiServeTask)task;

			if (serveTask.isPickup()) {
				return new SinglePassengerPickupActivity(passengerEngine, dynAgent, serveTask, serveTask.getRequest(),
						OneTaxiOptimizer.PICKUP_DURATION, "OneTaxiPickup");
			} else {
				return new SinglePassengerDropoffActivity(passengerEngine, dynAgent, serveTask, serveTask.getRequest(),
						"OneTaxiDropoff");
			}
		} else { // WAIT
			return new VrpActivity("OneTaxiStay", (StayTask)task);
		}
	}
}
