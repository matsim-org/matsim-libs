package org.matsim.contrib.carsharing.models;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;

import com.google.inject.Inject;

public class ChooseVehicleTypeExample implements ChooseVehicleType {
	
	@Inject CarsharingSupplyInterface carsharingSupply;
	@Inject MembershipContainer membershipContainer;
		
	@Override
	public String getPreferredVehicleType(Plan plan, Leg currentLeg){
		
		String vehicleType = "car";
		return vehicleType;
	}

}
