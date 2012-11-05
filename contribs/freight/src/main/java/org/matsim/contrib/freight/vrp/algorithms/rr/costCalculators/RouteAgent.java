package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import org.matsim.contrib.freight.vrp.basics.InsertionData;
import org.matsim.contrib.freight.vrp.basics.Job;
import org.matsim.contrib.freight.vrp.basics.VehicleRoute;

public interface RouteAgent {
	
	public InsertionData calculateBestInsertion(Job job, double bestKnownPrice);
		
	public boolean removeJob(Job job);
	
	public void insertJob(Job job, InsertionData insertionData);

	public void updateTour();
	
	public VehicleRoute getRoute();
	
	public double getCost();

}
