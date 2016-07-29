package org.matsim.contrib.transEnergySim.chargingInfrastructure.management;

import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingLevel;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary.ChargingPlugType;

public interface ChargingSitePolicy {
	double getParkingPriceQuote(double time, double duration);
	
	double getChargingPriceQuote(double time, double duration, ChargingPlugType plugType);
}

