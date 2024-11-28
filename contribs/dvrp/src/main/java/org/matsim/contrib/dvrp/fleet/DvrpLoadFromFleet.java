package org.matsim.contrib.dvrp.fleet;

import org.matsim.api.core.v01.Id;

public interface DvrpLoadFromFleet {
	DvrpLoad getDvrpVehicleLoad(int capacity, Id<DvrpVehicle> vehicleId);
}
