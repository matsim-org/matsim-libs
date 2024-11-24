package org.matsim.contrib.dvrp.schedule;

import org.matsim.contrib.dvrp.fleet.DvrpLoad;


public interface CapacityChangeTask extends StayTask {

	DvrpLoad getNewVehicleCapacity();
}
