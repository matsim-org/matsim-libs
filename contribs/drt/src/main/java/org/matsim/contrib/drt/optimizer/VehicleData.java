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

package org.matsim.contrib.drt.optimizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.drt.schedule.DrtTask.DrtTaskType;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.util.LinkTimePair;

import com.google.common.collect.ImmutableList;

/**
 * @author michalm
 */
public class VehicleData {
	public static class Entry {
		public final Vehicle vehicle;
		public final LinkTimePair start;
		public final int startOccupancy;
		public final ImmutableList<Stop> stops;

		public Entry(Vehicle vehicle, LinkTimePair start, int startOccupancy, ImmutableList<Stop> stops) {
			this.vehicle = vehicle;
			this.start = start;
			this.startOccupancy = startOccupancy;
			this.stops = stops;
		}
	}

	public static class Stop {
		public final DrtStopTask task;
		public final double maxArrivalTime;// relating to max pass drive time (for dropoff requests)
		public final double maxDepartureTime;// relating to pass max wait time (for pickup requests)
		public final int occupancyChange;// diff in pickups and dropoffs
		public final int outgoingOccupancy;

		public Stop(DrtStopTask task, int outputOccupancy) {
			this.task = task;
			this.outgoingOccupancy = outputOccupancy;

			maxArrivalTime = calcMaxArrivalTime();
			maxDepartureTime = calcMaxDepartureTime();
			occupancyChange = task.getPickupRequests().size() - task.getDropoffRequests().size();
		}

		private double calcMaxArrivalTime() {
			double maxTime = Double.MAX_VALUE;
			for (DrtRequest r : task.getDropoffRequests()) {
				double reqMaxArrivalTime = r.getLatestArrivalTime();
				if (reqMaxArrivalTime < maxTime) {
					maxTime = reqMaxArrivalTime;
				}
			}
			return maxTime;
		}

		private double calcMaxDepartureTime() {
			double maxTime = Double.MAX_VALUE;
			for (DrtRequest r : task.getPickupRequests()) {
				double reqMaxDepartureTime = r.getLatestStartTime();
				if (reqMaxDepartureTime < maxTime) {
					maxTime = reqMaxDepartureTime;
				}
			}
			return maxTime;
		}

		@Override
		public String toString() {
			return "VehicleData.Stop for: " + task.toString();
		}
	}

	private final Map<Id<Vehicle>, Entry> entries;
	private final double currentTime;

	public VehicleData(double currentTime, Stream<? extends Vehicle> vehicles) {
		this.currentTime = currentTime;
		entries = vehicles.map(this::createVehicleData).filter(Objects::nonNull)
				.collect(Collectors.toMap(e -> e.vehicle.getId(), e -> e));
	}

	public void updateEntry(Vehicle vehicle) {
		Entry e = createVehicleData(vehicle);
		if (e != null) {
			entries.put(vehicle.getId(), e);
		} else {
			entries.remove(vehicle.getId());
		}
	}

	private Entry createVehicleData(Vehicle vehicle) {
		Schedule schedule = vehicle.getSchedule();
		ScheduleStatus status = schedule.getStatus();
		if (currentTime <= vehicle.getServiceBeginTime()) {
			return null;
		}
		if (currentTime >= vehicle.getServiceEndTime() || status == ScheduleStatus.COMPLETED) {
			return null;
		}

		@SuppressWarnings("unchecked")
		List<DrtTask> tasks = (List<DrtTask>)schedule.getTasks();
		DrtTask currentTask = (DrtTask)schedule.getCurrentTask();

		LinkTimePair start;
		int nextTaskIdx;
		if (status == ScheduleStatus.STARTED) {
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

	public int getSize() {
		return entries.size();
	}

	public Collection<Entry> getEntries() {
		return Collections.unmodifiableCollection(entries.values());
	}
}