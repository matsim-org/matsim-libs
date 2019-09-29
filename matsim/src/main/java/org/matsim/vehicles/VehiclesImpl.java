/* *********************************************************************** *
 * project: org.matsim.*
 * BasicVehiclesImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.core.utils.misc.Counter;

import java.util.Collections;
import java.util.Map;


/**
 * @author dgrether
 * @author jwjoubert
 */
final class VehiclesImpl implements Vehicles {
	private final Map<Id<VehicleType>, VehicleType> vehicleTypes;
	private final Map<Id<Vehicle>, Vehicle> vehicles;
	private final VehiclesFactoryImpl builder;

	private final Counter counter = new Counter("[VehiclesImpl] added vehicle # " );

	/**
	 * deliberately non-public since there is a factory.  kai, nov'11
	 */
	VehiclesImpl(){
		this.vehicleTypes = new IdMap<>(VehicleType.class); // FIXME potential iteration order change
		this.builder = new VehiclesFactoryImpl() ;
		this.vehicles = new IdMap<>(Vehicle.class); // FIXME potential iteration order change
	}


	@Override
	public VehiclesFactory getFactory() {
		return this.builder;
	}

	@Override
	public final Map<Id<Vehicle>, Vehicle> getVehicles() {
		return Collections.unmodifiableMap(this.vehicles);
	}


	@Override
	public Map<Id<VehicleType>, VehicleType> getVehicleTypes() {
		return Collections.unmodifiableMap(this.vehicleTypes);
	}

	/**
	 * Add the vehicle to the container.
	 *
	 * @param v
	 * @throws IllegalArgumentException if another {@link Vehicle} with
	 * the same {@link Id} already exists in the container.
	 */
	@Override
	public void addVehicle(final Vehicle v) {
		/* Validation. */
		if(this.getVehicles().containsKey(v.getId())){
			throw new IllegalArgumentException("Vehicle with id = " + v.getId() + " already exists.");
		}

		/* Check if the VehicleType associated with the vehicle already exist.
		 * Here only an error message is given. A RuntimeException is thrown
		 * when the MatsimVehicleWriter is called (JWJ, '14). */
		if(!this.vehicleTypes.containsKey(v.getType().getId())){
			throw new IllegalArgumentException("Cannot add Vehicle with type = " + v.getType().getId().toString() +
					" if the VehicleType has not been added to the Vehicles container.");
		}

		/* Add the vehicle. */
		this.vehicles.put(v.getId(), v);
		this.counter.incCounter();
	}

	/**
	 * Removes the vehicle with the given Id
	 *
	 * @param vehicleId
	 */
	@Override
	public void removeVehicle(final Id<Vehicle> vehicleId) {
		this.vehicles.remove(vehicleId);
	}

	/**
	 * Adds the vehicle type to the container.
	 *
	 * @param type
	 * @throws IllegalArgumentException if another {@link VehicleType} with the
	 * same {@link Id} already exists in the container.
	 */
	@Override
	public void addVehicleType(VehicleType type){
		/* Validation. */
		if(this.getVehicleTypes().containsKey(type.getId())){
			throw new IllegalArgumentException("Vehicle type with id = " + type.getId() + " already exists.");
		}

		/* Add the vehicle type. */
		this.vehicleTypes.put(type.getId(), type);
	}

	@Override
	public void removeVehicleType(Id<VehicleType> vehicleTypeId) {
		for (Vehicle veh : this.vehicles.values()) {
			if (veh.getType().getId().equals(vehicleTypeId)) {
				throw new IllegalArgumentException("Cannot remove vehicle type as it is used by at least one vehicle.");
			}
		}
		this.vehicleTypes.remove(vehicleTypeId);
	}
}
