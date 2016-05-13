package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
/**
@author rashid_waraich, colin_sheppard
TODO: descibe in detail how we think about this
*/
//TODO: move to: org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;

public interface ChargingPoint extends Identifiable<ChargingPoint> {
	
	ChargingSite getChargingSite();
	
	Collection<ChargingPlug> getAllChargingPlugs();
	
	// We assume that one charging plug can potentially serve multiple parking spots located adjacent to it. But only one parked car at a time can use it.
	// For example in the scenarios it could be assumed that the charger is released when charging is finished (e.g. electronic unlock - chargingPlugStatus=AVAILABLE).
	double getNumberOfAvailableParkingSpots();
	
	void registerVehicleArrival(double time, Id<Vehicle> vehicleId); 
	void registerVehicleDeparture(double time, Id<Vehicle> vehicleId);
	
	void addChargingPlug(ChargingPlug plug);
}