package org.matsim.contrib.shared_mobility.io;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.shared_mobility.service.SharingStation;

public interface SharingStationSpecification {
	Id<SharingStation> getId();

	Id<Link> getLinkId();

	int getCapacity();
}
