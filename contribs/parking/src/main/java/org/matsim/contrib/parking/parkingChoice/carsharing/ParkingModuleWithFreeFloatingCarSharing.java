package org.matsim.contrib.parking.parkingChoice.carsharing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public interface ParkingModuleWithFreeFloatingCarSharing {

	/**
	 * gives back the closest available vehicle and and its parking location
	 *
	 * @param coord
	 * @return
	 */
	public ParkingInfo getNextFreeFloatingVehicle(Coord coord);
	
	/**
	 * finds closest available parking for vehicle from destCoord
	 * (gives back coordinate, where vehicle parked) 
	 * 
	 * @param vehicleId
	 * @param destCoord
	 * @return
	 */
	public ParkingInfo parkFreeFloatingVehicle(Id vehicleId, Coord destCoord);
	
}
