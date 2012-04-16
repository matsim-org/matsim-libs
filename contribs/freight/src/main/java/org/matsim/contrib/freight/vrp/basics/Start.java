package org.matsim.contrib.freight.vrp.basics;


public class Start implements TourActivity{

	private String locationId;
	
	private double practical_earliestArrivalTime;

	private double practical_latestArrivalTime;

	private int currentLoad;
	
	public Start(String locationId) {
		super();
		this.locationId = locationId;
	}

	@Override
	public String getType() {
		return "Start";
	}

	@Override
	public void setEarliestOperationStartTime(double early) {
		practical_earliestArrivalTime = early;	
	}

	@Override
	public double getEarliestOperationStartTime() {
		return practical_earliestArrivalTime;
	}

	@Override
	public double getLatestOperationStartTime() {
		return practical_latestArrivalTime;
	}

	@Override
	public void setLatestOperationStartTime(double late) {
		practical_latestArrivalTime = late;
	}

	@Override
	public String getLocationId() {
		return locationId;
	}

	@Override
	public double getOperationTime() {
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
