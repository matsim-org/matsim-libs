package vrp.api;

import java.util.Map;

import vrp.basics.VehicleType;

public interface MultipleDepotsVRP extends VRP{
	
	public Map<String, Customer> getDepots();
	
	public void assignVehicleTypeToDepot(String depotId, VehicleType vehicleType);
	
	public VehicleType getVehicleType(String depotId);

}
