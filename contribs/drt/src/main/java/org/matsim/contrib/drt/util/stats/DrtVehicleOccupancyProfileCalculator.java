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
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;

/**
 * @author michalm (Michal Maciejewski)
 */
public class DrtVehicleOccupancyProfileCalculator
		implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleEntersTrafficEventHandler,
		VehicleLeavesTrafficEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {
	private final TimeDiscretizer timeDiscretizer;

	private final long[] idleVehicleProfileInSeconds;
	private final long[][] vehicleOccupancyProfilesInSeconds;

	private final double[] idleVehicleProfileRelative;
	private final double[][] vehicleOccupancyProfilesRelative;

	private final Map<Id<DvrpVehicle>, Double> stopBeginTimes = new IdMap<>(DvrpVehicle.class);
	private final Map<Id<DvrpVehicle>, Double> idleBeginTimes = new IdMap<>(DvrpVehicle.class);
	private final Map<Id<DvrpVehicle>, Double> driveBeginTimes = new IdMap<>(DvrpVehicle.class);

	private final Map<Id<DvrpVehicle>, Integer> vehicleOccupancies = new IdMap<>(DvrpVehicle.class);
	private final Map<Id<DvrpVehicle>, Integer> stopOccupancies = new IdMap<>(DvrpVehicle.class);

	private final double analysisEndTime;

	public DrtVehicleOccupancyProfileCalculator(FleetSpecification fleet, EventsManager events, int timeInterval,
			QSimConfigGroup qsimConfig) {
		events.addHandler(this);

		Max maxCapacity = new Max();
		Max maxServiceTime = new Max();
		for (DvrpVehicleSpecification v : fleet.getVehicleSpecifications().values()) {
			maxCapacity.increment(v.getCapacity());
			maxServiceTime.increment(v.getServiceEndTime());
		}

		if (qsimConfig.getSimEndtimeInterpretation() == EndtimeInterpretation.onlyUseEndtime
				&& qsimConfig.getEndTime().isDefined()) {
			analysisEndTime = qsimConfig.getEndTime().seconds();
		} else {
			analysisEndTime = maxServiceTime.getResult();
		}

		int intervalCount = (int) Math.ceil((analysisEndTime + 1) / timeInterval);
		timeDiscretizer = new TimeDiscretizer(intervalCount * timeInterval, timeInterval, TimeDiscretizer.Type.ACYCLIC);

		int occupancyProfilesCount = (int) maxCapacity.getResult() + 1;
		vehicleOccupancyProfilesInSeconds = new long[occupancyProfilesCount][timeDiscretizer.getIntervalCount()];
		idleVehicleProfileInSeconds = new long[timeDiscretizer.getIntervalCount()];

		vehicleOccupancyProfilesRelative = new double[occupancyProfilesCount][timeDiscretizer.getIntervalCount()];
		idleVehicleProfileRelative = new double[timeDiscretizer.getIntervalCount()];

		fleet.getVehicleSpecifications().keySet().forEach(id -> vehicleOccupancies.put(id, 0));
	}

	public void consolidate() {
		for (double beginTime : idleBeginTimes.values()) {
			increment(idleVehicleProfileInSeconds, beginTime, analysisEndTime);
		}

		for (Map.Entry<Id<DvrpVehicle>, Double> entry : stopBeginTimes.entrySet()) {
			int occupancy = stopOccupancies.get(entry.getKey());
			increment(vehicleOccupancyProfilesInSeconds[occupancy], entry.getValue(), analysisEndTime);
		}

		for (Map.Entry<Id<DvrpVehicle>, Double> entry : driveBeginTimes.entrySet()) {
			int occupancy = vehicleOccupancies.get(entry.getKey());
			increment(vehicleOccupancyProfilesInSeconds[occupancy], entry.getValue(), analysisEndTime);
		}

		idleBeginTimes.clear();
		stopBeginTimes.clear();
		driveBeginTimes.clear();

		for (int t = 0; t < timeDiscretizer.getIntervalCount(); t++) {
			idleVehicleProfileRelative[t] = (double) idleVehicleProfileInSeconds[t] / timeDiscretizer.getTimeInterval();
			for (int o = 0; o < vehicleOccupancyProfilesInSeconds.length; o++) {
				vehicleOccupancyProfilesRelative[o][t] = (double) vehicleOccupancyProfilesInSeconds[o][t]
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

	private void increment(long[] values, double beginTime, double endTime) {
		int timeInterval = timeDiscretizer.getTimeInterval();
		int fromIdx = timeDiscretizer.getIdx(Math.min(beginTime, analysisEndTime));
		int toIdx = timeDiscretizer.getIdx(Math.min(endTime, analysisEndTime));

		for (int i = fromIdx; i < toIdx; i++) {
			values[i] += timeInterval;
		}

		// reduce first time bin
		values[fromIdx] -= (int) beginTime % timeInterval;

		// handle last time bin
		values[toIdx] += (int) endTime % timeInterval;
	}

	/* Event handling starts here */

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)) {
			Id<DvrpVehicle> vehicleId = Id.create(event.getPersonId(), DvrpVehicle.class);

			if (vehicleOccupancies.containsKey(vehicleId)) {
				idleBeginTimes.put(vehicleId, event.getTime());
			}
		} else if (event.getActType().equals(DrtActionCreator.DRT_STOP_NAME)) {
			Id<DvrpVehicle> vehicleId = Id.create(event.getPersonId(), DvrpVehicle.class);

			if (vehicleOccupancies.containsKey(vehicleId)) {
				stopBeginTimes.put(vehicleId, event.getTime());
				stopOccupancies.put(vehicleId, vehicleOccupancies.get(vehicleId));
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)) {
			Id<DvrpVehicle> vehicleId = Id.create(event.getPersonId(), DvrpVehicle.class);

			if (vehicleOccupancies.containsKey(vehicleId)) {
				double beginTime = idleBeginTimes.remove(vehicleId);
				increment(idleVehicleProfileInSeconds, beginTime, event.getTime());
			}
		} else if (event.getActType().equals(DrtActionCreator.DRT_STOP_NAME)) {
			Id<DvrpVehicle> vehicleId = Id.create(event.getPersonId(), DvrpVehicle.class);

			if (vehicleOccupancies.containsKey(vehicleId)) {
				double beginTime = stopBeginTimes.remove(vehicleId);
				int occupancy = stopOccupancies.remove(vehicleId);

				increment(vehicleOccupancyProfilesInSeconds[occupancy], beginTime, event.getTime());
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		Id<DvrpVehicle> vehicleId = Id.create(event.getVehicleId(), DvrpVehicle.class);

		if (vehicleOccupancies.containsKey(vehicleId)) {
			Id<DvrpVehicle> personId = Id.create(event.getPersonId(), DvrpVehicle.class);

			if (!vehicleOccupancies.containsKey(personId)) { // We need to ignore the driver!
				vehicleOccupancies.put(vehicleId, vehicleOccupancies.get(vehicleId) + 1);
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		Id<DvrpVehicle> vehicleId = Id.create(event.getVehicleId(), DvrpVehicle.class);

		if (vehicleOccupancies.containsKey(vehicleId)) {
			Id<DvrpVehicle> personId = Id.create(event.getPersonId(), DvrpVehicle.class);

			if (!vehicleOccupancies.containsKey(personId)) { // We need to ignore the driver!
				vehicleOccupancies.put(vehicleId, vehicleOccupancies.get(vehicleId) - 1);

				if (stopOccupancies.containsKey(vehicleId)) {
					stopOccupancies.put(vehicleId, stopOccupancies.get(vehicleId) - 1);
				}
			}
		}
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		Id<DvrpVehicle> vehicleId = Id.create(event.getVehicleId(), DvrpVehicle.class);

		if (vehicleOccupancies.containsKey(vehicleId)) {
			driveBeginTimes.put(vehicleId, event.getTime());
		}
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		Id<DvrpVehicle> vehicleId = Id.create(event.getVehicleId(), DvrpVehicle.class);

		if (vehicleOccupancies.containsKey(vehicleId)) {
			double beginTime = driveBeginTimes.remove(vehicleId);
			int occupancy = vehicleOccupancies.get(vehicleId);

			increment(vehicleOccupancyProfilesInSeconds[occupancy], beginTime, event.getTime());
		}
	}

	@Override
	public void reset(int iteration) {
		idleBeginTimes.clear();
		stopBeginTimes.clear();
		driveBeginTimes.clear();

		vehicleOccupancies.replaceAll((k, v) -> 0);
		stopOccupancies.replaceAll((k, v) -> 0);

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
