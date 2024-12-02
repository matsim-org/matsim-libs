package org.matsim.contrib.dvrp.fleet.dvrp_load;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.vehicles.Vehicle;

/**
 * This interface specifies how to build {@link DvrpVehicle} capacities (expressed using a {@link DvrpLoad} when building the vehicles from standard MATSim {@link Vehicle} objects.
 * @author Tarek Chouaki (tkchouaki)
 */
public interface DvrpLoadFromVehicle {
	DvrpLoad getLoad(Vehicle vehicle);
}
