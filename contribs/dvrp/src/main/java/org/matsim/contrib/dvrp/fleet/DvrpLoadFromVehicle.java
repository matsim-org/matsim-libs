package org.matsim.contrib.dvrp.fleet;

import org.matsim.vehicles.Vehicle;

public interface DvrpLoadFromVehicle {
	DvrpLoad getLoad(Vehicle vehicle);
}
