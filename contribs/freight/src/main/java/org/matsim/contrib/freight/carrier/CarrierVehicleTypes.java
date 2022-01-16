package org.matsim.contrib.freight.carrier;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

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
			for(CarrierVehicle v : c.getCarrierCapabilities().getCarrierVehicles().values()){
				VehicleType vehicleType = v.getType();
				if(vehicleType != null){
					types.getVehicleTypes().put(vehicleType.getId(), vehicleType);
				}
			}
		}
		return types;
	}
	
	private Map<Id<VehicleType>, VehicleType> vehicleTypes;

	public CarrierVehicleTypes() {
		super();
		this.vehicleTypes = new HashMap<>();
//		VehicleType defaultVehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("default", VehicleType.class ) );
//		this.vehicleTypes.put( defaultVehicleType.getId(), defaultVehicleType );
	}

	public Map<Id<VehicleType>, VehicleType> getVehicleTypes() {
		return vehicleTypes;
	}
}
