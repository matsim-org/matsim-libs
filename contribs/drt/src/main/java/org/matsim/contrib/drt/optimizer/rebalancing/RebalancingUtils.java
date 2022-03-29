/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.rebalancing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RebalancingUtils {
	public static Map<DrtZone, List<DvrpVehicle>> groupRebalancableVehicles(DrtZonalSystem zonalSystem,
			RebalancingParams params, Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone = new HashMap<>();
		rebalancableVehicles.filter(v -> v.getServiceEndTime() > time + params.getMinServiceTime()).forEach(v -> {
			Link link = ((StayTask)v.getSchedule().getCurrentTask()).getLink();
			DrtZone zone = zonalSystem.getZoneForLinkId(link.getId());
			if (zone == null) {
				zone = DrtZone.createDummyZone("single-vehicle-zone-" + v.getId(), List.of(link),
						link.getToNode().getCoord());
			}
			rebalancableVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v);
		});
		return rebalancableVehiclesPerZone;
	}

	// also include vehicles being right now relocated or recharged
	public static Map<DrtZone, List<DvrpVehicle>> groupSoonIdleVehicles(DrtZonalSystem zonalSystem,
			RebalancingParams params, Fleet fleet, double time) {
		Map<DrtZone, List<DvrpVehicle>> soonIdleVehiclesPerZone = new HashMap<>();
		for (DvrpVehicle v : fleet.getVehicles().values()) {
			Schedule s = v.getSchedule();
			StayTask stayTask = (StayTask)Schedules.getLastTask(s);
			if (stayTask.getStatus() == Task.TaskStatus.PLANNED
					&& stayTask.getBeginTime() < time + params.getMaxTimeBeforeIdle()
					&& v.getServiceEndTime() > time + params.getMinServiceTime()) {
				DrtZone zone = zonalSystem.getZoneForLinkId(stayTask.getLink().getId());
				if (zone != null) {
					soonIdleVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v);
				}
			}
		}
		return soonIdleVehiclesPerZone;
	}
}
