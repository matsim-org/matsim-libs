package org.matsim.contrib.transEnergySim.chargingInfrastructure.management;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.agents.VehicleAgent;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPlug;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPlugType;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;

public interface ChargingNetworkOperator {

	Id getChargingNetworktOperatorId();
	
	String getName();

	double estimateChargingSessionDuration(ChargingSitePolicy chargingSitePolicy, ChargingPlugType chargingPlugType, VehicleWithBattery vehicle);
	double determineEnergyDelivered(ChargingPlug plug, VehicleWithBattery vehicle, double duration);

	double getTimeToDequeueNextVehicle(ChargingPlug plug, VehicleAgent agent);
	
}
