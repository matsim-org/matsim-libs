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

import java.util.Arrays;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtVehicleOccupancyProfileCalculator
		implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TaskStartedEventHandler,
		TaskEndedEventHandler {

	private static class VehicleState {
		private Task.TaskType taskType;
		private int occupancy;
		private double beginTime;
	}

	private final TimeDiscretizer timeDiscretizer;

	private final ImmutableMap<Task.TaskType, long[]> nonPassengerServingTaskProfilesInSeconds;
	private final long[][] vehicleOccupancyProfilesInSeconds;

	private final ImmutableMap<Task.TaskType, double[]> nonPassengerServingTaskProfiles;
	private final double[][] vehicleOccupancyProfiles;

	private final ImmutableSet<Task.TaskType> nonPassengerServingTaskTypes;
	private final Map<Id<DvrpVehicle>, VehicleState> vehicleStates = new IdMap<>(DvrpVehicle.class);

	private final double analysisEndTime;

	private final FleetSpecification fleet;

	public DrtVehicleOccupancyProfileCalculator(FleetSpecification fleet, EventsManager events, int timeInterval,
			QSimConfigGroup qsimConfig, ImmutableSet<Task.TaskType> nonPassengerServingTaskTypes) {
		this.fleet = fleet;
		this.nonPassengerServingTaskTypes = nonPassengerServingTaskTypes;

		events.addHandler(this);

		if (qsimConfig.getSimEndtimeInterpretation() == EndtimeInterpretation.onlyUseEndtime && qsimConfig.getEndTime()
				.isDefined()) {
			analysisEndTime = qsimConfig.getEndTime().seconds();
		} else {
			analysisEndTime = fleet.getVehicleSpecifications()
					.values()
					.stream()
					.mapToDouble(DvrpVehicleSpecification::getServiceEndTime)
					.max()
					.orElse(0);
		}

		int intervalCount = (int)Math.ceil((analysisEndTime + 1) / timeInterval);
		timeDiscretizer = new TimeDiscretizer(intervalCount * timeInterval, timeInterval, TimeDiscretizer.Type.ACYCLIC);

		int maxCapacity = fleet.getVehicleSpecifications()
				.values()
				.stream()
				.mapToInt(DvrpVehicleSpecification::getCapacity)
				.max()
				.orElse(0);
		int occupancyProfilesCount = maxCapacity + 1;
		vehicleOccupancyProfilesInSeconds = new long[occupancyProfilesCount][timeDiscretizer.getIntervalCount()];
		nonPassengerServingTaskProfilesInSeconds = nonPassengerServingTaskTypes.stream()
				.collect(ImmutableMap.toImmutableMap(v -> v, v -> new long[timeDiscretizer.getIntervalCount()]));

		//TODO create and directly return the normalized profiles in consolidate()
		vehicleOccupancyProfiles = new double[occupancyProfilesCount][timeDiscretizer.getIntervalCount()];
		nonPassengerServingTaskProfiles = nonPassengerServingTaskTypes.stream()
				.collect(ImmutableMap.toImmutableMap(v -> v, v -> new double[timeDiscretizer.getIntervalCount()]));
	}

	public void consolidate() {
		for (VehicleState state : vehicleStates.values()) {
			if (state.taskType != null) {
				increment(state, analysisEndTime);
			}
		}
		vehicleStates.clear();

		for (Task.TaskType taskType : nonPassengerServingTaskTypes) {
			computeNormalizedProfiles(nonPassengerServingTaskProfilesInSeconds.get(taskType),
					nonPassengerServingTaskProfiles.get(taskType));
		}

		for (int occupancy = 0; occupancy < vehicleOccupancyProfilesInSeconds.length; occupancy++) {
			computeNormalizedProfiles(vehicleOccupancyProfilesInSeconds[occupancy],
					vehicleOccupancyProfiles[occupancy]);
		}
	}

	private void computeNormalizedProfiles(long[] profileInSeconds, double[] normalizedProfile) {
		for (int t = 0; t < timeDiscretizer.getIntervalCount(); t++) {
			normalizedProfile[t] = (double)profileInSeconds[t] / timeDiscretizer.getTimeInterval();
		}
	}

	public Map<Task.TaskType, double[]> getNonPassengerServingTaskProfiles() {
		return nonPassengerServingTaskProfiles;
	}

	public double[][] getVehicleOccupancyProfiles() {
		return vehicleOccupancyProfiles;
	}

	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}

	private void increment(VehicleState state, double endTime) {
		Verify.verify(state.taskType != null);
		Verify.verify(state.occupancy >= 0);

		boolean servingPassengers = !nonPassengerServingTaskTypes.contains(state.taskType);
		Verify.verify(servingPassengers || state.occupancy == 0,
				"Vehicles not serving passengers must not be occupied");

		long[] profile = servingPassengers ?
				vehicleOccupancyProfilesInSeconds[state.occupancy] :
				nonPassengerServingTaskProfilesInSeconds.get(state.taskType);
		increment(profile, Math.min(state.beginTime, endTime), endTime);
	}

	private void increment(long[] values, double beginTime, double endTime) {
		if (beginTime == endTime) {
			return;
		}

		int timeInterval = timeDiscretizer.getTimeInterval();
		int fromIdx = timeDiscretizer.getIdx(beginTime);
		int toIdx = timeDiscretizer.getIdx(endTime);

		for (int i = fromIdx; i < toIdx; i++) {
			values[i] += timeInterval;
		}

		// reduce first time bin
		values[fromIdx] -= (int)beginTime % timeInterval;

		// handle last time bin
		values[toIdx] += (int)endTime % timeInterval;
	}

	/* Event handling starts here */

	@Override
	public void handleEvent(TaskStartedEvent event) {
		if (event.getTaskIndex() == 0) {
			if (fleet.getVehicleSpecifications().containsKey(event.getDvrpVehicleId())) {
				VehicleState state = new VehicleState();
				state.taskType = event.getTaskType();
				state.beginTime = event.getTime();
				vehicleStates.put(event.getDvrpVehicleId(), state);
			}
		} else {
			VehicleState state = vehicleStates.get(event.getDvrpVehicleId());
			if (state != null) {
				state.taskType = event.getTaskType();
				state.beginTime = event.getTime();
			}
		}
	}

	@Override
	public void handleEvent(TaskEndedEvent event) {
		VehicleState state = vehicleStates.get(event.getDvrpVehicleId());
		if (state != null) {
			increment(state, event.getTime());
			state.taskType = null;
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		processOccupancyChange(event, event.getVehicleId(), +1);
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		processOccupancyChange(event, event.getVehicleId(), -1);
	}

	private <E extends Event & HasPersonId> void processOccupancyChange(E event, Id<Vehicle> vehicleId, int change) {
		VehicleState state = vehicleStates.get(Id.create(vehicleId, DvrpVehicle.class));
		if (state != null && !isDriver(event.getPersonId(), vehicleId)) {
			increment(state, event.getTime());
			state.occupancy += change;
			state.beginTime = event.getTime();
		}
	}

	private boolean isDriver(Id<Person> personId, Id<Vehicle> vehicleId) {
		return personId.equals(vehicleId);
	}

	@Override
	public void reset(int iteration) {
		vehicleStates.clear();

		nonPassengerServingTaskProfilesInSeconds.values().forEach(profile -> Arrays.fill(profile, 0));
		nonPassengerServingTaskProfiles.values().forEach(profile -> Arrays.fill(profile, 0));

		for (int k = 0; k < vehicleOccupancyProfilesInSeconds.length; k++) {
			Arrays.fill(vehicleOccupancyProfilesInSeconds[k], 0);
			Arrays.fill(vehicleOccupancyProfiles[k], 0);
		}
	}
}
