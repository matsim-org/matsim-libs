package org.matsim.contrib.shared_mobility.service;

import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.routing.InteractionPoint;

public class VehicleInteractionPoint extends InteractionPoint {
	private final SharingVehicle vehicle;

	protected VehicleInteractionPoint(SharingVehicle vehicle, Id<Link> linkId, Optional<Id<SharingStation>> stationId) {
		super(linkId, stationId);
		this.vehicle = vehicle;
	}

	public SharingVehicle getVehicle() {
		return vehicle;
	}

	static public VehicleInteractionPoint of(SharingVehicle vehicle) {
		return new VehicleInteractionPoint(vehicle, vehicle.getLink().getId(), Optional.empty());
	}

	static public VehicleInteractionPoint of(SharingVehicle vehicle, SharingStation station) {
		return new VehicleInteractionPoint(vehicle, vehicle.getLink().getId(), Optional.of(station.getId()));
	}
}
