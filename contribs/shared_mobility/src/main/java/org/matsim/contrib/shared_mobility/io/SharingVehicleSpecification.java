package org.matsim.contrib.shared_mobility.io;

import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.service.SharingStation;
import org.matsim.contrib.shared_mobility.service.SharingVehicle;
import org.matsim.vehicles.Vehicle;

public interface SharingVehicleSpecification {
	Id<SharingVehicle> getId();

	Optional<Id<Link>> getStartLinkId();

	Optional<Id<SharingStation>> getStartStationId();

	default Id<Vehicle> getVehicleId() {
		return Id.createVehicleId(getId());
	}
}
