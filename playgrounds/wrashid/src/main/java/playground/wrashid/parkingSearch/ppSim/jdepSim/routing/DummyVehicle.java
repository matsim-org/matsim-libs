/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.wrashid.parkingSearch.ppSim.jdepSim.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

public class DummyVehicle implements Vehicle {


	private final VehicleType vt = new VehicleTypeImpl(Id.create("dummy", VehicleType.class));
	private final Id<Vehicle> id;

	public DummyVehicle() {
		this.id = Id.create("dummy", Vehicle.class);
	}

	@Override
	public Id<Vehicle> getId() {
		return this.id;
	}

	@Override
	public VehicleType getType() {
		return this.vt;
	}

}