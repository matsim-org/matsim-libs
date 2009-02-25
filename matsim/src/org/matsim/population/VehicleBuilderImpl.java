/* *********************************************************************** *
 * project: org.matsim.*
 * VehicleBuilder
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
package org.matsim.population;

import java.util.Map;

import org.matsim.basic.v01.BasicVehicle;
import org.matsim.basic.v01.BasicVehicleBuilder;
import org.matsim.basic.v01.BasicVehicleType;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Vehicle;



/**
 * @author dgrether
 *
 */
public class VehicleBuilderImpl extends BasicVehicleBuilder {

	public VehicleBuilderImpl(Map<String, BasicVehicleType> vehicleTypes,
			Map<Id, Vehicle> vehicles) {
		super(vehicleTypes, (Map)vehicles);
	}

	@Override
	public BasicVehicle createVehicle(Id id, String type) {
		BasicVehicleType t = this.getVehicleTypes().get(type);
		if (t == null) {
			throw new IllegalArgumentException("Cannot create vehicle for unknown VehicleType: "+ type);
		}
		Vehicle v = new VehicleImpl(id, t);
		this.getVehicles().put(id, v);
		return v;
	}
}
