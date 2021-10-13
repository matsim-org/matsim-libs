package org.matsim.contrib.sharing.io;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.sharing.service.SharingStation;

public interface SharingStationSpecification {
	Id<SharingStation> getId();

	Id<Link> getLinkId();

	int getCapacity();
}
