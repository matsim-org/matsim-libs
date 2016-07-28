package org.matsim.contrib.carsharing.vehicles;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;

public class FFCSVehicle {
	
	private String type;
	private String vehicleId;
	private Link link;
	
	public FFCSVehicle(String type, String vehicleId, Link link) {
		
		this.setType(type);
		this.setVehicleId(vehicleId);
		this.setLink(link);
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

	public Link getLink() {
		return link;
	}

	public void setLink(Link link) {
		this.link = link;
	}

}
