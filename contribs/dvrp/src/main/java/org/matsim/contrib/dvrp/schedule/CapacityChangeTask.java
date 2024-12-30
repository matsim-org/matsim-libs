package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoad;


/**
 * @author Tarek Chouaki (tkchouaki)
 */
public interface CapacityChangeTask extends StayTask {
	DvrpLoad getNewVehicleCapacity();
}
