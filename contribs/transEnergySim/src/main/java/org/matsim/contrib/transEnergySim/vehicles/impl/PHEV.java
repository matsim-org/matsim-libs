package org.matsim.contrib.transEnergySim.vehicles.impl;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.vehicles.api.AbstractHybridElectricVehicle;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;

public class PHEV extends AbstractHybridElectricVehicle {

	public PHEV(EnergyConsumptionModel ecm, EnergyConsumptionModel engineECM, double usableBatteryCapacityInJoules, Id<Vehicle> vehicleId) {
		this.electricDriveEnergyConsumptionModel=ecm;
		this.engineECM=engineECM;
		this.usableBatteryCapacityInJoules=usableBatteryCapacityInJoules;
		this.socInJoules=usableBatteryCapacityInJoules;
		this.vehicleId = vehicleId;
	}
	
	protected void logEngineEnergyConsumption(double energyConsumptionInJoule) {
		
	}

}
