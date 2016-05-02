package org.matsim.contrib.parking.parkingChoice.carsharing;

import java.util.Collection;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;


public interface ParkingModuleWithFreeFloatingCarSharing {

	/**
	 * gives back the closest available vehicle and and its parking location
	 *
	 * @param coord
	 * @return
	 */
	public ParkingLinkInfo getNextFreeFloatingVehicle(Coord coord, Id personId, double time);
	
	/**
	 * finds closest available parking for vehicle from destCoord
	 * (gives back coordinate, where vehicle parked) 
	 * 
	 * @param vehicleId
	 * @param destCoord
	 * @return
	 */
	public ParkingLinkInfo parkFreeFloatingVehicle(Id vehicleId, Coord destCoord, Id personId, double time);
	
	
	public void resetForNewIterationStart();

	void makeFFVehicleAvailable(Id vehicleId, PC2Parking parking);

	void makeFFVehicleUnavailable(Id vehicleId, PC2Parking parking, double departureTime, Id personId);	
	
}
