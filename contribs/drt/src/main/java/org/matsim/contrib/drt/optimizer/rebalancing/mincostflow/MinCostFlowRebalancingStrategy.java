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

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task.TaskStatus;

/**
 * @author michalm
 */
public class MinCostFlowRebalancingStrategy implements RebalancingStrategy {
	public interface RebalancingTargetCalculator {
		int estimate(String zone, double time);
	}

	private static final double MAX_REMAINING_TIME_TO_IDLENESS = 1800;// for soon-idle vehicles
	private static final double MIN_REMAINING_SERVICE_TIME = 3600;// for idle vehicles

	private final RebalancingTargetCalculator rebalancingTargetCalculator;
	private final DrtZonalSystem zonalSystem;
	private final Fleet fleet;
	private final MinCostRelocationCalculator minCostRelocationCalculator;

	@Inject
	public MinCostFlowRebalancingStrategy(RebalancingTargetCalculator rebalancingTargetCalculator,
			DrtZonalSystem zonalSystem, Fleet fleet, MinCostRelocationCalculator minCostRelocationCalculator) {
		this.rebalancingTargetCalculator = rebalancingTargetCalculator;
		this.zonalSystem = zonalSystem;
		this.fleet = fleet;
		this.minCostRelocationCalculator = minCostRelocationCalculator;
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends Vehicle> rebalancableVehicles, double time) {
		Map<String, List<Vehicle>> rebalancableVehiclesPerZone = groupRebalancableVehicles(rebalancableVehicles, time);
		if (rebalancableVehiclesPerZone.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, List<Vehicle>> soonIdleVehiclesPerZone = groupSoonIdleVehicles(time);
		return calculateMinCostRelocations(time, rebalancableVehiclesPerZone, soonIdleVehiclesPerZone);
	}

	private Map<String, List<Vehicle>> groupRebalancableVehicles(Stream<? extends Vehicle> rebalancableVehicles,
			double time) {
		Map<String, List<Vehicle>> rebalancableVehiclesPerZone = new HashMap<>();
		rebalancableVehicles.filter(v -> v.getServiceEndTime() > time + MIN_REMAINING_SERVICE_TIME).forEach(v -> {
			Link link = ((StayTask)v.getSchedule().getCurrentTask()).getLink();
			String zone = zonalSystem.getZoneForLinkId(link.getId());
			if (zone != null) {
				// zonePerVehicle.put(v.getId(), zone);
				rebalancableVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v);
			}
		});
		return rebalancableVehiclesPerZone;
	}

	// also include vehicles being right now relocated or recharged
	private Map<String, List<Vehicle>> groupSoonIdleVehicles(double time) {
		Map<String, List<Vehicle>> soonIdleVehiclesPerZone = new HashMap<>();
		for (Vehicle v : fleet.getVehicles().values()) {
			Schedule s = v.getSchedule();
			StayTask stayTask = (StayTask)Schedules.getLastTask(s);
			if (stayTask.getStatus() == TaskStatus.PLANNED
					&& stayTask.getBeginTime() < time + MAX_REMAINING_TIME_TO_IDLENESS
					&& stayTask.getBeginTime() < time + MIN_REMAINING_SERVICE_TIME) {// XXX a separate constant???
				String zone = zonalSystem.getZoneForLinkId(stayTask.getLink().getId());
				if (zone != null) {
					soonIdleVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v);
				}
			}
		}
		return soonIdleVehiclesPerZone;
	}

	private List<Relocation> calculateMinCostRelocations(double time,
			Map<String, List<Vehicle>> rebalancableVehiclesPerZone,
			Map<String, List<Vehicle>> soonIdleVehiclesPerZone) {
		List<Pair<String, Integer>> supply = new ArrayList<>();
		List<Pair<String, Integer>> demand = new ArrayList<>();

		for (String z : zonalSystem.getZones().keySet()) {
			int rebalancable = rebalancableVehiclesPerZone.getOrDefault(z, Collections.emptyList()).size();
			int soonIdle = soonIdleVehiclesPerZone.getOrDefault(z, Collections.emptyList()).size();
			int target = rebalancingTargetCalculator.estimate(z, time);

			int delta = Math.min(rebalancable + soonIdle - target, rebalancable);
			if (delta < 0) {
				demand.add(Pair.of(z, -delta));
			} else if (delta > 0) {
				supply.add(Pair.of(z, delta));
			}
		}

		return minCostRelocationCalculator.calcRelocations(supply, demand, rebalancableVehiclesPerZone);
	}
}
