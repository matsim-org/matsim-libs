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
	
	Collection<ChargingLevel> getAvailableChargingLevels();
		
	void plugVehicle(VehicleWithBattery vehicle, double time);
	void unPlugVehicle(VehicleWithBattery vehicle, double time);

	
	void selectChargingLevel(ChargingLevel chargingLevel);
	
}
