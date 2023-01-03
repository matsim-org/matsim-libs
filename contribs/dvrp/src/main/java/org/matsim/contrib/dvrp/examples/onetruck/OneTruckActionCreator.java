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

package org.matsim.contrib.dvrp.examples.onetruck;

import static org.matsim.contrib.dvrp.examples.onetruck.OneTruckOptimizer.OneTruckTaskType;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
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
final class OneTruckActionCreator implements VrpAgentLogic.DynActionCreator {
	private final MobsimTimer timer;

	@Inject
	public OneTruckActionCreator(MobsimTimer timer) {
		this.timer = timer;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();
		switch ((OneTruckTaskType)task.getTaskType()) {
			case EMPTY_DRIVE:
			case LOADED_DRIVE:
				return VrpLegFactory.createWithOfflineTracker(TransportMode.truck, vehicle, timer);

			case PICKUP:
			case DELIVERY:
			case WAIT:
				return new IdleDynActivity(task.getTaskType() + "", task::getEndTime);

			default:
				throw new IllegalStateException();
		}
	}
}
