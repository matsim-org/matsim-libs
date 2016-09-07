package org.matsim.contrib.transEnergySim.chargingInfrastructure.management;

import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingLevel;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPlugType;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;

public interface ChargingSitePolicy {
	double getParkingCost(double time, double duration);
	
	double getChargingCost(double time, double duration, ChargingPlugType plugType, VehicleWithBattery vehicle);
}

