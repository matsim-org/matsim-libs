/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.core.events.algorithms;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Basic event handler that collects the relation between vehicles and drivers.
 * Necessary since link enter and leave events do not contain the driver anymore.
 * 
 * @author tthunig
 */
public final class Vehicle2DriverEventHandler implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private final Map<Id<Vehicle>, Id<Person>> driverAgents = new HashMap<>();
	
	@Override
	public void reset(int iteration) {
		driverAgents.clear();
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		driverAgents.put(event.getVehicleId(), event.getPersonId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		driverAgents.remove(event.getVehicleId());
	}
	
	/**
	 * @param vehicleId
	 * @return person id of the driver
	 */
	public Id<Person> getDriverOfVehicle(Id<Vehicle> vehicleId){
		return driverAgents.get(vehicleId);
	}

}
