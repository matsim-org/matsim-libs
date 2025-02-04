/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */
package org.matsim.contrib.dvrp.analysis;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.vehicles.Vehicle;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * @author michalm (Michal Maciejewski)
 * 
 * Note: This class calculates occupancy in terms of *requests*. It does *not* take into
 * account the DvrpLoad of a request for the time being. All the standard DRT analysis tools
 * are kept backwards compatible at the time of introducing DvrpLoad. For a pricse occupancy
 * analysis using the actual loads, make use of the specific analysis tools (can be enabled
 * in the respective parameters set). /sh, jan 2025
 */


public class VehicleOccupancyProfileCalculator
		implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, TaskStartedEventHandler,
		TaskEndedEventHandler {

	private final static Logger logger = LogManager.getLogger(VehicleOccupancyProfileCalculator.class);

	private static class VehicleState {
		private Task.TaskType taskType;
		private int occupancy;
		private double beginTime;

		public VehicleState(Task.TaskType taskType, int occupancy, double beginTime) {
			this.taskType = taskType;
			this.occupancy = occupancy;
			this.beginTime = beginTime;
		}

		public VehicleState(int occupancy) {
			this(null, occupancy, 0.0);
		}
	}

	private final TimeDiscretizer timeDiscretizer;

	private Map<Task.TaskType, double[]> nonPassengerServingTaskProfiles;
	private ImmutableList<double[]> vehicleOccupancyProfiles;

	private final ImmutableSet<Task.TaskType> passengerServingTaskTypes;
	private final Map<Id<DvrpVehicle>, VehicleState> vehicleStates = new IdMap<>(DvrpVehicle.class);

	private final double analysisEndTime;
	private final int maxCapacity;

	private final String dvrpMode;

	private boolean wasConsolidatedInThisIteration = false;

	public VehicleOccupancyProfileCalculator(String dvrpMode, FleetSpecification fleet, int timeInterval,
			QSimConfigGroup qsimConfig, ImmutableSet<Task.TaskType> passengerServingTaskTypes, DvrpLoadType loadType) {
		this.dvrpMode = dvrpMode;
		this.passengerServingTaskTypes = passengerServingTaskTypes;

		double qsimEndTime = qsimConfig.getEndTime().orElse(0);
		double maxServiceEndTime = fleet.getVehicleSpecifications()
				.values()
				.stream()
				.mapToDouble(DvrpVehicleSpecification::getServiceEndTime)
				.max()
				.orElse(0);
		analysisEndTime = Math.max(qsimEndTime, maxServiceEndTime);
		timeDiscretizer = new TimeDiscretizer((int)Math.ceil(analysisEndTime), timeInterval);
		maxCapacity = findMaxVehicleCapacity(fleet, loadType);
	}

	/**
	 * @param fleet
	 * @return
	 */
	public static int findMaxVehicleCapacity(FleetSpecification fleet, DvrpLoadType loadType) {
		int maximumCapacity = 0;

		for (var spec : fleet.getVehicleSpecifications().values()) {
			int vehicleCapacity = 0;

			// for multi-dimensional capacities, we consider the sum
			for (int k = 0; k < loadType.size(); k++) {
				vehicleCapacity += (int) spec.getCapacity().getElement(k);
			}

			maximumCapacity = Math.max(maximumCapacity, vehicleCapacity);
		}
		
		return maximumCapacity;
	}

	private void consolidate() {
		Preconditions.checkState(!wasConsolidatedInThisIteration || vehicleStates.isEmpty(),
					"The profiles has been already consolidated, but the vehicles states are not empty."
							+ " This means consolidation was done too early (before all events has been processed)."
							+ " Hint: run consolidate() after Mobsim is completed (e.g. MobsimBeforeCleanupEvent is sent).");

		if (!wasConsolidatedInThisIteration) {
			// consolidate
			for (VehicleState state : vehicleStates.values()) {
				if (state.taskType != null) {
					increment(state, analysisEndTime);
				}
			}
			vehicleStates.clear();

			nonPassengerServingTaskProfiles.values().forEach(this::normalizeProfile);
			vehicleOccupancyProfiles.forEach(this::normalizeProfile);
			wasConsolidatedInThisIteration = true;
		}
	}

	private void normalizeProfile(double[] profile) {
		for (int i = 0; i < profile.length; i++) {
			profile[i] /= timeDiscretizer.getTimeInterval();
		}
	}

	public Map<Task.TaskType, double[]> getNonPassengerServingTaskProfiles() {
		this.consolidate();
		return nonPassengerServingTaskProfiles;
	}

	public List<double[]> getVehicleOccupancyProfiles() {
		this.consolidate();
		return vehicleOccupancyProfiles;
	}

	public double[] getNumberOfVehiclesInServiceProfile() {
		double[] numberOfVehiclesInServiceProfile = new double[timeDiscretizer.getIntervalCount()];
		Map<Task.TaskType, double[]> nonPassengerServingTaskProfiles = getNonPassengerServingTaskProfiles();
		List<double[]> vehicleOccupancyProfiles = getVehicleOccupancyProfiles();
		for (int i = 0; i < timeDiscretizer.getIntervalCount(); i++) {
			double total = 0.0;
			for (double[] profile : nonPassengerServingTaskProfiles.values()) {
				total += profile[i];
			}
			for (double[] profile : vehicleOccupancyProfiles) {
				total += profile[i];
			}
			numberOfVehiclesInServiceProfile[i] = total;
		}
		return numberOfVehiclesInServiceProfile;
	}

	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}

	private void increment(VehicleState state, double endTime) {
		Verify.verify(state.taskType != null);

		int currentVehicleOccupancy = state.occupancy;
		Verify.verify(currentVehicleOccupancy >= 0);
		if(currentVehicleOccupancy > maxCapacity) {
			logger.warn("Current limitation of VehicleOccupancyProfileCalculator does not allow to consider changing scalar capacities that are greater than the initial maximum capacity of the fleet");
			return;
		}
		boolean servingPassengers = passengerServingTaskTypes.contains(state.taskType) || currentVehicleOccupancy > 0;

		double[] profile = servingPassengers ?
				vehicleOccupancyProfiles.get(currentVehicleOccupancy) :
				nonPassengerServingTaskProfiles.computeIfAbsent(state.taskType,
						v -> new double[timeDiscretizer.getIntervalCount()]);
		increment(profile, Math.min(state.beginTime, endTime), endTime);
	}

	private void increment(double[] values, double beginTime, double endTime) {
		if (beginTime == endTime && beginTime >= analysisEndTime) {
			return;
		}
		endTime = Math.min(endTime, analysisEndTime);

		double timeInterval = timeDiscretizer.getTimeInterval();
		int fromIdx = timeDiscretizer.getIdx(beginTime);
		int toIdx = timeDiscretizer.getIdx(endTime);

		for (int i = fromIdx; i < toIdx; i++) {
			values[i] += timeInterval;
		}

		// reduce first time bin
		values[fromIdx] -= beginTime % timeInterval;

		// handle last time bin
		values[toIdx] += endTime % timeInterval;
	}

	/* Event handling starts here */

	@Override
	public void handleEvent(TaskStartedEvent event) {
		if (!event.getDvrpMode().equals(dvrpMode)) {
			return;
		}
		final VehicleState state;
		if (event.getTaskIndex() == 0) {
			state = new VehicleState(0);
			vehicleStates.put(event.getDvrpVehicleId(), state);
		} else {
			state = vehicleStates.get(event.getDvrpVehicleId());
		}
		state.taskType = event.getTaskType();
		state.beginTime = event.getTime();
	}

	@Override
	public void handleEvent(TaskEndedEvent event) {
		if (!event.getDvrpMode().equals(dvrpMode)) {
			return;
		}

		VehicleState state = vehicleStates.get(event.getDvrpVehicleId());
		increment(state, event.getTime());
		state.taskType = null;
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

		vehicleOccupancyProfiles = IntStream.rangeClosed(0, maxCapacity)
				.mapToObj(i -> new double[timeDiscretizer.getIntervalCount()])
				.collect(toImmutableList());

		nonPassengerServingTaskProfiles = new HashMap<>();
		wasConsolidatedInThisIteration = false;
	}
}
