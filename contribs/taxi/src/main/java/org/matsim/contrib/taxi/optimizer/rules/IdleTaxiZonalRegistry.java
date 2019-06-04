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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.contrib.zone.ZonalSystems;
import org.matsim.contrib.zone.Zone;

import com.google.common.collect.Maps;

public class IdleTaxiZonalRegistry {
	private final ScheduleInquiry scheduleInquiry;

	private final ZonalSystem zonalSystem;
	private final Map<Id<Zone>, List<Zone>> zonesSortedByDistance;

	private final Map<Id<Zone>, Map<Id<DvrpVehicle>, DvrpVehicle>> vehiclesInZones;
	private final Map<Id<DvrpVehicle>, DvrpVehicle> vehicles = new LinkedHashMap<>();

	public IdleTaxiZonalRegistry(ZonalSystem zonalSystem, ScheduleInquiry scheduleInquiry) {
		this.scheduleInquiry = scheduleInquiry;

		this.zonalSystem = zonalSystem;
		zonesSortedByDistance = ZonalSystems.initZonesByDistance(zonalSystem.getZones());

		vehiclesInZones = Maps.newHashMapWithExpectedSize(zonalSystem.getZones().size());
		for (Id<Zone> id : zonalSystem.getZones().keySet()) {
			vehiclesInZones.put(id, new HashMap<>());
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
		Predicate<DvrpVehicle> idleVehicleFilter = vehicleFilter == null ? scheduleInquiry::isIdle
				: vehicleFilter.and(scheduleInquiry::isIdle);

		return minCount >= vehicles.size() //
				? vehicles.values().stream().filter(idleVehicleFilter)
				: zonesSortedByDistance.get(zonalSystem.getZone(node).getId()).stream()//
						.flatMap(z -> vehiclesInZones.get(z.getId()).values().stream())//
						.filter(idleVehicleFilter)//
						.limit(minCount);
	}

	private Id<Zone> getZoneId(TaxiStayTask stayTask) {
		return zonalSystem.getZone(stayTask.getLink().getToNode()).getId();
	}

	public Stream<DvrpVehicle> vehicles() {
		return vehicles.values().stream().filter(scheduleInquiry::isIdle);
	}

	public int getVehicleCount() {
		return vehicles.size();
	}
}
