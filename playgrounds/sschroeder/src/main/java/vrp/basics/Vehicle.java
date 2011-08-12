package vrp.basics;

import org.matsim.api.core.v01.Id;

/**
 * 
 * @author stefan schroeder
 *
 */

public class Vehicle{
	private int capacity;
	
	private Id id;
	
	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	private Id locationId;

	public Id getLocationId() {
		return locationId;
	}

	public void setLocationId(Id locationId) {
		this.locationId = locationId;
	}

	public Vehicle(int capacity) {
		super();
		this.capacity = capacity;
	}

	public Vehicle(VehicleType type) {
		this.capacity = type.capacity;
	}

	public int getCapacity() {
		return capacity;
	}
}