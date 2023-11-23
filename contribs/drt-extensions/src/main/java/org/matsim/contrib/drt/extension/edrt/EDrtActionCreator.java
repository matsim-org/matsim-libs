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

package org.matsim.contrib.drt.extension.edrt;

import org.matsim.contrib.drt.extension.edrt.schedule.EDrtChargingTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
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
import org.matsim.contrib.evrp.ChargingActivity;
import org.matsim.contrib.evrp.ChargingTask;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.contrib.evrp.tracker.OfflineETaskTracker;
import org.matsim.contrib.evrp.tracker.OnlineEDriveTaskTracker;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class EDrtActionCreator implements VrpAgentLogic.DynActionCreator {
	private final VrpAgentLogic.DynActionCreator delegate;
	private final MobsimTimer timer;

	public EDrtActionCreator(VrpAgentLogic.DynActionCreator delegate, MobsimTimer timer) {
		this.timer = timer;
		this.delegate = delegate;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();
		if (task.getTaskType().equals(EDrtChargingTask.TYPE)) {
			task.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle)vehicle, timer));
			return new ChargingActivity((ChargingTask)task);
		} else {
			DynAction dynAction = delegate.createAction(dynAgent, vehicle, now);
			if (task.getTaskTracker() == null) {
				task.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle)vehicle, timer));
			}
			return dynAction;
		}
	}

	public static VrpLeg createLeg(String mobsimMode, DvrpVehicle vehicle, MobsimTimer timer) {
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
