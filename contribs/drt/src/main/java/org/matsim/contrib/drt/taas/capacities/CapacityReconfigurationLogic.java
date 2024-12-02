package org.matsim.contrib.drt.taas.capacities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoad;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.List;

/**
 * This interface allows to define logics for planning DRT vehicle capacities ({@link org.matsim.contrib.dvrp.schedule.CapacityChangeTask}) for the whole day at the beginning of the iterations.
 * @author Tarek Chouaki (tkchouaki)
 */
public interface CapacityReconfigurationLogic {

	/**
	 * Describes a capacity change task to be scheduled for a vehicle
	 * @param time
	 * @param linkId
	 * @param nextCapacity
	 */
	record CapacityChangeItem(double time, Id<Link> linkId, DvrpLoad nextCapacity) {

	}

	/**
	 * This can be used to override the capacities coming from {@link org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification} at the beginning of the iteration, so that the vehicle starts with a different capacity without creating capacity change tasks
	 * @return a map between vehicle id and starting capacities. Vehicles from the fleet that do not appear here will start the day with the default capacity coming from the specification.
	 */
	IdMap<DvrpVehicle, DvrpLoad> getOverriddenStartingCapacities();

	/**
	 * @param dvrpVehicle the {@link DvrpVehicle} for which the capacity changes are requested
	 * @return the list of capacity changes to be planned for the vehicle during the day
	 */
	List<DefaultCapacityReconfigurationLogic.CapacityChangeItem> getPreScheduledCapacityChanges(DvrpVehicle dvrpVehicle);
}
