package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;

public interface VehicleRoutingProblemSolution {

	public Collection<VehicleRoute> getRoutes();

	public double getTotalCost();

}
