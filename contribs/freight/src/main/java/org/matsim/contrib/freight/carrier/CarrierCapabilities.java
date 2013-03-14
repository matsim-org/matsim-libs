package org.matsim.contrib.freight.carrier;

import java.util.ArrayList;
import java.util.Collection;

public class CarrierCapabilities {

	public static CarrierCapabilities newInstance(){
		return new CarrierCapabilities();
	}
	
	private Collection<CarrierVehicle> carrierVehicles = new ArrayList<CarrierVehicle>();

	public Collection<CarrierVehicle> getCarrierVehicles() {
		return carrierVehicles;
	}

}
