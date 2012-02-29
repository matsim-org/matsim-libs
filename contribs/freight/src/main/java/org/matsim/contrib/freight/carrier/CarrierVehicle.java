package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;

public class CarrierVehicle {

	private Id vehicleId;
	
	private Id location;
	
	private int capacity;
	
	private boolean active = true;
	
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	private double earliestStartTime;
	
	private double latestEndTime;
	
	public CarrierVehicle(Id vehicleId, Id location) {
		this.vehicleId = vehicleId;
		this.location = location;
	}

	public Id getLocation() {
		return location;
	}

	public Id getVehicleId() {
		return vehicleId;
	}
	
	public int getCapacity() {
		return capacity;
	}
	
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	@Override
	public String toString() {
		return vehicleId + " stationed at " + location;
	}

	public void setLatestEndTime(double endTime) {
		this.latestEndTime = endTime;
	}
	
	public void setEarliestStartTime(double startTime){
		this.earliestStartTime = startTime;
	}

	public double getEarliestStartTime() {
		return earliestStartTime;
	}

	public double getLatestEndTime() {
		return latestEndTime;
	}

	
	
}
