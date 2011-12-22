/* *********************************************************************** *
 * project: org.matsim.*
 * CreateVehiclesForHouseholds.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.vehicles;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class CreateVehiclesForHouseholds {

	private final Scenario scenario;
	private final Map<Id, HouseholdVehiclesInfo> householdVehicles;

	public CreateVehiclesForHouseholds(Scenario scenario, Map<Id, HouseholdVehiclesInfo> householdVehicles) {
		this.scenario = scenario;
		this.householdVehicles = householdVehicles;
	}
	
	public void run() {
		for (Household household : ((ScenarioImpl)this.scenario).getHouseholds().getHouseholds().values()) {
			this.createVehiclesForHousehold(household);
		}
	}
	
	private void createVehiclesForHousehold(Household household) {
		Vehicles vehicles = ((ScenarioImpl)this.scenario).getVehicles();
		
		HouseholdVehiclesInfo info = householdVehicles.get(household.getId());
		
		int numVehicles = info.getNumVehicles();
		String idString = household.getId().toString();
		List<Id> vehicleIds = household.getVehicleIds();
		if (numVehicles > 0) {
			Vehicle veh = vehicles.getFactory().createVehicle(scenario.createId(idString + "_veh1"), VehicleUtils.getDefaultVehicleType());
			VehicleCapacity cap = VehicleUtils.getFactory().createVehicleCapacity();
			veh.getType().setCapacity(cap);
			cap.setSeats(info.getFirstCapacity());
			vehicleIds.add(veh.getId());
			vehicles.getVehicles().put(veh.getId(), veh);
		}
		if (numVehicles > 1) {
			Vehicle veh = vehicles.getFactory().createVehicle(scenario.createId(idString + "_veh2"), VehicleUtils.getDefaultVehicleType());
			VehicleCapacity cap = VehicleUtils.getFactory().createVehicleCapacity();
			veh.getType().setCapacity(cap);
			cap.setSeats(info.getSecondCapacity());
			vehicleIds.add(veh.getId());
			vehicles.getVehicles().put(veh.getId(), veh);
		}
		if (numVehicles > 2) {
			Vehicle veh = vehicles.getFactory().createVehicle(scenario.createId(idString + "_veh3"), VehicleUtils.getDefaultVehicleType());
			VehicleCapacity cap = VehicleUtils.getFactory().createVehicleCapacity();
			veh.getType().setCapacity(cap);
			cap.setSeats(info.getThirdCapacity());
			vehicleIds.add(veh.getId());
			vehicles.getVehicles().put(veh.getId(), veh);
		}

	}
}
