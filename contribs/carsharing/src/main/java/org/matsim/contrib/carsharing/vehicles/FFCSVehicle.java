package org.matsim.contrib.carsharing.vehicles;

public class FFCSVehicle {
	
	private String type;
	private String vehicleId;
	
	public FFCSVehicle(String type, String vehicleId) {
		
		this.setType(type);
		this.setVehicleId(vehicleId);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	

}
