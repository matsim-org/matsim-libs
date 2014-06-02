package org.matsim.contrib.parking.parkingChoice.carsharing;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

public class ParkingLinkInfo {
	public ParkingLinkInfo(Id vehicleId, Id linkId) {
		super();
		this.vehicleId = vehicleId;
		this.linkId = linkId;
	}

	Id vehicleId;
	Id linkId;

	public Id getVehicleId() {
		return vehicleId;
	}

	public Id getLinkId() {
		return linkId;
	}
}
