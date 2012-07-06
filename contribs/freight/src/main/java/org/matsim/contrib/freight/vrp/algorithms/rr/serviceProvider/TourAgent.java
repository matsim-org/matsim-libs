package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.Driver;
import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public interface TourAgent {
	
	public String getId();
	
	public Tour getTour();
	
	public Vehicle getVehicle();
	
	public Driver getDriver();
	
	public boolean isActive();
	
	public double getTourCost();

}
