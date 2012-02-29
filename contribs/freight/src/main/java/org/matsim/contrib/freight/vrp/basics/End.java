package org.matsim.contrib.freight.vrp.basics;


public class End implements TourActivity{
	
	private String locationId;
	
	private double practical_earliestArrivalTime;

	private double practical_latestArrivalTime;

	private int currentLoad;
	
	public End(String locationId) {
		super();
		this.locationId = locationId;
	}

	@Override
	public String getType() {
		return "End";
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
		return 0.0;
	}

	@Override
	public int getCurrentLoad() {
		return this.currentLoad;
	}

	@Override
	public void setCurrentLoad(int load) {
		this.currentLoad = load;
		
	}
}
