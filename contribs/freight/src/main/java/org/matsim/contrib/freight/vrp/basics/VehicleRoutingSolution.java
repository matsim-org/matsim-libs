package org.matsim.contrib.freight.vrp.basics;

import java.util.Collection;

public interface VehicleRoutingSolution {
	
	public Collection<VehicleRoute> getRoutes();
	
	public double getTotalCost();

}
