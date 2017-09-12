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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.schedule.ScheduleInquiry;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.taxi.schedule.TaxiStayTask;
import org.matsim.contrib.zone.ZonalSystem;
import org.matsim.contrib.zone.ZonalSystems;
import org.matsim.contrib.zone.Zone;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

public class IdleTaxiZonalRegistry {
	private final ScheduleInquiry scheduleInquiry;

	private final ZonalSystem zonalSystem;
	private final Map<Id<Zone>, List<Zone>> zonesSortedByDistance;

	private final Map<Id<Zone>, Map<Id<Vehicle>, Vehicle>> vehiclesInZones;
	private final Map<Id<Vehicle>, Vehicle> vehicles = new LinkedHashMap<>();

	public IdleTaxiZonalRegistry(ZonalSystem zonalSystem, ScheduleInquiry scheduleInquiry) {
		this.scheduleInquiry = scheduleInquiry;

		this.zonalSystem = zonalSystem;
		zonesSortedByDistance = ZonalSystems.initZonesByDistance(zonalSystem.getZones());

		vehiclesInZones = Maps.newHashMapWithExpectedSize(zonalSystem.getZones().size());
		for (Id<Zone> id : zonalSystem.getZones().keySet()) {
			vehiclesInZones.put(id, new HashMap<Id<Vehicle>, Vehicle>());
		}
	}

	public void addVehicle(Vehicle vehicle) {
		TaxiStayTask stayTask = (TaxiStayTask)vehicle.getSchedule().getCurrentTask();
		Id<Zone> zoneId = getZoneId(stayTask);

		if (vehiclesInZones.get(zoneId).put(vehicle.getId(), vehicle) != null) {
			throw new IllegalStateException(vehicle + " is already in the registry");
		}

		if (vehicles.put(vehicle.getId(), vehicle) != null) {
			throw new IllegalStateException(vehicle + " is already in the registry");
		}
	}

	public void removeVehicle(Vehicle vehicle) {
		TaxiStayTask stayTask = (TaxiStayTask)Schedules.getPreviousTask(vehicle.getSchedule());
		Id<Zone> zoneId = getZoneId(stayTask);

		if (vehiclesInZones.get(zoneId).remove(vehicle.getId()) == null) {
			throw new IllegalStateException(vehicle + " is not in the registry");
		}

		if (vehicles.remove(vehicle.getId()) == null) {
			throw new IllegalStateException(vehicle + " is not in the registry");
		}
	}

	public List<Vehicle> findNearestVehicles(Node node, int minCount) {
		return findNearestVehicles(node, minCount, null);
	}

	public List<Vehicle> findNearestVehicles(Node node, int minCount, Predicate<Vehicle> vehicleFilter) {
		if (minCount >= vehicles.size()) {
			return getVehicles();
		}

		Predicate<Vehicle> idleVehicleFilter = vehicleFilter == null ? scheduleInquiry::isIdle
				: Predicates.and(vehicleFilter, scheduleInquiry::isIdle);

		Zone zone = zonalSystem.getZone(node);
		Iterable<? extends Zone> zonesByDistance = zonesSortedByDistance.get(zone.getId());
		List<Vehicle> nearestVehs = new ArrayList<>();

		for (Zone z : zonesByDistance) {
			Iterables.addAll(nearestVehs, Iterables.filter(vehiclesInZones.get(z.getId()).values(), idleVehicleFilter));

			if (nearestVehs.size() >= minCount) {
				return nearestVehs;
			}
		}

		return nearestVehs;
	}

	private Id<Zone> getZoneId(TaxiStayTask stayTask) {
		return zonalSystem.getZone(stayTask.getLink().getToNode()).getId();
	}

	public List<Vehicle> getVehicles() {
		List<Vehicle> vehs = new ArrayList<>();
		Iterables.addAll(vehs, Iterables.filter(vehicles.values(), scheduleInquiry::isIdle));
		return vehs;
	}

	public int getVehicleCount() {
		return vehicles.size();
	}
}
