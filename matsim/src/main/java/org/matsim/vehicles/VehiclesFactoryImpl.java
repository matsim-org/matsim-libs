/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import org.matsim.vehicles.EngineInformation.FuelType;

/**
 * @author dgrether
 */
public class VehiclesFactoryImpl implements VehiclesFactory {


	public VehiclesFactoryImpl() {
	}

	public Vehicle createVehicle(Id id, VehicleType type) {
		Vehicle veh = new VehicleImpl(id, type);
		return veh;
	}
	
	public VehicleType createVehicleType(Id typeId) {
			VehicleType veh = new VehicleTypeImpl(typeId);
			return veh;
	}


	public VehicleCapacity createVehicleCapacity() {
		return new VehicleCapacityImpl();
	}


	public FreightCapacity createFreigthCapacity() {
		return new FreightCapacityImpl();
	}


	public EngineInformation createEngineInformation(FuelType fuelType,
			double gasConsumption) {
			return new EngineInformationImpl(fuelType, gasConsumption);
	}
}
