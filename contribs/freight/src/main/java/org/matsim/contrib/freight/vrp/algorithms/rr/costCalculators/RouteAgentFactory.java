package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import org.matsim.contrib.freight.vrp.basics.VehicleRoute;

public interface RouteAgentFactory {
		
	public RouteAgent createAgent(VehicleRoute route);

}
