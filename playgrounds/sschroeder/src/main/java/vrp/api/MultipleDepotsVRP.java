package vrp.api;

import vrp.basics.VehicleType;

import java.util.Map;

public interface MultipleDepotsVRP extends VRP{
	
	public Map<String, Customer> getDepots();
	
	public void assignVehicleTypeToDepot(String depotId, VehicleType vehicleType);
	
	public VehicleType getVehicleType(String depotId);

}
