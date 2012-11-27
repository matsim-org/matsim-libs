package org.matsim.contrib.freight.vrp.basics;

import java.util.List;

public interface Tour {

	public List<TourActivity> getActivities();
	
	public int getLoad();
	
	public double getTotalCost();

}
