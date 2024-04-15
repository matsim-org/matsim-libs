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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RebalancingUtils {
	public static Map<Zone, List<DvrpVehicle>> groupRebalancableVehicles(ZoneSystem zoneSystem,
																		 RebalancingParams params, Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		Map<Zone, List<DvrpVehicle>> rebalancableVehiclesPerZone = new HashMap<>();
		rebalancableVehicles.filter(v -> v.getServiceEndTime() > time + params.minServiceTime).forEach(v -> {
			Link link = ((StayTask)v.getSchedule().getCurrentTask()).getLink();
			Zone zone = zoneSystem.getZoneForLinkId(link.getId())
				.orElse(ZoneImpl.createDummyZone(Id.create("single-vehicle-zone-" + v.getId(), Zone.class),
				link.getToNode().getCoord()));
			rebalancableVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v);
		});
		return rebalancableVehiclesPerZone;
	}

	// also include vehicles being right now relocated or recharged
	public static Map<Zone, List<DvrpVehicle>> groupSoonIdleVehicles(ZoneSystem zoneSystem,
			RebalancingParams params, Fleet fleet, double time) {
		Map<Zone, List<DvrpVehicle>> soonIdleVehiclesPerZone = new HashMap<>();
		for (DvrpVehicle v : fleet.getVehicles().values()) {
			Schedule s = v.getSchedule();
			StayTask stayTask = (StayTask)Schedules.getLastTask(s);
			if (stayTask.getStatus() == Task.TaskStatus.PLANNED
					&& stayTask.getBeginTime() < time + params.maxTimeBeforeIdle
					&& v.getServiceEndTime() > time + params.minServiceTime) {
				zoneSystem.getZoneForLinkId(stayTask.getLink().getId())
					.ifPresent(
						zone -> soonIdleVehiclesPerZone.computeIfAbsent(zone, z -> new ArrayList<>()).add(v)
					);
			}
		}
		return soonIdleVehiclesPerZone;
	}
}
