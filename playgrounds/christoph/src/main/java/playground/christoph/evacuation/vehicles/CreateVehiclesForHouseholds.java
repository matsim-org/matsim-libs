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
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.utils.misc.Counter;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class CreateVehiclesForHouseholds {

	private final Counter counter3Plus;
	private final Scenario scenario;
	private final Map<Id<Household>, HouseholdVehiclesInfo> householdVehicles;

	public CreateVehiclesForHouseholds(Scenario scenario, Map<Id<Household>, HouseholdVehiclesInfo> householdVehicles) {
		this.scenario = scenario;
		this.householdVehicles = householdVehicles;
		this.counter3Plus = new Counter("Created additional vehicles for households with three or more cars: ");
	}
	
	public void run() {
		for (Household household : ((ScenarioImpl)this.scenario).getHouseholds().getHouseholds().values()) {
			this.createVehiclesForHousehold(household);
		}
		this.counter3Plus.printCounter();

	}

	private void createVehiclesForHousehold(Household household) {
		Vehicles vehicles = ((ScenarioImpl)this.scenario).getTransitVehicles();

		HouseholdVehiclesInfo info = householdVehicles.get(household.getId());
		
		int numVehicles = info.getNumVehicles();
		String idString = household.getId().toString();
		List<Id<Vehicle>> vehicleIds = household.getVehicleIds();
		if (numVehicles > 0) {
			Vehicle veh = vehicles.getFactory().createVehicle(Id.create(idString + "_veh1", Vehicle.class), VehicleUtils.getDefaultVehicleType());
			VehicleCapacity cap = VehicleUtils.getFactory().createVehicleCapacity();
			veh.getType().setCapacity(cap);
			cap.setSeats(info.getFirstCapacity());
			vehicleIds.add(veh.getId());
			vehicles.addVehicle( veh);
		}
		if (numVehicles > 1) {
			Vehicle veh = vehicles.getFactory().createVehicle(Id.create(idString + "_veh2", Vehicle.class), VehicleUtils.getDefaultVehicleType());
			VehicleCapacity cap = VehicleUtils.getFactory().createVehicleCapacity();
			veh.getType().setCapacity(cap);
			cap.setSeats(info.getSecondCapacity());
			vehicleIds.add(veh.getId());
			vehicles.addVehicle( veh);
		}
		if (numVehicles > 2) {
			Vehicle veh = vehicles.getFactory().createVehicle(Id.create(idString + "_veh3", Vehicle.class), VehicleUtils.getDefaultVehicleType());
			VehicleCapacity cap = VehicleUtils.getFactory().createVehicleCapacity();
			veh.getType().setCapacity(cap);
			cap.setSeats(info.getThirdCapacity());
			vehicleIds.add(veh.getId());
			vehicles.addVehicle( veh);
		}
		
		/*
		 * We check for each household how many people have always a car available.
		 * If this is more than the model predicts, we create additional cars.
		 */
		int alwaysCarAvailable = 0;
		for (Id id : household.getMemberIds()) {
			Person person = scenario.getPopulation().getPersons().get(id);
			String carAvailability = ((PersonImpl) person).getCarAvail();
			if (carAvailability != null && carAvailability.equals("always")) alwaysCarAvailable++;
		}
		for (int i = numVehicles + 1; i <= alwaysCarAvailable; i++) {
			Vehicle veh = vehicles.getFactory().createVehicle(Id.create(idString + "_veh" + i, Vehicle.class), VehicleUtils.getDefaultVehicleType());
			VehicleCapacity cap = VehicleUtils.getFactory().createVehicleCapacity();
			veh.getType().setCapacity(cap);
			cap.setSeats(5);
			vehicleIds.add(veh.getId());
			vehicles.addVehicle( veh);
			counter3Plus.incCounter();
		}
	}
}