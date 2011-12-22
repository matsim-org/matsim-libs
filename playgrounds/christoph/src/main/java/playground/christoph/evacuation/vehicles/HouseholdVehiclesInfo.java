/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdVehiclesInfo.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package playground.christoph.evacuation.vehicles;

import org.apache.log4j.Logger;

public class HouseholdVehiclesInfo {

	private static final Logger log = Logger.getLogger(HouseholdVehiclesInfo.class);
	
	private String firstVehicleType = null;
	private String secondVehicleType = null;
	private String thirdVehicleType = null;
	
	private int firstVehicleCapacity = 0;
	private int secondVehicleCapacity = 0;
	private int thirdVehicleCapacity = 0;
	
	public HouseholdVehiclesInfo() {
	}

	public void setFirstVehicle(String type) {
		this.firstVehicleType = type;
		this.firstVehicleCapacity = getTypeCapacity(type);
	}
	
	public void setSecondVehicle(String type) {
		this.secondVehicleType = type;
		this.secondVehicleCapacity = getTypeCapacity(type);
	}
	
	public void setThirdVehicle(String type) {
		this.thirdVehicleType = type;
		this.thirdVehicleCapacity = getTypeCapacity(type);
	}
	
	public int getFirstCapacity() {
		return this.firstVehicleCapacity;
	}
	
	public int getSecondCapacity() {
		return this.secondVehicleCapacity;
	}
	
	public int getThirdCapacity() {
		return this.thirdVehicleCapacity;
	}
	
	public int getNumVehicles() {
		int num = 0;
		if (firstVehicleType != null) num++;
		if (secondVehicleType != null) num++;
		if (thirdVehicleType != null) num++;
		return num;
	}
	
	private int getTypeCapacity(String type) {
		if (type == null) return 0;
		else if (type.equals("B_00")) return 2;	// Micro 
		else if (type.equals("B_01")) return 4;	// Subcompact
		else if (type.equals("B_02")) return 5;	// Compact
		else if (type.equals("B_03")) return 5;	// Mini MVP
		else if (type.equals("B_04")) return 5;	// Mid-Sized
		else if (type.equals("B_05")) return 7;	// Mini-Van, Full-Sized
		else if (type.equals("B_06")) return 5;	// Luxurious
		else if (type.equals("B_07")) return 2;	// Sportscar
		else if (type.equals("D_00")) return 2;	// Micro
		else if (type.equals("D_01")) return 4;	// Subcomplact
		else if (type.equals("D_02")) return 5;	// Compact
		else if (type.equals("D_03")) return 5;	// Mini MVP
		else if (type.equals("D_04")) return 5;	// Mid-Sized
		else if (type.equals("D_05")) return 7;	// Mini-Van, Full-Sized
		else if (type.equals("D_06")) return 5;	// Luxurious, Sportscar
		else if (type.equals("U_00")) return 5;	// Gas, Hybrid, Electric - all types
		else {
			log.warn("Unknown vehicle type: " + type);
			return 0;
		}
	}
}
