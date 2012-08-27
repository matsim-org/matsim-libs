package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.TourImpl;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public interface TourAgent {
	
	public String getId();
	
	public TourImpl getTour();
	
	public Vehicle getVehicle();
	
	public Driver getDriver();
	
	public boolean isActive();
	
	public double getTourCost();

}
