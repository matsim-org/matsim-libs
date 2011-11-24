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
package playground.benjamin.emissions.types;

/**
 * @author benjamin
 *
 */
public class HbefaAvgColdEmissionFactor {
	
	//TODO test: deleted final modifier on all variables

	/*private String vehCat ;
	private String component;
	private String parkingTime;
	private String distance;*/
	private double coldEF;

	public double getColdEF() {
		return coldEF;
	}

	public void setColdEF(double coldEF) {
		this.coldEF = coldEF;
	}
	
	public HbefaAvgColdEmissionFactor(
			String vehCat, 
			String component,
			String parkingTime,
			String distance, 
			double coldEF){
		this.coldEF = coldEF;
		}
	
	public HbefaAvgColdEmissionFactor(double coldEF){
		this.coldEF=coldEF;
	}
	
	public HbefaAvgColdEmissionFactor(){
		this.coldEF=0.0;
	}
	
	
	/*
//	public String getVehCat() {
//		return this.vehCat;
//	}
public void setVehCat(String vehCat) {
		this.vehCat = vehCat;
	}
//	public String getComponent() {
//		return this.component;
//	}
	public void setComponent(String component) {
		this.component = component;
	}
//	public String getParkingTime() {
//		return parkingTime;
//	}
	public void setParkingTime(String parkingTime) {
		this.parkingTime = parkingTime;
	}
//	public String getDistance() {
//		return distance;
//	}
	public void setDistance(String distance) {
		this.distance = distance;
	}
	public double getColdEF() {
		return coldEF;
	}
	public void setColdEF(double coldEF) {
		this.coldEF = coldEF;
	}

	public HbefaAvgColdEmissionFactor(){
		this.vehCat="";
		this.component="";
		this.distance="";
		this.parkingTime="";
		this.coldEF=0.0;
	}
	
	public HbefaAvgColdEmissionFactor(
			String vehCat, 
			String component,
			String parkingTime,
			String distance, 
			double coldEF){
		
		this.vehCat = vehCat;
		this.component = component;
		this.parkingTime = parkingTime;
		this.distance = distance;
		this.coldEF = coldEF;
		}

	public String getVehCat() {
		return vehCat;
	}

	public String getComponent() {
		return component;
	}

	public String getParkingTime() {
		return parkingTime;
	}

	public String getDistance() {
		return distance;
	}
*/
}