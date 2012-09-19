package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.Job;

public interface ServiceProviderAgent extends TourAgent{
	
	public InsertionData calculateBestInsertion(Job job, double bestKnownPrice);
		
	public boolean removeJob(Job job);
	
	public void insertJob(Job job, InsertionData insertionData);

	public void updateTour();

}
