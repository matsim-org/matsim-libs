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
package playground.tnicolai.matsim4opus.utils.helperObjects;

/**
 * @author thomas
 *
 */
public class ZoneAccessibilityObject {
	
	private int zoneId;
	private double congestedTravelTimeAccessibility;
	private double freespeedTravelCostAccessibility;
	private double walkTravelTimeAccessibility;
	
	/**
	 * constructor
	 * @param zoneID
	 */
	public ZoneAccessibilityObject(int zoneID){
		this.zoneId 					= zoneID;
		this.congestedTravelTimeAccessibility 	= 0.;
		this.freespeedTravelCostAccessibility 	= 0.;
		this.walkTravelTimeAccessibility= 0.;
	}
	
	// setter methods
	public void setZoneID(int zoneId){
		this.zoneId = zoneId;
	}
	public void setCongestedTravelTimeAccessibility(double congestedTravelTimeAccessibility){
		this.congestedTravelTimeAccessibility = congestedTravelTimeAccessibility;
	}
	public void setFreespeedTravelTimeAccessibility(double freespeedTravelTimeAccessibility){
		this.freespeedTravelCostAccessibility = freespeedTravelTimeAccessibility;
	}
	public void setWalkTravelTimeAccessibility(double walkTravelTimeAccessibility){
		this.walkTravelTimeAccessibility = walkTravelTimeAccessibility;
	}

	// getter methods
	public int getZoneID(){
		return this.zoneId;
	}
	public double getCongestedTravelTimeAccessibility(){
		return this.congestedTravelTimeAccessibility;
	}
	public double getFreespeedTravelTimeAccessibility(){
		return this.freespeedTravelCostAccessibility;
	}
	public double getWalkTravelTimeAccessibility(){
		return this.walkTravelTimeAccessibility;
	}
}

