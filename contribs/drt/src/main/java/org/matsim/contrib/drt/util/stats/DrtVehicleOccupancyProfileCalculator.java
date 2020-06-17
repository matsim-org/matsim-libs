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
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtVehicleOccupancyProfileCalculator
		implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ActivityStartEventHandler,
		ActivityEndEventHandler {

	private static class VehicleState {
		private static VehicleState nonOperating(double beginTime, String nonOperatingStateType) {
			return new VehicleState(beginTime, Preconditions.checkNotNull(nonOperatingStateType), 0);
		}

		private static VehicleState operating(double beginTime, int occupancy) {
			Preconditions.checkArgument(occupancy >= 0);
			return new VehicleState(beginTime, null, occupancy);
		}

		private final double beginTime;
		private final String nonOperatingStateType;//non-operating vehicle
		private final int occupancy;//operating vehicle

		private VehicleState(double beginTime, String nonOperatingStateType, int occupancy) {
			this.beginTime = beginTime;
			this.nonOperatingStateType = nonOperatingStateType;
			this.occupancy = occupancy;
			//maybe we could relax it in some situations, but that would also require adapting the charts
			Preconditions.checkArgument(nonOperatingStateType == null || occupancy == 0,
					"Vehicles not serving passengers must not be occupied ");
		}
	}

	private final TimeDiscretizer timeDiscretizer;

	private final ImmutableMap<String, long[]> nonOperatingVehicleProfileInSeconds;
	private final long[][] vehicleOccupancyProfilesInSeconds;

	private final ImmutableMap<String, double[]> nonOperatingVehicleProfileNormalized;
	private final double[][] vehicleOccupancyProfilesNormalized;

	private final ImmutableSet<String> nonOperatingActivities;
	private final Map<Id<DvrpVehicle>, VehicleState> vehicleStates = new IdMap<>(DvrpVehicle.class);

	private final double analysisEndTime;

	private final FleetSpecification fleet;

	public DrtVehicleOccupancyProfileCalculator(FleetSpecification fleet, EventsManager events, int timeInterval,
			QSimConfigGroup qsimConfig, ImmutableSet<String> nonOperatingActivities) {
		this.fleet = fleet;
		this.nonOperatingActivities = nonOperatingActivities;

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
		nonOperatingVehicleProfileInSeconds = nonOperatingActivities.stream()
				.collect(ImmutableMap.toImmutableMap(v -> v, v -> new long[timeDiscretizer.getIntervalCount()]));

		//TODO create and directly return the normalized profiles in consolidate()
		vehicleOccupancyProfilesNormalized = new double[occupancyProfilesCount][timeDiscretizer.getIntervalCount()];
		nonOperatingVehicleProfileNormalized = nonOperatingActivities.stream()
				.collect(ImmutableMap.toImmutableMap(v -> v, v -> new double[timeDiscretizer.getIntervalCount()]));
	}

	public void consolidate() {
		vehicleStates.values().forEach(state -> increment(state, analysisEndTime));
		vehicleStates.clear();

		for (String activity : nonOperatingActivities) {
			computeNormalizedProfiles(nonOperatingVehicleProfileInSeconds.get(activity),
					nonOperatingVehicleProfileNormalized.get(activity));
		}

		for (int occupancy = 0; occupancy < vehicleOccupancyProfilesInSeconds.length; occupancy++) {
			computeNormalizedProfiles(vehicleOccupancyProfilesInSeconds[occupancy],
					vehicleOccupancyProfilesNormalized[occupancy]);
		}
	}

	private void computeNormalizedProfiles(long[] profileInSeconds, double[] normalizedProfile) {
		for (int t = 0; t < timeDiscretizer.getIntervalCount(); t++) {
			normalizedProfile[t] = (double)profileInSeconds[t] / timeDiscretizer.getTimeInterval();
		}
	}

	public Map<String, double[]> getIdleVehicleProfile() {
		return nonOperatingVehicleProfileNormalized;
	}

	public double[][] getVehicleOccupancyProfiles() {
		return vehicleOccupancyProfilesNormalized;
	}

	public TimeDiscretizer getTimeDiscretizer() {
		return timeDiscretizer;
	}

	private void increment(VehicleState state, double endTime) {
		long[] profile = state.nonOperatingStateType != null ?
				nonOperatingVehicleProfileInSeconds.get(state.nonOperatingStateType) :
				vehicleOccupancyProfilesInSeconds[state.occupancy];
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
	public void handleEvent(ActivityStartEvent event) {
		if (nonOperatingActivities.contains(event.getActType())) {
			vehicleStates.computeIfPresent(vehicleId(event.getPersonId()), (id, oldState) -> {
				Verify.verify(oldState.nonOperatingStateType == null && oldState.occupancy == 0);
				increment(oldState, event.getTime());
				return VehicleState.nonOperating(event.getTime(), event.getActType());
			});
		} else if (event.getActType().equals(VrpAgentLogic.AFTER_SCHEDULE_ACTIVITY_TYPE)) {
			vehicleStates.computeIfPresent(vehicleId(event.getPersonId()), (id, oldState) -> {
				Verify.verify(oldState.nonOperatingStateType == null && oldState.occupancy == 0);
				increment(oldState, event.getTime());
				return null;
			});
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			if (fleet.getVehicleSpecifications().containsKey(vehicleId(event.getPersonId()))) {
				vehicleStates.put(vehicleId(event.getPersonId()), VehicleState.operating(event.getTime(), 0));
			}
		} else if (nonOperatingActivities.contains(event.getActType())) {
			vehicleStates.computeIfPresent(vehicleId(event.getPersonId()), (id, oldState) -> {
				Verify.verify(oldState.nonOperatingStateType.equals(event.getActType()));
				increment(oldState, event.getTime());
				return VehicleState.operating(event.getTime(), 0);
			});
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		vehicleStates.computeIfPresent(vehicleId(event.getVehicleId()), (id, oldState) -> {
			if (isDriver(event.getPersonId(), id)) {
				return oldState;//ignore the driver, no state change
			}
			Verify.verify(oldState.nonOperatingStateType == null);
			increment(oldState, event.getTime());
			return VehicleState.operating(event.getTime(), oldState.occupancy + 1);
		});
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		vehicleStates.computeIfPresent(vehicleId(event.getVehicleId()), (id, oldState) -> {
			if (isDriver(event.getPersonId(), id)) {
				return oldState;//ignore the driver, no state change
			}
			Verify.verify(oldState.nonOperatingStateType == null);
			increment(oldState, event.getTime());
			return VehicleState.operating(event.getTime(), oldState.occupancy - 1);
		});
	}

	private Id<DvrpVehicle> vehicleId(Id<?> id) {
		return Id.create(id, DvrpVehicle.class);
	}

	private boolean isDriver(Id<Person> personId, Id<DvrpVehicle> vehicleId) {
		return personId.equals(vehicleId);
	}

	@Override
	public void reset(int iteration) {
		vehicleStates.clear();

		nonOperatingVehicleProfileInSeconds.values().forEach(profile -> Arrays.fill(profile, 0));
		nonOperatingVehicleProfileNormalized.values().forEach(profile -> Arrays.fill(profile, 0));

		for (int k = 0; k < vehicleOccupancyProfilesInSeconds.length; k++) {
			Arrays.fill(vehicleOccupancyProfilesInSeconds[k], 0);
			Arrays.fill(vehicleOccupancyProfilesNormalized[k], 0);
		}
	}
}
