package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

public interface UtilityOperatorSimulationManager {

	//TODO: integrate this into the simulation at the correct location.
	
	//TODO: evolutionary algorithm for this is missing, where all operators could optimize prices for themselves
	void performPricingUpdatesForChargingPoints();
	
	
}
