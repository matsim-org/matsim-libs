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

package org.matsim.contrib.ev.dvrp;

import org.matsim.contrib.dvrp.path.VrpPath;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.ev.data.ElectricVehicle;
import org.matsim.contrib.ev.discharging.DriveEnergyConsumption;

/**
 * @author michalm
 */
public class TaskEnergyConsumptions {
	public static double calcTotalEnergyConsumption(ElectricVehicle ev, Task task) {
		double driveEnergy = task instanceof DriveTask ? calcDriveEnergy(ev, ((DriveTask)task).getPath()) : 0;
		return calcAuxEnergy(ev, task) + driveEnergy;
	}

	public static double calcTotalEnergyConsumption(ElectricVehicle ev, VrpPathWithTravelData path) {
		return calcAuxEnergy(ev, path.getDepartureTime(), path.getArrivalTime()) + calcDriveEnergy(ev, path);
	}

	public static double calcAuxEnergy(ElectricVehicle ev, Task task) {
		return calcAuxEnergy(ev, task.getBeginTime(), task.getEndTime());
	}

	public static double calcAuxEnergy(ElectricVehicle ev, double beginTime, double endTime) {
		return ev.getAuxEnergyConsumption().calcEnergyConsumption(endTime - beginTime);
	}

	public static double calcDriveEnergy(ElectricVehicle ev, VrpPath path) {
		return calcRemainingDriveEnergy(ev, path, 1);// skip first link
	}

	public static double calcRemainingDriveEnergy(ElectricVehicle ev, VrpPath path, int currentLinkIdx) {
		DriveEnergyConsumption consumption = ev.getDriveEnergyConsumption();
		double energy = 0;
		for (int i = Math.max(currentLinkIdx, 1); i < path.getLinkCount(); i++) {// skip first link
			energy += consumption.calcEnergyConsumption(path.getLink(i), path.getLinkTravelTime(i));
		}
		return energy;
	}
}
