
package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

public interface ChargingPlugType {

	double getChargingPowerInKW();
	double getDischargingPowerInKW(); 
	boolean isV1GCapable();
	boolean isV2GCapable();
	String getPlugTypeName();
	
}