package org.matsim.contrib.freight.vrp.basics;


public class Start implements TourActivity{

	private String locationId;
	
	private double practical_earliestArrivalTime;

	private double practical_latestArrivalTime;
	
	public Start(String locationId) {
		super();
		this.locationId = locationId;
	}

	@Override
	public String getType() {
		return "Start";
	}

	@Override
	public void setEarliestArrTime(double early) {
		practical_earliestArrivalTime = early;	
	}

	@Override
	public double getEarliestArrTime() {
		return practical_earliestArrivalTime;
	}

	@Override
	public double getLatestArrTime() {
		return practical_latestArrivalTime;
	}

	@Override
	public void setLatestArrTime(double late) {
		practical_latestArrivalTime = late;
	}

	@Override
	public String getLocationId() {
		return locationId;
	}

	@Override
	public double getServiceTime() {
		return 0;
	}
}
