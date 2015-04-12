package org.matsim.contrib.eventsBasedPTRouter.vehicleOccupancy;

import org.matsim.api.core.v01.Id;

public interface VehicleOccupancy {

	//Methods
	public double getVehicleOccupancy(Id stopOId, Id lineId, Id routeId, double time);
		
}
