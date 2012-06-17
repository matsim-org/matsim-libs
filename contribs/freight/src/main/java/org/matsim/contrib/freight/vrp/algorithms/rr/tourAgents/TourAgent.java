package org.matsim.contrib.freight.vrp.algorithms.rr.tourAgents;

import org.matsim.contrib.freight.vrp.basics.Tour;
import org.matsim.contrib.freight.vrp.basics.Vehicle;

public interface TourAgent {
	
	public Tour getTour();
	
	public Vehicle getVehicle();
	
	public boolean isActive();
	
	public double getTourCost();

}
