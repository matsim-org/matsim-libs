package org.matsim.demandmodeling.primloc;

import org.matsim.world.Zone;

public interface PrimlocTravelCostAggregator {
	double travelCost( Zone i, Zone j);
}
