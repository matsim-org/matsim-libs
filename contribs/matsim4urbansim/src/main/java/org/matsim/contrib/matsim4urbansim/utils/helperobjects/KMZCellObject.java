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
package org.matsim.contrib.matsim4urbansim.utils.helperobjects;

/**
 * @author thomas
 *
 */
public class KMZCellObject {
	
	private int cellID;
	private double carAccessibility;
	private double walkAccessibility;
	private double customAccessibility;
	
	/**
	 * constructor
	 * @param zoneID
	 */
	public KMZCellObject(int zoneID){
		this.cellID 			= zoneID;
		this.carAccessibility 	= 0.;
		this.walkAccessibility	= 0.;
		this.customAccessibility= 0.;
	}
	
	// setter methods
	public void setID(int zoneId){
		this.cellID = zoneId;
	}
	public void setCarAccessibility(double carAccessibility){
		this.carAccessibility = carAccessibility;
	}
	public void setWalkAccessibility(double walkAccessibility){
		this.walkAccessibility = walkAccessibility;
	}
	public void setCustomAccessibility(double customAccessibility){
		this.customAccessibility = customAccessibility;
	}

	// getter methods
	public int getID(){
		return this.cellID;
	}
	public double getCarAccessibility(){
		return this.carAccessibility;
	}
	public double getWalkAccessibility(){
		return this.walkAccessibility;
	}
	public double getCustomAccessibility(){
		return this.customAccessibility;
	}
}

