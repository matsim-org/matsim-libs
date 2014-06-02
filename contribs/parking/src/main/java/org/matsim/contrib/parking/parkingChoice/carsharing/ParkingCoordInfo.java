package org.matsim.contrib.parking.parkingChoice.carsharing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class ParkingCoordInfo {
	public ParkingCoordInfo(Id vehicleId, Coord parkingCoordinate) {
		super();
		this.vehicleId = vehicleId;
		this.parkingCoordinate = parkingCoordinate;
	}

	Id vehicleId;
	Coord parkingCoordinate;

	public Id getVehicleId() {
		return vehicleId;
	}

	public Coord getParkingCoordinate() {
		return parkingCoordinate;
	}
}
