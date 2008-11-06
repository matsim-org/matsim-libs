/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.basic.v01;

import java.util.List;
import java.util.Map;

import org.matsim.basic.v01.BasicEngineInformation.FuelType;
import org.matsim.basic.v01.BasicVehicleCapacity.BasicFreightCapacity;


/**
 * @author dgrether
 *
 */
public class BasicVehicleBuilder {

	private Map<String, BasicVehicleType> vehicleTypes;
	private List<BasicVehicle> vehicles;


	public BasicVehicleBuilder(Map<String, BasicVehicleType> vehicleTypes,
			List<BasicVehicle> vehicles) {
		this.vehicleTypes = vehicleTypes;
		this.vehicles = vehicles;
	}


	public BasicVehicleType createVehicleType(String type) {
		if (!this.vehicleTypes.containsKey(type)) {
			BasicVehicleType veh = new BasicVehicleTypeImpl(type);
			this.vehicleTypes.put(type, veh);
			return veh;
		}
		throw new IllegalArgumentException("Vehicle type with id: " + type + " already exists!");
	}


	public BasicVehicleCapacity createVehicleCapacity() {
		return new BasicVehicleCapacityImpl();
	}


	public BasicFreightCapacity createFreigthCapacity() {
		return new BasicFreightCapacityImpl();
	}


	public BasicEngineInformation createEngineInformation(FuelType fuelType,
			double gasConsumption) {
			return new BasicEngineInformationImpl(fuelType, gasConsumption);
	}


	public BasicVehicle createVehicle(Id id, String type) {
		BasicVehicle veh = new BasicVehicleImpl(id, type);
		this.vehicles.add(veh);
		return veh;
	}

}
