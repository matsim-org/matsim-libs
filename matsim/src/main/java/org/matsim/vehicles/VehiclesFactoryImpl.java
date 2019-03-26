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
 * deliberately non-public since there is an interface.  kai, nov'11
 * 
 * @author dgrether
 */
class VehiclesFactoryImpl implements VehiclesFactory {


	/**
	 * deliberately non-public since there is a factory.  kai, nov'11
	 */
	VehiclesFactoryImpl() {
	}

	@Override
	public Vehicle createVehicle(Id<Vehicle> id, VehicleType type) {
		Vehicle veh = new VehicleImpl(id, type);
		return veh;
	}
	
	@Override
	public VehicleType createVehicleType(Id<VehicleType> typeId) {
			VehicleType veh = new VehicleType(typeId);
			return veh;
	}


	@Override
	public VehicleCapacity createVehicleCapacity() {
		return new VehicleCapacityImpl();
	}


	@Override
	public FreightCapacity createFreigthCapacity() {
		return new FreightCapacityImpl();
	}


	@Override
	public EngineInformation createEngineInformation(FuelType fuelType,
			double gasConsumption) {
			return new EngineInformationImpl(fuelType, gasConsumption);
	}

	@Override
	public CostInformation createCostInformation(double fixedCosts, double costsPerMeter, double costsPerSecond) {
		return new CostInformationImpl(fixedCosts, costsPerMeter, costsPerSecond);
	}
}
