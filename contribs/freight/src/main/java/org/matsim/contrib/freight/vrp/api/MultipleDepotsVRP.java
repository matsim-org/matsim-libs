package org.matsim.contrib.freight.vrp.api;


import java.util.Map;

import org.matsim.contrib.freight.vrp.basics.VehicleType;

public interface MultipleDepotsVRP extends VRP{
	
	public Map<String, Customer> getDepots();
	
	public void assignVehicleTypeToDepot(String depotId, VehicleType vehicleType);
	
	public VehicleType getVehicleType(String depotId);

}
