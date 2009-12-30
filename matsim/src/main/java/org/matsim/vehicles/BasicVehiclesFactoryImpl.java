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
import org.matsim.vehicles.BasicEngineInformation.FuelType;

/**
 * @author dgrether
 */
public class BasicVehiclesFactoryImpl implements VehiclesFactory {


	public BasicVehiclesFactoryImpl() {
	}

	public BasicVehicle createVehicle(Id id, BasicVehicleType type) {
		BasicVehicle veh = new BasicVehicleImpl(id, type);
		return veh;
	}
	
	public BasicVehicleType createVehicleType(Id typeId) {
			BasicVehicleType veh = new BasicVehicleTypeImpl(typeId);
			return veh;
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
}
