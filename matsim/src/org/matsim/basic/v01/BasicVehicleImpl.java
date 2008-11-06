package org.matsim.basic.v01;


public class BasicVehicleImpl implements BasicVehicle {

	private String type;
	private Id id;

	public BasicVehicleImpl(Id id, String type) {
		this.id = id;
		this.type = type;
	}

	public String getType() {
		return type;
	}
	
	public Id getId() {
		return id;
	}

	
}
