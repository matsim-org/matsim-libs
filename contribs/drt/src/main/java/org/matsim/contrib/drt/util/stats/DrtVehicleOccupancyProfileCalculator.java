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
package org.matsim.contrib.drt.util.stats;

import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtVehicleOccupancyProfileCalculator {
	private final Fleet fleet;
	private final TimeDiscretizer timeDiscretizer;

	private final long[] idleVehicleProfileInSeconds;
	private final long[][] vehicleOccupancyProfilesInSeconds;

	private final double[] idleVehicleProfileRelative;
	private final double[][] vehicleOccupancyProfilesRelative;

	public DrtVehicleOccupancyProfileCalculator(Fleet fleet, int timeInterval) {
		this.fleet = fleet;

		Max maxCapacity = new Max();
		Max maxTime = new Max();
		for (DvrpVehicle v : fleet.getVehicles().values()) {
			maxCapacity.increment(v.getCapacity());
			maxTime.increment(v.getSchedule().getEndTime());
		}

		int intervalCount = (int)Math.ceil((maxTime.getResult() + 1) / timeInterval);
		timeDiscretizer = new TimeDiscretizer(intervalCount * timeInterval, timeInterval, TimeDiscretizer.Type.ACYCLIC);

		int occupancyProfilesCount = (int)maxCapacity.getResult() + 1;
		vehicleOccupancyProfilesInSeconds = new long[occupancyProfilesCount][timeDiscretizer.getIntervalCount()];
		idleVehicleProfileInSeconds = new long[timeDiscretizer.getIntervalCount()];

		vehicleOccupancyProfilesRelative = new double[occupancyProfilesCount][timeDiscretizer.getIntervalCount()];
		idleVehicleProfileRelative = new double[timeDiscretizer.getIntervalCount()];
	}

	public void calculate() {
		for (DvrpVehicle v : fleet.getVehicles().values()) {
			updateProfiles(v);
		}
		for (int t = 0; t < timeDiscretizer.getIntervalCount(); t++) {
			idleVehicleProfileRelative[t] = (double)idleVehicleProfileInSeconds[t] / timeDiscretizer.getTimeInterval();
			for (int o = 0; o < vehicleOccupancyProfilesInSeconds.length; o++) {
				vehicleOccupancyProfilesRelative[o][t] =
						(double)vehicleOccupancyProfilesInSeconds[o][t] / timeDiscretizer.getTimeInterval();
			}
		}
	}

	public int getMaxCapacity() {
		return vehicleOccupancyProfilesInSeconds.length - 1;
	}

	public double[] getIdleVehicleProfile() {
		return idleVehicleProfileRelative;
	}

	public double[][] getVehicleOccupancyProfiles() {
		return vehicleOccupancyProfilesRelative;
	}

	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}

	private void updateProfiles(DvrpVehicle vehicle) {
		int occupancy = 0;
		for (Task t : vehicle.getSchedule().getTasks()) {
			DrtTask drtTask = (DrtTask)t;
			switch (drtTask.getDrtTaskType()) {
				case DRIVE:
					increment(vehicleOccupancyProfilesInSeconds[occupancy], drtTask);
					break;

				case STOP:
					DrtStopTask stopTask = (DrtStopTask)drtTask;
					occupancy -= stopTask.getDropoffRequests().size();
					increment(vehicleOccupancyProfilesInSeconds[occupancy], drtTask);
					occupancy += stopTask.getPickupRequests().size();
					break;

				case STAY:
					increment(idleVehicleProfileInSeconds, drtTask);
					break;
			}
		}
	}

	private void increment(long[] values, Task task) {
		int timeInterval = timeDiscretizer.getTimeInterval();
		int fromIdx = timeDiscretizer.getIdx(task.getBeginTime());
		int toIdx = timeDiscretizer.getIdx(task.getEndTime());

		for (int i = fromIdx; i < toIdx; i++) {
			values[i] += timeInterval;
		}

		//reduce first time bin
		values[fromIdx] -= (int)task.getBeginTime() % timeInterval;

		//handle last time bin
		values[toIdx] += (int)task.getEndTime() % timeInterval;
	}
}
