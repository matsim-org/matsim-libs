package org.matsim.contrib.transEnergySim.events;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPlug;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;

public interface ChargingEventHandler {

	void handleStartChargingEvent(double time, Id<Vehicle> vehicleId, Id<ChargingPlug> plugId);
	void handleEndChargingEvent(double time, Id<Vehicle> vehicleId, Id<ChargingPlug> plugId);
	void handleTimeStep(double time);

}
