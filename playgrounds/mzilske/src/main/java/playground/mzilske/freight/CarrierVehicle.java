package playground.mzilske.freight;

import org.matsim.api.core.v01.Id;

public class CarrierVehicle {

	private Id vehicleId;
	
	private Id location;
	
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

	@Override
	public String toString() {
		return vehicleId + " stationed at " + location;
	}

	
	
}
