package org.matsim.contrib.freight.vrp.basics;

public class Service implements Job{
	
	private String id;
	
	private String locationId;
	
	private double serviceTime;

	public Service(String id, String locationId) {
		super();
		this.locationId = locationId;
		this.id = id;
	}

	public String getLocationId() {
		return locationId;
	}

	public double getServiceTime() {
		return serviceTime;
	}

	public void setServiceTime(double serviceTime) {
		this.serviceTime = serviceTime;
	}

	@Override
	public String getId() {
		return id;
	}
	
}
