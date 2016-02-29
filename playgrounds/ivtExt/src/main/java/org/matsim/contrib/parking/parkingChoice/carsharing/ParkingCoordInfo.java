package org.matsim.contrib.parking.parkingChoice.carsharing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.Vehicle;

public class ParkingCoordInfo {
	public ParkingCoordInfo(Id<Vehicle> vehicleId, Coord parkingCoordinate) {
		super();
		this.vehicleId = vehicleId;
		this.parkingCoordinate = parkingCoordinate;
	}

	Id<Vehicle> vehicleId;
	Coord parkingCoordinate;

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public Coord getParkingCoordinate() {
		return parkingCoordinate;
	}
}
