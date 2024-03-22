/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.fleet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * Contains all DvrpVehicles generated for a given iteration. Its lifespan is
 * limited to a single QSim simulation.
 * 
 * Fleet (and the contained DvrpVehicles) are created from FleetSpecification
 * (and the contained DvrpVehicleSpecifications).
 * 
 * Vehicles may be added and removed during the QSim simulation.
 *
 * @author michalm
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public final class Fleet {
	private final String mode;

	private final EventsManager eventsManager;
	private final MobsimTimer timer;

	private final DvrpVehicleLookup lookup;

	private final IdMap<DvrpVehicle, DvrpVehicle> vehicles = new IdMap<>(DvrpVehicle.class);

	public Fleet(String mode, EventsManager eventsManager, MobsimTimer timer, DvrpVehicleLookup lookup) {
		this.mode = mode;
		this.eventsManager = eventsManager;
		this.timer = timer;
		this.lookup = lookup;
	}

	/**
	 * Adds a vehicle to the fleet.
	 * 
	 * @return True if the vehicle was not already part of the fleet
	 */
	public boolean addVehicle(DvrpVehicle vehicle) {
		boolean added = vehicles.put(vehicle.getId(), vehicle) == null;

		if (added) {
			lookup.addVehicle(mode, vehicle);
			eventsManager.processEvent(new VehicleAddedEvent(timer.getTimeOfDay(), mode, vehicle.getId(), vehicle.getCapacity()));
		}

		return added;
	}

	/**
	 * Removes a vehicle from the fleet.
	 * 
	 * @return True if the vehicle existed in the fleet
	 */
	public boolean removeVehicle(Id<DvrpVehicle> vehicleId) {
		boolean removed = vehicles.remove(vehicleId) != null;

		if (removed) {
			lookup.removeVehicle(mode, vehicleId);
			eventsManager.processEvent(new VehicleRemovedEvent(timer.getTimeOfDay(), mode, vehicleId));
		}

		return removed;
	}

	/**
	 * Returns a map of vehicles for this fleet
	 */
	public IdMap<DvrpVehicle, DvrpVehicle> getVehicles() {
		return vehicles;
	}
}
