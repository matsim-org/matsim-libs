/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.michalm.taxi.optimizer.assignment;

import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.taxi.optimizer.VehicleData;
import org.matsim.contrib.taxi.optimizer.VehicleData.Entry;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentDestinationData.DestEntry;
import org.matsim.contrib.taxi.optimizer.assignment.VehicleAssignmentProblem.AssignmentCost;

import playground.michalm.taxi.optimizer.assignment.AssignmentChargerPlugData.ChargerPlug;

public class ETaxiToPlugAssignmentCostProvider {
	public enum Mode {
		// DRIVE_TIME, //not so useful

		ARRIVAL_TIME, // not so useful

		// PLUG_IDLE_TIME,

		CHARGING_START_TIME;

		// both:
		// good during when we do not have so many plugs, we do want to increase charging throughput,
		// so we want vehicles to come as soon as possible
		// good during vehicle undersupply; increases request serving throughput

		// [in a deterministic setting, with fixed (time-invariant) travel times, etc.]
		// CHARGING_START_TIME = max(plug_ready_time, ARRIVAL_TIME)

		// XXX when no dummy vehs/plugs, we can transform linearly between:
		// (a) ARRIVAL_TIME and DRIVE_TIME
		// (b) PLUG_IDLE_TIME and CHARGING_START_TIME
	}

	private final AssignmentETaxiOptimizerParams params;

	public ETaxiToPlugAssignmentCostProvider(AssignmentETaxiOptimizerParams params) {
		this.params = params;
	}

	public AssignmentCost<ChargerPlug> getCost(AssignmentChargerPlugData pData, VehicleData vData) {
		final Mode currentMode = Mode.CHARGING_START_TIME;
		return new AssignmentCost<ChargerPlug>() {
			public double calc(Entry departure, DestEntry<ChargerPlug> plugEntry, PathData pathData) {
				double arrivalTime = calcArrivalTime(departure, pathData);
				switch (currentMode) {
					case ARRIVAL_TIME:
						return arrivalTime;

					case CHARGING_START_TIME:
						return Math.max(plugEntry.time, arrivalTime);

					default:
						throw new IllegalStateException();
				}
			}
		};
	}

	private double calcArrivalTime(VehicleData.Entry departure, PathData pathData) {
		double travelTime = pathData == null ? //
				params.nullPathCost : // no path (too far away)
				pathData.firstAndLastLinkTT + pathData.path.travelTime;
		return departure.time + travelTime;
	}
}
