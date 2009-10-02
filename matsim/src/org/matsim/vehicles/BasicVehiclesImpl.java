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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.basic.v01.Id;


/**
 * @author dgrether
 *
 */
public class BasicVehiclesImpl implements BasicVehicles {
	
	private Map<Id, BasicVehicleType> vehicleTypes;
	private LinkedHashMap<Id, BasicVehicle> vehicles;
	private BasicVehiclesFactoryImpl builder;

	public BasicVehiclesImpl(){
		this.vehicleTypes = new LinkedHashMap<Id, BasicVehicleType>();
		this.builder = new BasicVehiclesFactoryImpl();
		this.vehicles = new LinkedHashMap<Id, BasicVehicle>();
	}
	
	
	public VehiclesFactory getFactory() {
		return this.builder;
	}

	public Map<Id, BasicVehicle> getVehicles() {
		return this.vehicles;
	}


	public Map<Id, BasicVehicleType> getVehicleTypes() {
		return this.vehicleTypes;
	}

}
