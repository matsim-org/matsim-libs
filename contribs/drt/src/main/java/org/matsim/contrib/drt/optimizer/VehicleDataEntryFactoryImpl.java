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

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.STOP;
import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.getBaseTypeOrElseThrow;

import java.util.ArrayList;
import java.util.List;

import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.DriveTask;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * @author michalm
 */
public class VehicleDataEntryFactoryImpl implements VehicleEntry.EntryFactory {
	private final double lookAhead;

	public VehicleDataEntryFactoryImpl(DrtConfigGroup drtCfg) {
		if (drtCfg.rejectRequestIfMaxWaitOrTravelTimeViolated) {
			lookAhead = drtCfg.maxWaitTime - drtCfg.stopDuration;
			Preconditions.checkArgument(lookAhead >= 0,
					"maxWaitTime must not be smaller than stopDuration");
		} else {
			// if no rejection due to max wait time, the look ahead is infinite
			lookAhead = Double.POSITIVE_INFINITY;
		}
	}

	public VehicleEntry create(DvrpVehicle vehicle, double currentTime) {
		if (isNotEligibleForRequestInsertion(vehicle, currentTime)) {
			return null;
		}

		Schedule schedule = vehicle.getSchedule();
		final LinkTimePair start;
		final Task startTask;
		int nextTaskIdx;
		if (schedule.getStatus() == ScheduleStatus.STARTED) {
			startTask = schedule.getCurrentTask();
			start = switch (getBaseTypeOrElseThrow(startTask)) {
				case DRIVE -> {
					var driveTask = (DriveTask)startTask;
					var diversionPoint = ((OnlineDriveTaskTracker)driveTask.getTaskTracker()).getDiversionPoint();
					yield diversionPoint != null ? diversionPoint : //diversion possible
							new LinkTimePair(driveTask.getPath().getToLink(),
									driveTask.getEndTime());// too late for diversion
				}
				case STOP -> new LinkTimePair(((DrtStopTask)startTask).getLink(), startTask.getEndTime());
				case STAY -> new LinkTimePair(((StayTask)startTask).getLink(), currentTime);
			};

			nextTaskIdx = startTask.getTaskIdx() + 1;
		} else { // PLANNED
			start = new LinkTimePair(vehicle.getStartLink(), vehicle.getServiceBeginTime());
			startTask = null;
			nextTaskIdx = 0;
		}

		List<? extends Task> tasks = schedule.getTasks();
		List<DrtStopTask> stopTasks = new ArrayList<>();
		for (Task task : tasks.subList(nextTaskIdx, tasks.size())) {
			if (STOP.isBaseTypeOf(task)) {
				stopTasks.add((DrtStopTask)task);
			}
		}

		Waypoint.Stop[] stops = new Waypoint.Stop[stopTasks.size()];
		int outgoingOccupancy = 0;
		for (int i = stops.length - 1; i >= 0; i--) {
			Waypoint.Stop s = stops[i] = new Waypoint.Stop(stopTasks.get(i), outgoingOccupancy);
			outgoingOccupancy -= s.getOccupancyChange();
		}

		var slackTimes = computeSlackTimes(vehicle, currentTime, stops);

		return new VehicleEntry(vehicle, new Waypoint.Start(startTask, start.link, start.time, outgoingOccupancy),
				ImmutableList.copyOf(stops), slackTimes);
	}

	public boolean isNotEligibleForRequestInsertion(DvrpVehicle vehicle, double currentTime) {
		return currentTime + lookAhead < vehicle.getServiceBeginTime() || currentTime >= vehicle.getServiceEndTime();
	}

	static double[] computeSlackTimes(DvrpVehicle vehicle, double now, Waypoint.Stop[] stops) {
		double[] slackTimes = new double[stops.length + 1];

		//vehicle
		double slackTime = calcVehicleSlackTime(vehicle, now);
		slackTimes[stops.length] = slackTime;

		//stops
		for (int i = stops.length - 1; i >= 0; i--) {
			var stop = stops[i];
			slackTime = Math.min(stop.latestArrivalTime - stop.task.getBeginTime(), slackTime);
			slackTime = Math.min(stop.latestDepartureTime - stop.task.getEndTime(), slackTime);
			slackTimes[i] = slackTime;
		}
		return slackTimes;
	}

	static double calcVehicleSlackTime(DvrpVehicle vehicle, double now) {
		var lastTask = Schedules.getLastTask(vehicle.getSchedule());
		//if the last task is started, take 'now', otherwise take the planned begin time
		double availableFromTime = Math.max(lastTask.getBeginTime(), now);
		//for an already delayed vehicle, assume slack is 0 (instead of a negative number)
		return Math.max(0, vehicle.getServiceEndTime() - availableFromTime);
	}
}
