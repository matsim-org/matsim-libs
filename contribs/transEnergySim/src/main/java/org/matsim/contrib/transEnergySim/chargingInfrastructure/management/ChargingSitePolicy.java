package org.matsim.contrib.transEnergySim.chargingInfrastructure.management;

import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingLevel;

public interface ChargingSitePolicy {
	double getParkingPriceQuote(double time, double duration);
	
	double getChargingPriceQuote(double time, double duration, ChargingLevel chargingLevel);
}

