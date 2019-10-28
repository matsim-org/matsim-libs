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

package org.matsim.contrib.drt.optimizer;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.VehicleData.EntryFactory;
import org.matsim.contrib.drt.optimizer.VehicleData.Stop;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.drt.schedule.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import com.google.common.collect.ImmutableList;

/**
 * @author michalm
 */
public class VehicleDataEntryFactoryImpl implements EntryFactory {
	private final double lookAhead;

	public VehicleDataEntryFactoryImpl(DrtConfigGroup drtCfg) {
		lookAhead = drtCfg.getMaxWaitTime() - drtCfg.getStopDuration();
		if (lookAhead < 0) {
			throw new IllegalArgumentException(
					DrtConfigGroup.MAX_WAIT_TIME + " must not be smaller than " + DrtConfigGroup.STOP_DURATION);
		}
	}

	public Entry create(DvrpVehicle vehicle, double currentTime) {
		if (!isEligibleForRequestInsertion(vehicle, currentTime)) {
			return null;
		}

		Schedule schedule = vehicle.getSchedule();
		@SuppressWarnings("unchecked")
		List<DrtTask> tasks = (List<DrtTask>)schedule.getTasks();

		LinkTimePair start;
		int nextTaskIdx;
		if (schedule.getStatus() == ScheduleStatus.STARTED) {
			DrtTask currentTask = (DrtTask)schedule.getCurrentTask();
			switch (currentTask.getDrtTaskType()) {
				case DRIVE:
					DrtDriveTask driveTask = (DrtDriveTask)currentTask;
					start = ((OnlineDriveTaskTracker)driveTask.getTaskTracker()).getDiversionPoint();
					if (start == null) { // too late to divert a vehicle
						start = new LinkTimePair(driveTask.getPath().getToLink(), driveTask.getEndTime());
					}
					break;

				case STOP:
					DrtStopTask stopTask = (DrtStopTask)currentTask;
					start = new LinkTimePair(stopTask.getLink(), stopTask.getEndTime());
					break;

				case STAY:
					DrtStayTask stayTask = (DrtStayTask)currentTask;
					start = new LinkTimePair(stayTask.getLink(), currentTime);
					break;

				default:
					throw new RuntimeException();
			}

			nextTaskIdx = currentTask.getTaskIdx() + 1;
		} else { // PLANNED
			start = new LinkTimePair(vehicle.getStartLink(), vehicle.getServiceBeginTime());
			nextTaskIdx = 0;
		}

		List<DrtStopTask> stopTasks = new ArrayList<>();
		for (DrtTask task : tasks.subList(nextTaskIdx, tasks.size())) {
			if (task.getDrtTaskType() == DrtTaskType.STOP) {
				stopTasks.add((DrtStopTask)task);
			}
		}

		Stop[] stops = new Stop[stopTasks.size()];
		int outputOccupancy = 0;
		for (int i = stops.length - 1; i >= 0; i--) {
			Stop s = stops[i] = new Stop(stopTasks.get(i), outputOccupancy);
			outputOccupancy -= s.occupancyChange;
		}

		return new Entry(vehicle, start, outputOccupancy, ImmutableList.copyOf(stops));
	}

	public boolean isEligibleForRequestInsertion(DvrpVehicle vehicle, double currentTime) {
		return !(currentTime + lookAhead < vehicle.getServiceBeginTime() || currentTime >= vehicle.getServiceEndTime());
	}
}
