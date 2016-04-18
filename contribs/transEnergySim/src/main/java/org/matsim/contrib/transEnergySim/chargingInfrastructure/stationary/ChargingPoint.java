package org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;
/**
@author rashid_waraich, colin_sheppard

TODO: descibe in detail how we think about this

*/
//TODO: move to: org.matsim.contrib.transEnergySim.chargingInfrastructure.stationary
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;

public interface ChargingPoint extends Identifiable<ChargingPoint> {
	
	ChargingSite getChargingSite();
	
	Collection<ChargingPlug> getAllChargingPlugs();
	
	void addChargingPlug(ChargingPlug chargingPlug);
	
	
}
