package org.matsim.contrib.freight.vrp.algorithms.rr.ruin;

import org.matsim.contrib.freight.vrp.basics.Job;

public interface JobDistance {
	
	public double calculateDistance(Job i, Job j);

}
