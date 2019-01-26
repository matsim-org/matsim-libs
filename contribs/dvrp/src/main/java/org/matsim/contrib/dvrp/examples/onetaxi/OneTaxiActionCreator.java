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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.SinglePassengerDropoffActivity;
import org.matsim.contrib.dvrp.passenger.SinglePassengerPickupActivity;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.dynagent.IdleDynActivity;
import org.matsim.core.mobsim.framework.MobsimTimer;

import com.google.inject.Inject;

/**
 * @author michalm
 */
final class OneTaxiActionCreator implements VrpAgentLogic.DynActionCreator {
	private final PassengerEngine passengerEngine;
	private final MobsimTimer timer;
	private final String mobsimMode;

	@Inject
	public OneTaxiActionCreator(@DvrpMode(TransportMode.taxi) PassengerEngine passengerEngine, MobsimTimer timer,
			DvrpConfigGroup dvrpCfg) {
		this.passengerEngine = passengerEngine;
		this.timer = timer;
		this.mobsimMode = dvrpCfg.getMobsimMode();
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();
		if (task instanceof DriveTask) {
			return VrpLegFactory.createWithOfflineTracker(mobsimMode, vehicle, timer);
		} else if (task instanceof OneTaxiServeTask) { // PICKUP or DROPOFF
			final OneTaxiServeTask serveTask = (OneTaxiServeTask)task;

			if (serveTask.isPickup()) {
				return new SinglePassengerPickupActivity(passengerEngine, dynAgent, serveTask, serveTask.getRequest(),
						"OneTaxiPickup");
			} else {
				return new SinglePassengerDropoffActivity(passengerEngine, dynAgent, serveTask, serveTask.getRequest(),
						"OneTaxiDropoff");
			}
		} else { // WAIT
			return new IdleDynActivity("OneTaxiStay", task::getEndTime);
		}
	}
}
