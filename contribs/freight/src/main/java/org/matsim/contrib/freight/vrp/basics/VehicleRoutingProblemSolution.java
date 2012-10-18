package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;
import java.util.Collections;

public interface VehicleRoutingProblemSolution {
	
	public Collection<VehicleRoute> getRoutes();

	public double getTotalCost();

}
