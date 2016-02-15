package org.matsim.contrib.parking.parkingChoice.carsharing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.PC2.infrastructure.PC2Parking;
import org.matsim.vehicles.Vehicle;

public class ParkingLinkInfo {
	public ParkingLinkInfo(Id<Vehicle> vehicleId, Id<Link> linkId) {
		super();
		this.vehicleId = vehicleId;
		this.linkId = linkId;
	}

	public ParkingLinkInfo(Id<Vehicle> vehicleId, Id<Link> linkId, PC2Parking parking) {
		super();
		this.vehicleId = vehicleId;
		this.linkId = linkId;
		this.parking = parking;
	}

	Id<Vehicle> vehicleId;
	Id<Link> linkId;
	PC2Parking parking;

	public PC2Parking getParking() {
		return parking;
	}

	public void setParking(PC2Parking parking) {
		this.parking = parking;
	}

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}
}
