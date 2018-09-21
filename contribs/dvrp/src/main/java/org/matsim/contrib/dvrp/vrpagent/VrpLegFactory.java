/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

/**
 * @author michalm
 */
public interface VrpLegFactory {
	/**
	 * @param vehicle for which the leg is created
	 * @return fully initialised VrpLeg (e.g. with online tracking, etc.)
	 */
	VrpLeg create(Vehicle vehicle);

	static VrpLeg createWithOfflineTracker(String mode, Vehicle vehicle, MobsimTimer timer) {
		DriveTask driveTask = (DriveTask)vehicle.getSchedule().getCurrentTask();
		VrpLeg leg = new VrpLeg(mode, driveTask.getPath());
		TaskTrackers.initOfflineTaskTracking(driveTask, timer);
		return leg;
	}

	static VrpLeg createWithOnlineTracker(String mode, Vehicle vehicle, VrpOptimizerWithOnlineTracking optimizer,
			MobsimTimer timer) {
		DriveTask driveTask = (DriveTask)vehicle.getSchedule().getCurrentTask();
		VrpLeg leg = new VrpLeg(mode, driveTask.getPath());
		TaskTrackers.initOnlineDriveTaskTracking(vehicle, leg, optimizer, timer);
		return leg;
	}
}
