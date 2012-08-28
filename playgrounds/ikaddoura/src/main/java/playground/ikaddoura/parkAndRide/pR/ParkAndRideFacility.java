/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideFacility.java
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
package playground.ikaddoura.parkAndRide.pR;

import org.matsim.api.core.v01.Id;

/**
 * @author Ihab
 *
 */
public class ParkAndRideFacility {

	private Id id;
	private String stopFacilityName;
	private Id prLink1in; 
	private Id prLink1out;
	private Id prLink2in; // SignalizeableItem
	private Id prLink2out;
	private Id prLink3in; // parkAndRideActivity
	private Id prLink3out;
	private int capacity;

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	public Id getPrLink1in() {
		return prLink1in;
	}

	public void setPrLink1in(Id prLink1in) {
		this.prLink1in = prLink1in;
	}

	public Id getPrLink1out() {
		return prLink1out;
	}

	public void setPrLink1out(Id prLink1out) {
		this.prLink1out = prLink1out;
	}

	public Id getPrLink2in() {
		return prLink2in;
	}

	public void setPrLink2in(Id prLink2in) {
		this.prLink2in = prLink2in;
	}

	public Id getPrLink2out() {
		return prLink2out;
	}

	public void setPrLink2out(Id prLink2out) {
		this.prLink2out = prLink2out;
	}

	public Id getPrLink3in() {
		return prLink3in;
	}

	public void setPrLink3in(Id prLink3in) {
		this.prLink3in = prLink3in;
	}

	public Id getPrLink3out() {
		return prLink3out;
	}

	public void setPrLink3out(Id prLink3out) {
		this.prLink3out = prLink3out;
	}

	public void setStopFacilityName(String stopFacilityName) {
		this.stopFacilityName = stopFacilityName;
	}

	public String getStopFacilityName() {
		return stopFacilityName;
	}

	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}

	public int getCapacity() {
		return capacity;
	}
	
}
