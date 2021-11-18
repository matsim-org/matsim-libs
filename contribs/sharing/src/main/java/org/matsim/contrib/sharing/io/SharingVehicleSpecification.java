package org.matsim.contrib.sharing.io;

import java.util.Optional;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.sharing.service.SharingStation;
import org.matsim.contrib.sharing.service.SharingVehicle;

public interface SharingVehicleSpecification {
	Id<SharingVehicle> getId();

	Optional<Id<Link>> getStartLinkId();

	Optional<Id<SharingStation>> getStartStationId();
}
