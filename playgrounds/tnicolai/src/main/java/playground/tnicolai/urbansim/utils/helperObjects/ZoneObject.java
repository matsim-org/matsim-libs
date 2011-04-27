/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneObject.java
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

/**
 * 
 */
package playground.tnicolai.urbansim.utils.helperObjects;

/**
 * @author thomas
 *
 */
public class ZoneObject {
	
	private int zoneId;
	private double travelTimeAccessibility;
	private double travelCostAccessibility;
	private double travelDistanceAccessibility;
	
	/**
	 * constructor
	 * @param zoneID
	 */
	public ZoneObject(int zoneID){
		this.zoneId 					= zoneID;
		this.travelTimeAccessibility 	= 0.;
		this.travelCostAccessibility 	= 0.;
		this.travelDistanceAccessibility = 0.;
	}
	
	// setter methods
	public void setZoneID(int zoneId){
		this.zoneId = zoneId;
	}
	public void setTravelTimeAccessibility(double travelTimeAccessibility){
		this.travelTimeAccessibility = travelTimeAccessibility;
	}
	public void setTravelCostAccessibility(double travelCostAccessibility){
		this.travelCostAccessibility = travelCostAccessibility;
	}
	public void setTravelDistanceAccessibility(double travelDistanceAccessibility){
		this.travelDistanceAccessibility = travelDistanceAccessibility;
	}

	// getter methods
	public int getZoneID(){
		return this.zoneId;
	}
	public double getTravelTimeAccessibility(){
		return this.travelTimeAccessibility;
	}
	public double getTravelCostAccessibility(){
		return this.travelCostAccessibility;
	}
	public double getTravelDistanceAccessibility(){
		return this.travelDistanceAccessibility;
	}
}

