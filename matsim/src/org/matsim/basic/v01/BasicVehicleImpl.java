package org.matsim.basic.v01;


public class BasicVehicleImpl implements BasicVehicle {

	private String typeId;
	private Id id;

	public BasicVehicleImpl(Id id, String type) {
		this.id = id;
		this.typeId = type;
	}

	public String getTypeId() {
		return typeId;
	}
	
	public Id getId() {
		return id;
	}

	
}
