package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Costs;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public interface ServiceProviderFactory {
	
	public ServiceProviderAgent createAgent(Tour tour, Vehicle vehicle, Costs costs);

}
