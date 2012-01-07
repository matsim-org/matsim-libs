package org.matsim.contrib.freight.vrp.basics;


public interface TourActivity {
	
	public abstract String getLocationId();

	public abstract void setEarliestArrTime(double early);

	public abstract double getEarliestArrTime();

	public abstract double getLatestArrTime();

	public abstract void setLatestArrTime(double late);

	public abstract String toString();
	
	public double getServiceTime();

	public abstract String getType();

}