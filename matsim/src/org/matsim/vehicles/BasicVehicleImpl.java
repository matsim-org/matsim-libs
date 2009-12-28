package org.matsim.vehicles;

import org.matsim.api.core.v01.Id;


public class BasicVehicleImpl implements BasicVehicle {

	private BasicVehicleType type;
	private Id id;

	public BasicVehicleImpl(Id id, BasicVehicleType type) {
		this.id = id;
		this.type = type;
	}

	public Id getId() {
		return id;
	}

	public BasicVehicleType getType() {
		return this.type;
	}

	
}
