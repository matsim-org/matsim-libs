package org.matsim.contrib.drt.taas.capacities;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpLoad;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.List;

public interface CapacityReconfigurationLogic {
	record CapacityChangeItem(double time, Id<Link> linkId, DvrpLoad nextCapacity) {

	}

	IdMap<DvrpVehicle, DvrpLoad> getOverriddenStartingCapacities();

	List<DefaultCapacityConfigurationLogic.CapacityChangeItem> getPreScheduledCapacityChanges(DvrpVehicle dvrpVehicle);
}
