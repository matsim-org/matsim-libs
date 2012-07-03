package org.matsim.contrib.freight.vrp.algorithms.rr.serviceProvider;

import org.matsim.contrib.freight.vrp.basics.Job;

public interface ServiceProviderAgent extends TourAgent{
	
	public String getId();
	
	public Offer requestService(Job job, double bestKnownPrice);
	
	public void offerRejected(Offer offer);
	
	public void offerGranted(Job job);
	
	public boolean removeJob(Job job);

}
