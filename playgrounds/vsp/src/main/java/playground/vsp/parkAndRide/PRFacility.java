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
package playground.vsp.parkAndRide;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * The physics of a park-and-ride facility. The actual park-and-ride activity is performed on the link prLink3in.
 * 
 * @author ikaddoura
 *
 */
public class PRFacility {

	private Id<PRFacility> id;
	private String stopFacilityName;
	private Id<Link> prLink1in; 
	private Id<Link> prLink1out;
	private Id<Link> prLink2in; // SignalizeableItem
	private Id<Link> prLink2out;
	private Id<Link> prLink3in; // parkAndRideActivity
	private Id<Link> prLink3out;
	private int capacity;

	public Id<PRFacility> getId() {
		return id;
	}

	public void setId(Id<PRFacility> id) {
		this.id = id;
	}

	public Id<Link> getPrLink1in() {
		return prLink1in;
	}

	public void setPrLink1in(Id<Link> prLink1in) {
		this.prLink1in = prLink1in;
	}

	public Id<Link> getPrLink1out() {
		return prLink1out;
	}

	public void setPrLink1out(Id<Link> prLink1out) {
		this.prLink1out = prLink1out;
	}

	public Id<Link> getPrLink2in() {
		return prLink2in;
	}

	public void setPrLink2in(Id<Link> prLink2in) {
		this.prLink2in = prLink2in;
	}

	public Id<Link> getPrLink2out() {
		return prLink2out;
	}

	public void setPrLink2out(Id<Link> prLink2out) {
		this.prLink2out = prLink2out;
	}

	public Id<Link> getPrLink3in() {
		return prLink3in;
	}

	public void setPrLink3in(Id<Link> prLink3in) {
		this.prLink3in = prLink3in;
	}

	public Id<Link> getPrLink3out() {
		return prLink3out;
	}

	public void setPrLink3out(Id<Link> prLink3out) {
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
