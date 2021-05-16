package org.matsim.contrib.freight.controler;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.vehicles.Vehicle;

class FreightControlerUtils{
	static final String CARRIER_VEHICLE = "carrierVehicle";
	public static Vehicle getVehicle( Plan plan ) {
		return (Vehicle) plan.getAttributes().getAttribute( CARRIER_VEHICLE );
	}
	static void putVehicle( Plan plan, Vehicle vehicle ){
		plan.getAttributes().putAttribute( CARRIER_VEHICLE, vehicle );
	}
}
