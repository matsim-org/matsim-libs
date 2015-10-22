package org.matsim.contrib.freight.carrier;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;

/**
 * VehicleTypeContainer mapping all vehicleTypes.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleTypes {
	
	public static CarrierVehicleTypes getVehicleTypes(Carriers carriers){
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		for(Carrier c : carriers.getCarriers().values()){
			for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles()){
				CarrierVehicleType vehicleType = v.getVehicleType();
				if(vehicleType != null){
					types.getVehicleTypes().put(vehicleType.getId(), vehicleType);
				}
			}
		}
		return types;
	}
	
	private Map<Id<VehicleType>,CarrierVehicleType> vehicleTypes;

	public CarrierVehicleTypes() {
		super();
		this.vehicleTypes = new HashMap<>();
	}

	public Map<Id<VehicleType>, CarrierVehicleType> getVehicleTypes() {
		return vehicleTypes;
	}
}
