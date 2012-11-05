package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public interface TourCost {
	
	public double getTourCost(TourImpl tour, Driver driver, Vehicle vehicle);

}
