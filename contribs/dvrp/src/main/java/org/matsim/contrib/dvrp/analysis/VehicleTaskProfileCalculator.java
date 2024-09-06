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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.common.timeprofile.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
import org.matsim.core.config.groups.QSimConfigGroup;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

/**
 * Collects task profiles of DVRP vehicles. Based on {@link VehicleOccupancyProfileCalculator} but only collects tasks.
 *
 * @author nkuehnel / MOIA
 */
public class VehicleTaskProfileCalculator implements TaskStartedEventHandler,
		TaskEndedEventHandler {

	private static class VehicleState {
		private Task.TaskType taskType;
		private double beginTime;
	}

	private final TimeDiscretizer timeDiscretizer;

	private Map<Task.TaskType, double[]> taskProfiles;

	private final Map<Id<DvrpVehicle>, VehicleState> vehicleStates = new IdMap<>(DvrpVehicle.class);

	private final double analysisEndTime;

	private final String dvrpMode;

	private boolean wasConsolidatedInThisIteration = false;

	public VehicleTaskProfileCalculator(String dvrpMode, FleetSpecification fleet, int timeInterval,
											 QSimConfigGroup qsimConfig) {
		this.dvrpMode = dvrpMode;

		double qsimEndTime = qsimConfig.getEndTime().orElse(0);
		double maxServiceEndTime = fleet.getVehicleSpecifications()
				.values()
				.stream()
				.mapToDouble(DvrpVehicleSpecification::getServiceEndTime)
				.max()
				.orElse(0);
		analysisEndTime = Math.max(qsimEndTime, maxServiceEndTime);
		timeDiscretizer = new TimeDiscretizer((int)Math.ceil(analysisEndTime), timeInterval);
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

			taskProfiles.values().forEach(this::normalizeProfile);
			wasConsolidatedInThisIteration = true;
		}
	}

	private void normalizeProfile(double[] profile) {
		for (int i = 0; i < profile.length; i++) {
			profile[i] /= timeDiscretizer.getTimeInterval();
		}
	}

	public Map<Task.TaskType, double[]> getTaskProfiles() {
		this.consolidate();
		return taskProfiles;
	}

	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}

	private void increment(VehicleState state, double endTime) {
		Verify.verify(state.taskType != null);

		double[] profile = taskProfiles.computeIfAbsent(state.taskType,
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
			state = new VehicleState();
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
	public void reset(int iteration) {
		vehicleStates.clear();

		taskProfiles = new HashMap<>();
		wasConsolidatedInThisIteration = false;
	}
}
