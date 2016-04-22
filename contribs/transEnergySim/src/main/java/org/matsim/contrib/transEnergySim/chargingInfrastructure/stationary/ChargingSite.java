package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.management.ChargingNetworktOperator;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.management.ChargingSitePolicy;

public interface ChargingSite extends Identifiable<ChargingSite> {
	
	

	abstract Collection<ChargingPlug> getAvailableChargingPlugsOfChargingLevel(ChargingLevel chargingLevel);
	
	abstract Collection<ChargingPlug> getAllChargingPlugs();
	
	abstract Collection<ChargingPoint> getAllChargingPoints();
	
	abstract Coord getCoord();
	
	abstract boolean isStationOpen(double time, double duration);
	
	abstract void addChargingPoint(ChargingPoint chargingPoint);
	
	
	// this property can be used for applications where the grid operation/simulation is integrated
	// each Utility Operator can be modelled as a separate entity which drives the prices of its
	// charging points
	abstract ChargingNetworktOperator getChargingNetworktOperator();
	
	double getParkingPriceQuote(double time, double duration);
	
	double getChargingPriceQuote(double time, double duration, ChargingLevel chargingLevel);
	
	void setChargingSitePolicy(ChargingSitePolicy chargingSitePolicy);
	
}
