package org.matsim.contrib.eventsBasedPTRouter.vehicleOccupancy;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public interface VehicleOccupancy {

	//Methods
	public double getVehicleOccupancy(Id<TransitStopFacility> stopOId, Id<TransitLine> lineId, Id<TransitRoute> routeId, double time);
		
}
