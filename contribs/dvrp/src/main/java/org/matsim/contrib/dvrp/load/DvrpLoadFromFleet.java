package org.matsim.contrib.dvrp.load;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetReader;

/**
 * This interface allows to specify how the {@link FleetReader} interprets the
 * capacity attribute of vehicle specifications into a {@link DvrpLoad}
 * 
 * @author Tarek Chouaki (tkchouaki), IRT SystemX
 */
public interface DvrpLoadFromFleet {
	DvrpLoad getDvrpVehicleLoad(int capacity, Id<DvrpVehicle> vehicleId);
}
