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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;
import org.matsim.utils.objectattributes.ObjectAttributes;


/**
 * @author dgrether
 * @author jwjoubert
 */
class VehiclesImpl implements Vehicles {
	
	private Map<Id, VehicleType> vehicleTypes;
	private LinkedHashMap<Id, Vehicle> vehicles;
	private VehiclesFactoryImpl builder;
	private final ObjectAttributes vehicleAttributes = new ObjectAttributes();
	
	private Counter counter = new Counter(" vehicles # ");

	/**
	 * deliberately non-public since there is a factory.  kai, nov'11
	 */
	VehiclesImpl(){
		this.vehicleTypes = new LinkedHashMap<Id, VehicleType>();
		this.builder = new VehiclesFactoryImpl() ;
		this.vehicles = new LinkedHashMap<Id, Vehicle>();
	}
	
	
	@Override
	public VehiclesFactory getFactory() {
		return this.builder;
	}

	@Override
	public final Map<Id, Vehicle> getVehicles() {
		// XXX should be immutable, but requires refactoring of contribs
		// and playgrounds before
		//return Collections.unmodifiableMap(this.vehicles);
		return this.vehicles;
	}


	@Override
	public Map<Id, VehicleType> getVehicleTypes() {
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
		
		/* Add the vehicle. */
		this.vehicles.put(v.getId(), v);
		this.counter.incCounter();
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
	public ObjectAttributes getVehicleAttributes() {
		return this.vehicleAttributes;
	}
	
}
