/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdModeAssignment.java
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

package playground.christoph.evacuation.withinday.replanning.utils;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

public class HouseholdModeAssignment {

	private Map<Id, String> transportModeMap = new HashMap<Id, String>();
	private Map<Id<Person>, Id<Vehicle>> driverVehicleMap = new HashMap<>();
	private Map<Id, Id> passengerVehicleMap = new HashMap<Id, Id>();
	
	public void addTransportModeMapping(Id personId, String transportMode) {
		this.transportModeMap.put(personId, transportMode);
	}
	
	public void addDriverVehicleMapping(Id personId, Id verhicleId) {
		this.driverVehicleMap.put(personId, verhicleId);
	}
	
	public void addPassengerVehicleMapping(Id personId, Id verhicleId) {
		this.passengerVehicleMap.put(personId, verhicleId);
	}
	
	public Map<Id, String> getTransportModeMap() {
		return this.transportModeMap;
	}
	
	public Map<Id<Person>, Id<Vehicle>> getDriverVehicleMap() {
		return this.driverVehicleMap;
	}
	
	public Map<Id, Id> getPassengerVehicleMap() {
		return this.passengerVehicleMap;
	}
}
