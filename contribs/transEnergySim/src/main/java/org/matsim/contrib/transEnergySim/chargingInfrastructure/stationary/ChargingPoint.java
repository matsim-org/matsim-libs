package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
/**
@author rashid_waraich, colin_sheppard

TODO: descibe in detail how we think about this

*/
//TODO: move to: org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;

public interface ChargingPoint {
	
	ChargingSite getChargingSite();
	
	double getParkingPriceQuote(double time, double duration);
	
	double getChargingPriceQuote(double time, double duration);
	
	// TODO: plug-in this at right point in the simulation
	// TODO: change from Vehicle to ChargableVehicle resp. Plug-in Electric Vehicle
	// TODO: create log events
	void registerVehicleArrival(double time, Vehicle vehicle); // => available parking decrease // => check, that no over use of parking capacity!
	
	//TODO: -> see arrival notes...
	void registerVehicleDeparture(double time, Vehicle vehicle);
	
	// TODO: this should be invoked in MATSim simulator
	// TODO: we assume, the charger is released when charging is finished (e.g. electronic unlock).
	// TODO: log end of charging
	// TODO: create proper test cases
	// TODO: this method takes over full control, when to release charging.
	//	=> event priority queue
	void startCharging(double time, Vehicle vehicle);
	
	
}
