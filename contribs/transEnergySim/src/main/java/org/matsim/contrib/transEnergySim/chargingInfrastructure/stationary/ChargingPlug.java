package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.transEnergySim.events.ChargingEvent;
import org.matsim.contrib.transEnergySim.vehicles.api.VehicleWithBattery;

public interface ChargingPlug extends Identifiable<ChargingPlug> {

	ChargingPoint getChargingPoint();
	
	ChargingPlugStatus getChargingPlugStatus();
	ChargingPlugType getChargingPlugType();
	
	void plugVehicle(VehicleWithBattery vehicle, double time);
	void unPlugVehicle(VehicleWithBattery vehicle, double time);

	VehicleWithBattery getVehicle();

	ChargingEvent estimateEndChargingEvent(double time);

	double getMaxChargingPowerInWatt();
	double getActualChargingPowerInWatt();

	double getEnergyDeliveredByTime(double time);
}
