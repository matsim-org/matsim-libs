package org.matsim.contrib.freight.carrier;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * VehicleTypeContainer mapping all vehicleTypes.
 * 
 * @author sschroeder
 *
 */
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
