/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.michalm.drt.optimizer;

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import playground.michalm.drt.schedule.*;
import playground.michalm.drt.schedule.NDrtTask.NDrtTaskType;

/**
 * @author michalm
 */
public class VehicleData {

	public static class Entry {
		public Vehicle vehicle;
		public LinkTimePair start;
		public List<NDrtStopTask> stopTasks = new ArrayList<>();

		public Entry(Vehicle vehicle, LinkTimePair start) {
			this.vehicle = vehicle;
			this.start = start;
		}
	}

	private final List<Entry> entries = new ArrayList<>();
	private final double currTime;
	private final boolean vehicleDiversion;

	public VehicleData(DrtOptimizerContext optimContext, Iterable<? extends Vehicle> vehicles) {
		currTime = optimContext.timer.getTimeOfDay();
		vehicleDiversion = optimContext.scheduler.getParams().vehicleDiversion;

		for (Vehicle v : vehicles) {
			Entry e = createVehicleData(v);
			if (e != null) {
				entries.add(e);
			}
		}
	}

	private Entry createVehicleData(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		ScheduleStatus status = schedule.getStatus();
		if (currTime >= vehicle.getServiceEndTime() || // too late
				status != ScheduleStatus.PLANNED || status != ScheduleStatus.STARTED) {
			return null;
		}

		@SuppressWarnings("unchecked")
		List<NDrtTask> tasks = (List<NDrtTask>)schedule.getTasks();
		NDrtTask currentTask = (NDrtTask)schedule.getCurrentTask();

		LinkTimePair start;
		int nextTaskIdx;
		if (status == ScheduleStatus.STARTED) {
			switch (currentTask.getDrtTaskType()) {
				case DRIVE:
					if (vehicleDiversion) {
						start = ((OnlineDriveTaskTracker)currentTask.getTaskTracker()).getDiversionPoint();
					} else {
						Link link = ((NDrtDriveTask)currentTask).getPath().getToLink();
						start = new LinkTimePair(link, currentTask.getEndTime());
					}

					break;

				case STOP:
					NDrtStopTask stopTask = (NDrtStopTask)currentTask;
					start = new LinkTimePair(stopTask.getLink(), stopTask.getEndTime());
					break;

				case STAY:
					NDrtStopTask stayTask = (NDrtStopTask)currentTask;
					start = new LinkTimePair(stayTask.getLink(), currTime);
					break;

				default:
					throw new RuntimeException();
			}

			nextTaskIdx = currentTask.getTaskIdx() + 1;
		} else { // PLANNED
			start = new LinkTimePair(vehicle.getStartLink(), vehicle.getServiceBeginTime());
			nextTaskIdx = 0;
		}

		Entry data = new Entry(vehicle, start);
		for (int i = nextTaskIdx; i < tasks.size(); i++) {
			NDrtTask task = tasks.get(i);
			if (task.getDrtTaskType() == NDrtTaskType.STOP) {
				data.stopTasks.add((NDrtStopTask)task);
			}
		}

		return data;
	}

	public int getSize() {
		return entries.size();
	}

	public Entry getEntry(int idx) {
		return entries.get(idx);
	}

	public List<Entry> getEntries() {
		return entries;
	}
}