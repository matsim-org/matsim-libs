/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.intervalBasedCongestionPricing.data;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

/**
 * 
 * @author ikaddoura
 */

public class CongestionLinkInfo {
	
	private final Id<Link> linkId;
	private final Set<Id<Vehicle>> leavingVehicles = new HashSet<>();
	
	private double travelTimeSum = 0.;
	private double travelTimeLastLeavingAgent = 0.;
	private double travelTimeMaximum = 0.;

	public CongestionLinkInfo(Id<Link> linkId) {
		this.linkId = linkId;
	}

	public Id<Link> getLinkId() {
		return linkId;
	}

	public Set<Id<Vehicle>> getLeavingVehicles() {
		return leavingVehicles;
	}

	public double getTravelTimeSum_sec() {
		return travelTimeSum;
	}

	public void setTravelTimeSum(double travelTimeSum) {
		this.travelTimeSum = travelTimeSum;
	}

	public double getTravelTimeLastLeavingAgent_sec() {
		return travelTimeLastLeavingAgent;
	}

	public void setTravelTimeLastLeavingAgent(double travelTimeLastLeavingAgent) {
		this.travelTimeLastLeavingAgent = travelTimeLastLeavingAgent;
	}

	public double getTravelTimeMaximum() {
		return travelTimeMaximum;
	}

	public void setTravelTimeMaximum(double travelTimeMaximum) {
		this.travelTimeMaximum = travelTimeMaximum;
	}

}

