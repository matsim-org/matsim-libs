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

package electric.edrt;

import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTrackerImpl;
import org.matsim.contrib.dvrp.tracker.TaskTrackers;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLeg;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.vsp.ev.dvrp.ChargingActivity;
import org.matsim.vsp.ev.dvrp.EvDvrpVehicle;
import org.matsim.vsp.ev.dvrp.tracker.OfflineETaskTracker;
import org.matsim.vsp.ev.dvrp.tracker.OnlineEDriveTaskTracker;

import com.google.inject.Inject;

import electric.edrt.schedule.EDrtChargingTask;


/**
 * @author michalm
 */
public class EDrtActionCreator implements VrpAgentLogic.DynActionCreator {
	private final DrtActionCreator drtActionCreator;
	private final MobsimTimer timer;

	@Inject
	public EDrtActionCreator(PassengerEngine passengerEngine, VrpOptimizer optimizer, MobsimTimer timer) {
		this.timer = timer;
		drtActionCreator = new DrtActionCreator(passengerEngine, v -> createLeg(v, optimizer, timer));
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

	private static VrpLeg createLeg(Vehicle vehicle, VrpOptimizer optimizer, MobsimTimer timer) {
		DriveTask driveTask = (DriveTask)vehicle.getSchedule().getCurrentTask();
		VrpLeg leg = new VrpLeg(driveTask.getPath());
		OnlineDriveTaskTracker onlineTracker = new OnlineDriveTaskTrackerImpl(vehicle, leg,
				(VrpOptimizerWithOnlineTracking)optimizer, timer);
		OnlineEDriveTaskTracker onlineETracker = new OnlineEDriveTaskTracker((EvDvrpVehicle)vehicle, timer,
				onlineTracker);
		TaskTrackers.initOnlineDriveTaskTracking(vehicle, leg, onlineETracker);
		return leg;
	}
}
