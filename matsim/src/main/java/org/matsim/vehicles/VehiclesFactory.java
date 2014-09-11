/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.vehicles.EngineInformation.FuelType;

public interface VehiclesFactory extends MatsimFactory {

	public VehicleType createVehicleType(Id<VehicleType> type);

	public VehicleCapacity createVehicleCapacity();

	public FreightCapacity createFreigthCapacity();

	public EngineInformation createEngineInformation(FuelType fuelType,
			double gasConsumption);

	public Vehicle createVehicle(Id<Vehicle> id, VehicleType type);

}