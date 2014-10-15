package org.matsim.contrib.parking.parkingChoice.carsharing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public class ParkingLinkInfo {
	public ParkingLinkInfo(Id<Vehicle> vehicleId, Id<Link> linkId) {
		super();
		this.vehicleId = vehicleId;
		this.linkId = linkId;
	}

	Id<Vehicle> vehicleId;
	Id<Link> linkId;

	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}
}
