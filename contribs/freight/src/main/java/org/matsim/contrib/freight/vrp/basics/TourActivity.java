package org.matsim.contrib.freight.vrp.basics;


public interface TourActivity {
	
	public abstract String getLocationId();

	public abstract void setEarliestOperationStartTime(double early);

	public abstract double getEarliestOperationStartTime();

	public abstract double getLatestOperationStartTime();

	public abstract void setLatestOperationStartTime(double late);

	public abstract String toString();
	
	public double getOperationTime();

	public abstract String getType();
	
	public abstract int getCurrentLoad();
	
	public abstract void setCurrentLoad(int load);

}