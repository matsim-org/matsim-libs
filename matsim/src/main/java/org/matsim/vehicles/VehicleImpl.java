package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;


public class VehicleImpl implements Vehicle {

	private VehicleType type;
	private Id id;

	public VehicleImpl(Id id, VehicleType type) {
		this.id = id;
		this.type = type;
	}

	public Id getId() {
		return id;
	}

	public VehicleType getType() {
		return this.type;
	}

	
}
