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

import static org.matsim.contrib.dvrp.examples.onetaxi.OneTaxiOptimizer.OneTaxiTaskType;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.passenger.SinglePassengerDropoffActivity;
import org.matsim.contrib.dvrp.passenger.SinglePassengerPickupActivity;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpMode;
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
public final class OneTaxiActionCreator implements VrpAgentLogic.DynActionCreator {
	private final PassengerHandler passengerHandler;
	private final MobsimTimer timer;
	private final String mobsimMode;

	@Inject
	public OneTaxiActionCreator(@DvrpMode(TransportMode.taxi) PassengerHandler passengerHandler, MobsimTimer timer,
			DvrpConfigGroup dvrpCfg) {
		this.passengerHandler = passengerHandler;
		this.timer = timer;
		this.mobsimMode = dvrpCfg.getMobsimMode();
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();
		switch ((OneTaxiTaskType)task.getTaskType()) {
			case EMPTY_DRIVE:
			case OCCUPIED_DRIVE:
				return VrpLegFactory.createWithOfflineTracker(mobsimMode, vehicle, timer);

			case PICKUP:
				OneTaxiServeTask pickupTask = (OneTaxiServeTask)task;
				return new SinglePassengerPickupActivity(passengerHandler, dynAgent, pickupTask,
						pickupTask.getRequest(), "OneTaxiPickup");

			case DROPOFF:
				OneTaxiServeTask dropoffTask = (OneTaxiServeTask)task;
				return new SinglePassengerDropoffActivity(passengerHandler, dynAgent, dropoffTask,
						dropoffTask.getRequest(), "OneTaxiDropoff");

			case WAIT:
				return new IdleDynActivity("OneTaxiStay", task::getEndTime);

			default:
				throw new IllegalStateException();
		}
	}
}
