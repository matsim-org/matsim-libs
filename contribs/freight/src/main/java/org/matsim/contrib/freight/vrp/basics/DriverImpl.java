package org.matsim.contrib.freight.vrp.basics;

public class DriverImpl implements Driver {

	private String id;

	private double earliestStart = 0.0;

	private double latestEnd = Double.MAX_VALUE;

	private String home;

	public DriverImpl(String id) {
		super();
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public double getEarliestStart() {
		return earliestStart;
	}

	public void setEarliestStart(double earliestStart) {
		this.earliestStart = earliestStart;
	}

	public double getLatestEnd() {
		return latestEnd;
	}

	public void setLatestEnd(double latestEnd) {
		this.latestEnd = latestEnd;
	}

	public void setHomeLocation(String locationId) {
		this.home = locationId;
	}

	public String getHomeLocation() {
		return this.home;
	}

}
