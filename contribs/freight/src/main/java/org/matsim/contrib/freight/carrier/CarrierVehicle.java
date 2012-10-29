package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;

public class CarrierVehicle {

	private final Id location;

	private final Id vehicleId;

	private CarrierVehicleType vehicleType;

	private int capacity;

	private boolean active = true;

	private double earliestStartTime = 0.0;

	private double latestEndTime = 48*3600.0;

	public CarrierVehicle(final Id vehicleId, final Id location) {
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

	public boolean isActive() {
		return active;
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

	public void setActive(boolean active) {
		this.active = active;
	}

	public CarrierVehicleType getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(CarrierVehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	public void setEarliestStartTime(double startTime) {
		this.earliestStartTime = startTime;
	}

	public double getEarliestStartTime() {
		return earliestStartTime;
	}

	public double getLatestEndTime() {
		return latestEndTime;
	}

	public Id getVehicleTypeId() {
		return vehicleType.getId();
	}

}
