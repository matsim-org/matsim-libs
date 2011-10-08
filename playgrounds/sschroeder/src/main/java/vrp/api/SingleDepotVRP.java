package vrp.api;

import vrp.basics.VehicleType;


public interface SingleDepotVRP extends VRP{
	
	public Customer getDepot();
	
	public VehicleType getVehicleType();

}
