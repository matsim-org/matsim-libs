/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingTimes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.PSF2.pluggable.parkingTimes;

import org.matsim.api.core.v01.Id;

public class ParkingIntervalInfo {

	private double arrivalTime;
	private double departureTime;
	private Id linkId;
	
	public void setArrivalTime(double arrivalTime) {
		this.arrivalTime = arrivalTime;
	}

	public void setDepartureTime(double departureTime) {
		this.departureTime = departureTime;
	}
	
	public double getArrivalTime() {
		return arrivalTime;
	}
	
	public double getDepartureTime() {
		return departureTime;
	}

	public void setLinkId(Id linkId) {
		this.linkId = linkId;
	}

	public Id getLinkId() {
		return linkId;
	}
	
}
