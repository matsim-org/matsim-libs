package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.management.ChargingNetworktOperator;

public interface ChargingSite {

	Collection<ChargingPlug> getAvailableChargingStationsOfChargingLevel(ChargingLevel chargingLevel);
	
	Collection<ChargingPlug> getAllChargingPlugs();
	
	Collection<ChargingPoint> getAllChargingPoints();
	
	Coord getChargingSiteCoordinate();
	
	boolean isStationOpen(double time, double duration);
	
	
	// this property can be used for applications where the grid operation/simulation is integrated
	// each Utility Operator can be modelled as a separate entity which drives the prices of its
	// charging points
	ChargingNetworktOperator getChargingNetworktOperator();
	
	
	
}
