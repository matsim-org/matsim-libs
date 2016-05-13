package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

import java.util.Collection;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;

public interface ChargingPlug extends Identifiable<ChargingPlug> {

	double getChargingPowerInWatt(); 
	
	ChargingPoint getChargingPoint();
	
	ChargingPlugStatus getChargingPlugStatus();
	ChargingPlugType getChargingPlugType();
	
	void plugVehicle(VehicleWithBattery vehicle, double time);
	void unPlugVehicle(VehicleWithBattery vehicle, double time);
}