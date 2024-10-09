/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.taxi.optimizer.rules;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.common.zones.ZoneSystemUtils;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;

public class IdleTaxiZonalRegistry {
	private final ScheduleInquiry scheduleInquiry;

	private final ZoneSystem zoneSystem;
	private final IdMap<Zone, List<Zone>> zonesSortedByDistance;

	private final IdMap<Zone, Map<Id<DvrpVehicle>, DvrpVehicle>> vehiclesInZones = new IdMap<>(Zone.class);
	private final Map<Id<DvrpVehicle>, DvrpVehicle> vehicles = new LinkedHashMap<>();

	public IdleTaxiZonalRegistry(ZoneSystem zoneSystem, ScheduleInquiry scheduleInquiry) {
		this.scheduleInquiry = scheduleInquiry;

		this.zoneSystem = zoneSystem;
		zonesSortedByDistance = ZoneSystemUtils.initZonesByDistance(zoneSystem.getZones());

		for (Id<Zone> id : zoneSystem.getZones().keySet()) {
			vehiclesInZones.put(id, new LinkedHashMap<>());//LinkedHashMap to preserve iteration order
		}
	}

	public void addVehicle(DvrpVehicle vehicle) {
		TaxiStayTask stayTask = (TaxiStayTask)vehicle.getSchedule().getCurrentTask();
		Id<Zone> zoneId = getZoneId(stayTask);

		if (vehiclesInZones.get(zoneId).put(vehicle.getId(), vehicle) != null) {
			throw new IllegalStateException(vehicle + " is already in the registry");
		}

		if (vehicles.put(vehicle.getId(), vehicle) != null) {
			throw new IllegalStateException(vehicle + " is already in the registry");
		}
	}

	public void removeVehicle(DvrpVehicle vehicle) {
		TaxiStayTask stayTask = (TaxiStayTask)Schedules.getPreviousTask(vehicle.getSchedule());
		Id<Zone> zoneId = getZoneId(stayTask);

		if (vehiclesInZones.get(zoneId).remove(vehicle.getId()) == null) {
			throw new IllegalStateException(vehicle + " is not in the registry");
		}

		if (vehicles.remove(vehicle.getId()) == null) {
			throw new IllegalStateException(vehicle + " is not in the registry");
		}
	}

	public Stream<DvrpVehicle> findNearestVehicles(Node node, int minCount) {
		return findNearestVehicles(node, minCount, null);
	}

	public Stream<DvrpVehicle> findNearestVehicles(Node node, int minCount, Predicate<DvrpVehicle> vehicleFilter) {
		Predicate<DvrpVehicle> idleVehicleFilter = vehicleFilter == null ? scheduleInquiry::isIdle : vehicleFilter.and(scheduleInquiry::isIdle);

		return minCount >= vehicles.size() ?
				vehicles.values().stream().filter(idleVehicleFilter) :
				zonesSortedByDistance.get(zoneSystem.getZoneForNodeId(node.getId()).orElseThrow().getId())
						.stream()
						.flatMap(z -> vehiclesInZones.get(z.getId()).values().stream())
						.filter(idleVehicleFilter)
						.limit(minCount);
	}

	private Id<Zone> getZoneId(TaxiStayTask stayTask) {
		return zoneSystem.getZoneForLinkId(stayTask.getLink().getId()).orElseThrow().getId();
	}

	public Stream<DvrpVehicle> vehicles() {
		return vehicles.values().stream().filter(scheduleInquiry::isIdle);
	}

	public int getVehicleCount() {
		return vehicles.size();
	}
}
