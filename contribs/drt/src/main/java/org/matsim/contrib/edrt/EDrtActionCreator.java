/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.vsp.edvrp.edrt;

import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTrackerImpl;
import org.matsim.contrib.dvrp.tracker.OnlineTrackerListener;
import org.matsim.contrib.dvrp.tracker.TaskTrackers;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLeg;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.ev.dvrp.ChargingActivity;
import org.matsim.contrib.ev.dvrp.EvDvrpVehicle;
import org.matsim.contrib.ev.dvrp.tracker.OfflineETaskTracker;
import org.matsim.contrib.ev.dvrp.tracker.OnlineEDriveTaskTracker;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.vsp.edvrp.edrt.schedule.EDrtChargingTask;

/**
 * @author michalm
 */
public class EDrtActionCreator implements VrpAgentLogic.DynActionCreator {
	private final DrtActionCreator drtActionCreator;
	private final MobsimTimer timer;

	public EDrtActionCreator(PassengerEngine passengerEngine, MobsimTimer timer, DvrpConfigGroup dvrpCfg) {
		this.timer = timer;
		drtActionCreator = new DrtActionCreator(passengerEngine, v -> createLeg(dvrpCfg.getMobsimMode(), v, timer));
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();
		if (task instanceof EDrtChargingTask) {
			task.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle)vehicle, timer));
			return new ChargingActivity((EDrtChargingTask)task);
		} else {
			DynAction dynAction = drtActionCreator.createAction(dynAgent, vehicle, now);
			if (task.getTaskTracker() == null) {
				task.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle)vehicle, timer));
			}
			return dynAction;
		}
	}

	private static VrpLeg createLeg(String mobsimMode, Vehicle vehicle, MobsimTimer timer) {
		DriveTask driveTask = (DriveTask)vehicle.getSchedule().getCurrentTask();
		VrpLeg leg = new VrpLeg(mobsimMode, driveTask.getPath());
		OnlineDriveTaskTracker onlineTracker = new OnlineDriveTaskTrackerImpl(vehicle, leg,
				OnlineTrackerListener.NO_LISTENER, timer);
		OnlineEDriveTaskTracker onlineETracker = new OnlineEDriveTaskTracker((EvDvrpVehicle)vehicle, timer,
				onlineTracker);
		TaskTrackers.initOnlineDriveTaskTracking(vehicle, leg, onlineETracker);
		return leg;
	}
}
