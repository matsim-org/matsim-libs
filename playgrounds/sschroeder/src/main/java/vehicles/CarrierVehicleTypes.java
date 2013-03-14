package vehicles;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;

public class CarrierVehicleTypes {
	
	private Map<Id,CarrierVehicleType> vehicleTypes;

	public CarrierVehicleTypes() {
		super();
		this.vehicleTypes = new HashMap<Id, CarrierVehicleType>();
	}

	public Map<Id, CarrierVehicleType> getVehicleTypes() {
		return vehicleTypes;
	}
}
