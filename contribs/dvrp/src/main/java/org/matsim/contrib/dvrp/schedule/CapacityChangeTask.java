package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.fleet.DvrpVehicleLoad;


public interface CapacityChangeTask extends StayTask {

	DvrpVehicleLoad getNewVehicleCapacity();
}
