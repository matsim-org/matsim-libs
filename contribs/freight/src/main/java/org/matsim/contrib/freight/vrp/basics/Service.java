package org.matsim.contrib.freight.vrp.basics;

public class Service implements Job {

	private final String id;

	private final String locationId;

	private final double serviceTime;

	private final double earliestServiceTime;

	private final double latestServiceTime;

	private final int demand;

	public Service(String id, String locationId, int demand,
			double serviceTime, double earliestServiceTime,
			double latestServiceTime) {
		super();
		this.id = id;
		this.locationId = locationId;
		this.demand = demand;
		this.serviceTime = serviceTime;
		this.earliestServiceTime = earliestServiceTime;
		this.latestServiceTime = latestServiceTime;
	}

	@Override
	public String getId() {
		return id;
	}

	public String getLocationId() {
		return locationId;
	}

	public double getServiceTime() {
		return serviceTime;
	}

	public double getEarliestServiceTime() {
		return earliestServiceTime;
	}

	public double getLatestServiceTime() {
		return latestServiceTime;
	}

	@Override
	public int getCapacityDemand() {
		return demand;
	}

}
