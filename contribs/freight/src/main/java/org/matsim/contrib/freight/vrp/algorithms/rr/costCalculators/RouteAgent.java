package org.matsim.contrib.freight.vrp.algorithms.rr.costCalculators;

import org.matsim.contrib.freight.vrp.basics.InsertionData;
import org.matsim.contrib.freight.vrp.basics.Job;

public interface RouteAgent {
	
	public InsertionData calculateBestInsertion(Job job, double bestKnownPrice);
		
	public boolean removeJobWithoutTourUpdate(Job job);
	
	public boolean removeJob(Job job);
	
	public void insertJobWithoutTourUpdate(Job job, InsertionData insertionData);
	
	public void insertJob(Job job, InsertionData insertionData);
	
	public void updateTour();


}
