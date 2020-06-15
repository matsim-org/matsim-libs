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

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.rank.Max;
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
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;

import com.google.common.base.Preconditions;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtVehicleOccupancyProfileCalculator
		implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ActivityStartEventHandler,
		ActivityEndEventHandler {

	private static class VehicleState {
		private final double beginTime;
		private final boolean idle;
		private final int occupancy;

		private VehicleState(double beginTime, boolean idle, int occupancy) {
			this.beginTime = beginTime;
			this.idle = idle;
			this.occupancy = occupancy;
			//maybe we could relax it in some situations, but that would also require adapting the charts
			Preconditions.checkArgument(!idle || occupancy == 0, "Idle vehicles must not be occupied ");
		}
	}

	private final TimeDiscretizer timeDiscretizer;

	private final long[] idleVehicleProfileInSeconds;
	private final long[][] vehicleOccupancyProfilesInSeconds;

	private final double[] idleVehicleProfileRelative;
	private final double[][] vehicleOccupancyProfilesRelative;

	private final Map<Id<DvrpVehicle>, VehicleState> vehicleStates = new IdMap<>(DvrpVehicle.class);

	private final double analysisEndTime;

	private final FleetSpecification fleet;

	public DrtVehicleOccupancyProfileCalculator(FleetSpecification fleet, EventsManager events, int timeInterval,
			QSimConfigGroup qsimConfig) {
		this.fleet = fleet;

		events.addHandler(this);

		Max maxCapacity = new Max();
		Max maxServiceTime = new Max();
		for (DvrpVehicleSpecification v : fleet.getVehicleSpecifications().values()) {
			maxCapacity.increment(v.getCapacity());
			maxServiceTime.increment(v.getServiceEndTime());
		}

		if (qsimConfig.getSimEndtimeInterpretation() == EndtimeInterpretation.onlyUseEndtime && qsimConfig.getEndTime()
				.isDefined()) {
			analysisEndTime = qsimConfig.getEndTime().seconds();
		} else {
			analysisEndTime = maxServiceTime.getResult();
		}

		int intervalCount = (int)Math.ceil((analysisEndTime + 1) / timeInterval);
		timeDiscretizer = new TimeDiscretizer(intervalCount * timeInterval, timeInterval, TimeDiscretizer.Type.ACYCLIC);

		int occupancyProfilesCount = (int)maxCapacity.getResult() + 1;
		vehicleOccupancyProfilesInSeconds = new long[occupancyProfilesCount][timeDiscretizer.getIntervalCount()];
		idleVehicleProfileInSeconds = new long[timeDiscretizer.getIntervalCount()];

		vehicleOccupancyProfilesRelative = new double[occupancyProfilesCount][timeDiscretizer.getIntervalCount()];
		idleVehicleProfileRelative = new double[timeDiscretizer.getIntervalCount()];
	}

	public void consolidate() {
		vehicleStates.values().forEach(state -> increment(state, analysisEndTime));
		vehicleStates.clear();

		for (int t = 0; t < timeDiscretizer.getIntervalCount(); t++) {
			idleVehicleProfileRelative[t] = (double)idleVehicleProfileInSeconds[t] / timeDiscretizer.getTimeInterval();
			for (int o = 0; o < vehicleOccupancyProfilesInSeconds.length; o++) {
				vehicleOccupancyProfilesRelative[o][t] = (double)vehicleOccupancyProfilesInSeconds[o][t]
						/ timeDiscretizer.getTimeInterval();
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

	private void increment(VehicleState state, double endTime) {
		long[] profile = state.idle ? idleVehicleProfileInSeconds : vehicleOccupancyProfilesInSeconds[state.occupancy];
		increment(profile, Math.min(state.beginTime, endTime), endTime);
	}

	private void increment(long[] values, double beginTime, double endTime) {
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
		if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)) {
			vehicleStates.computeIfPresent(vehicleId(event.getPersonId()), (id, oldState) -> {
				increment(oldState, event.getTime());
				return new VehicleState(event.getTime(), true, oldState.occupancy);
			});
		} else if (event.getActType().equals(VrpAgentLogic.AFTER_SCHEDULE_ACTIVITY_TYPE)) {
			vehicleStates.computeIfPresent(vehicleId(event.getPersonId()), (id, oldState) -> {
				increment(oldState, event.getTime());
				return null;
			});
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(VrpAgentLogic.BEFORE_SCHEDULE_ACTIVITY_TYPE)) {
			if (fleet.getVehicleSpecifications().containsKey(vehicleId(event.getPersonId()))) {
				vehicleStates.put(vehicleId(event.getPersonId()), new VehicleState(event.getTime(), false, 0));
			}
		} else if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)) {
			vehicleStates.computeIfPresent(vehicleId(event.getPersonId()), (id, oldState) -> {
				increment(oldState, event.getTime());
				return new VehicleState(event.getTime(), false, oldState.occupancy);
			});
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		vehicleStates.computeIfPresent(vehicleId(event.getVehicleId()), (id, oldState) -> {
			if (isDriver(event.getPersonId(), id)) {
				return oldState;//ignore the driver, no state change
			}
			increment(oldState, event.getTime());
			return new VehicleState(event.getTime(), oldState.idle, oldState.occupancy + 1);
		});
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		vehicleStates.computeIfPresent(vehicleId(event.getVehicleId()), (id, oldState) -> {
			if (isDriver(event.getPersonId(), id)) {
				return oldState;//ignore the driver, no state change
			}
			increment(oldState, event.getTime());
			return new VehicleState(event.getTime(), oldState.idle, oldState.occupancy - 1);
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

		for (int i = 0; i < idleVehicleProfileInSeconds.length; i++) {
			idleVehicleProfileInSeconds[i] = 0;
			idleVehicleProfileRelative[i] = 0;
		}

		for (int k = 0; k < vehicleOccupancyProfilesInSeconds.length; k++) {
			for (int i = 0; i < vehicleOccupancyProfilesInSeconds[k].length; i++) {
				vehicleOccupancyProfilesInSeconds[k][i] = 0;
				vehicleOccupancyProfilesRelative[k][i] = 0;
			}
		}
	}
}
