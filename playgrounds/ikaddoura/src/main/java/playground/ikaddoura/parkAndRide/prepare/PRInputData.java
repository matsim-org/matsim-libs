/* *********************************************************************** *
 * project: org.matsim.*
 * PRCarLinkToNode.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.ikaddoura.parkAndRide.prepare;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;

/**
 * @author Ihab
 *
 */
public class PRInputData {
	
	private Id<ParkAndRideFacility> id;
	private String stopName;
	private int capacity;
	private Coord coord;
	
	public Id<ParkAndRideFacility> getId() {
		return id;
	}
	public void setId(Id<ParkAndRideFacility> id) {
		this.id = id;
	}
	public String getStopName() {
		return stopName;
	}
	public void setStopName(String stopName) {
		this.stopName = stopName;
	}
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	public int getCapacity() {
		return capacity;
	}
	public void setCoord(Coord coord) {
		this.coord = coord;
	}
	public Coord getCoord() {
		return coord;
	}

}
