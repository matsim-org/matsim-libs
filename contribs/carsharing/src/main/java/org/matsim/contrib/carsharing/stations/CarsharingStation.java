package org.matsim.contrib.carsharing.stations;

import org.matsim.api.core.v01.network.Link;

public interface CarsharingStation {
	
	public String getStationId();

	public Link getLink();
}
