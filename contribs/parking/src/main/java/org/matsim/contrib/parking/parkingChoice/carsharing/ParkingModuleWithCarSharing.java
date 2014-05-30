package org.matsim.contrib.parking.parkingChoice.carsharing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public interface ParkingModuleWithCarSharing {

	public ParkingInfo getNextFreeFloatingVehicle(Coord coord);
	public ParkingInfo parkFreeFloatingVehicle(Id vehicleId, Coord destCoord);
	
}
