/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.util.LinkProvider;

/**
 * @author michalm
 */
public class FleetImpl implements Fleet {
	public static Fleet create(FleetSpecification fleetSpecification, LinkProvider<Id<Link>> linkProvider) {
		FleetImpl fleet = new FleetImpl();
		fleetSpecification.getVehicleSpecifications()
				.values()
				.stream()
				.map(s -> VehicleImpl.createFromSpecification(s, linkProvider))
				.forEach(fleet::addVehicle);
		return fleet;
	}

	private final Map<Id<Vehicle>, Vehicle> vehicles = new LinkedHashMap<>();

	@Override
	public Map<Id<Vehicle>, ? extends Vehicle> getVehicles() {
		return Collections.unmodifiableMap(vehicles);
	}

	public void addVehicle(Vehicle vehicle) {
		vehicles.put(vehicle.getId(), vehicle);
	}

	public void resetSchedules() {
		for (Vehicle v : vehicles.values()) {
			v.resetSchedule();
		}
	}
}
