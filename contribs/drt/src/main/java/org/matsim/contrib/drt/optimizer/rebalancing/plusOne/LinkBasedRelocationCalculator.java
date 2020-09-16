package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy.Relocation;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

public interface LinkBasedRelocationCalculator {
	List<Relocation> calcRelocations(List<Link> targetLinks, List<? extends DvrpVehicle> rebalancableVehicles);
}
