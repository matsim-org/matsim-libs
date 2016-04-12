package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;

public interface ChargingPlug {

	double getChargingPowerInWatt(); 
	
	ChargingPoint getChargingPoint();
	
	ChargingPlugStatus getChargingPlugStatus();
	
	Collection<ChargingLevel> getChargingLevel();
	
	// TODO: put charging intelligence here according to plug/agent preference/grid operator
	
	
	// we assume that one charging point can have potentially multiple parking spots
		// located adjacent to it. But only one parked car at a time can use it.
		// we assume, the charger is released when charging is finished (e.g. electronic unlock).
		double getNumberOfAvailableParkingSpots();
		
	// TODO: provide constructor for setting parking capacity 
		
		
	void plugVehicle(VehicleWithBattery vehicle, double time);
	
	
	void unPlugVehicle(VehicleWithBattery vehicle, double time);
	
}
