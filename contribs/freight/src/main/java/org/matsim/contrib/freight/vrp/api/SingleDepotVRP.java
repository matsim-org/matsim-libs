package org.matsim.contrib.freight.vrp.api;

import org.matsim.contrib.freight.vrp.basics.VehicleType;


public interface SingleDepotVRP extends VRP{
	
	public Customer getDepot();
	
	public VehicleType getVehicleType();

}
