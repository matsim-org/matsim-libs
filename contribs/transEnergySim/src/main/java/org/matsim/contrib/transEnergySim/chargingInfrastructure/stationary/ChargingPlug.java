package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

import java.util.Collection;
import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;

public interface ChargingPlug extends Identifiable<ChargingPlug> {

	double getChargingPowerInWatt(); 
	
	ChargingPoint getChargingPoint();
	
	ChargingPlugStatus getChargingPlugStatus();
	
	HashMap<ChargingLevel,Double> getAvailableChargingLevels();
	
	// We assume that one charging plug can potentially serve multiple parking spots located adjacent to it. But only one parked car at a time can use it.
	// For example in the scenarios it could be assumed that the charger is released when charging is finished (e.g. electronic unlock - chargingPlugStatus=AVAILABLE).
	double getNumberOfAvailableParkingSpots();
		
	void plugVehicle(VehicleWithBattery vehicle, double time);
	void unPlugVehicle(VehicleWithBattery vehicle, double time);
	
	void registerVehicleArrival(double time, Id<Vehicle> vehicleId); 
	void registerVehicleDeparture(double time, Id<Vehicle> vehicleId);
	
	void selectChargingLevel(ChargingLevel chargingLevel);
	
	double getParkingPriceQuote(double time, double duration);
	
	double getChargingPriceQuote(double time, double duration);
	
}
