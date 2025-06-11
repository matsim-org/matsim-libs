/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers;

import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;

/**
 * VehicleTypeContainer mapping all vehicleTypes.
 *
 * @author sschroeder
 *
 */
public class CarrierVehicleTypes {

	public static CarrierVehicleTypes getVehicleTypes(Carriers carriers){
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		for(Carrier c : carriers.getCarriers().values()){
			for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles().values()){
				VehicleType vehicleType = v.getType();
				if(vehicleType != null){
					types.getVehicleTypes().put(vehicleType.getId(), vehicleType);
				}
			}
		}
		return types;
	}

	private final Map<Id<VehicleType>, VehicleType> vehicleTypes;

	public CarrierVehicleTypes() {
		super();
		this.vehicleTypes = new HashMap<>();
	}

	public Map<Id<VehicleType>, VehicleType> getVehicleTypes() {
		return vehicleTypes;
	}
}
