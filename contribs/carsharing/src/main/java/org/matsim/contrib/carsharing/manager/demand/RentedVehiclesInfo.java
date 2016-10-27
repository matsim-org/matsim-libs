package org.matsim.contrib.carsharing.manager.demand;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;
/** 
 * @author balac
 */
public class RentedVehiclesInfo {
	
	private String carsharingType;
	private CSVehicle vehicle;
	private Id<Link> locationOfVehicle;
	
	
	public String getCarsharingType() {
		return carsharingType;
	}
	public void setCarsharingType(String carsharingType) {
		this.carsharingType = carsharingType;
	}
	public CSVehicle getVehicle() {
		return vehicle;
	}
	public void setVehicle(CSVehicle vehicle) {
		this.vehicle = vehicle;
	}
	public Id<Link> getLocationOfVehicle() {
		return locationOfVehicle;
	}
	public void setLocationOfVehicle(Id<Link> locationOfVehicle) {
		this.locationOfVehicle = locationOfVehicle;
	}

}
