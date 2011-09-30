package playground.mzilske.freight.carrier;

import org.matsim.api.core.v01.Id;

public class CarrierVehicle {

	private Id vehicleId;
	
	private Id location;
	
	private int capacity;
	
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

	
	
}
