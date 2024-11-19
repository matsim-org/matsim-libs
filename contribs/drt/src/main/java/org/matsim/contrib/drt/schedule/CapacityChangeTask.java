package org.matsim.contrib.drt.schedule;

import org.matsim.contrib.dvrp.fleet.DvrpVehicleLoad;

import java.util.Random;

public interface CapacityChangeTask extends DrtStopTask{

	DvrpVehicleLoad getNewVehicleCapacity();
}
