/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.vrpagent;

import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.tracker.TaskTrackers;
import org.matsim.core.mobsim.framework.MobsimTimer;

public class VrpLegs {
	public static VrpLeg createLegWithOfflineTracker(Vehicle vehicle, MobsimTimer timer) {
		DriveTask driveTask = (DriveTask)vehicle.getSchedule().getCurrentTask();
		VrpLeg leg = new VrpLeg(driveTask.getPath());
		TaskTrackers.initOfflineTaskTracking(driveTask, timer);
		return leg;
	}

	public static VrpLeg createLegWithOnlineTracker(Vehicle vehicle, VrpOptimizerWithOnlineTracking optimizer,
			MobsimTimer timer) {
		DriveTask driveTask = (DriveTask)vehicle.getSchedule().getCurrentTask();
		VrpLeg leg = new VrpLeg(driveTask.getPath());
		TaskTrackers.initOnlineDriveTaskTracking(vehicle, leg, optimizer, timer);
		return leg;
	}

	public static interface LegCreator {
		/**
		 * @param vehicle
		 *            for which the leg is created
		 * @return fully initialised vrp leg (e.g. with online tracking, etc.)
		 */
		VrpLeg createLeg(Vehicle vehicle);
	}

	public static LegCreator createLegWithOfflineTrackerCreator(final MobsimTimer timer) {
		return vehicle -> createLegWithOfflineTracker(vehicle, timer);
	}

	public static LegCreator createLegWithOnlineTrackerCreator(final VrpOptimizerWithOnlineTracking optimizer,
			final MobsimTimer timer) {
		return vehicle -> createLegWithOnlineTracker(vehicle, optimizer, timer);
	}
}
