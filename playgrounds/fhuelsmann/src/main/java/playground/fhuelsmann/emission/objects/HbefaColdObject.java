package playground.fhuelsmann.emission.objects;
/* *********************************************************************** *
 * project: org.matsim.*
 * FhMain.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */



//VehCat;Year;Component;ParkingTime [h];Distance [km];EFA_km_weighted


public class HbefaColdObject {

	private String VehCat ;
	private String Component;
	private String parkingTime;
	private String distance;
	private double coldEF;
	
	
	public String getVehCat() {
		return VehCat;
	}
	public void setVehCat(String vehCat) {
		VehCat = vehCat;
	}
	public String getComponent() {
		return Component;
	}
	public void setComponent(String component) {
		Component = component;
	}
	public String getParkingTime() {
		return parkingTime;
	}
	public void setParkingTime(String parkingTime) {
		this.parkingTime = parkingTime;
	}
	public String getDistance() {
		return distance;
	}
	public void setDistance(String distance) {
		this.distance = distance;
	}
	public double getColdEF() {
		return coldEF;
	}
	public void setColdEF(double coldEF) {
		this.coldEF = coldEF;
	}


	public HbefaColdObject(
			String vehCat, 
			String component,
			String parkingTime,
			String distance, 
			double coldEF){
		
		this.VehCat= vehCat;
		this.Component= component;
		this.parkingTime=parkingTime;
		this.distance= distance;
		this.coldEF= coldEF;
		}
}
