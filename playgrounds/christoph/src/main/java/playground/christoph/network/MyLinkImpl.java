/* *********************************************************************** *
 * project: org.matsim.*
 * MyLinkImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.christoph.network;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;

/*
 *  This extended Version of a LinkImpl contains some
 *  additional Information like the VehicleCount, the
 *  TravelTimes and TravelCosts.
 *  
 *  A Time/CostCalculator can use this Data for its
 *  calculations without the need of searching trough
 *  Maps over and over again.
 */
public class MyLinkImpl extends LinkImpl{

	protected int vehiclesCount;
	protected double travelTime;
	protected double travelCost;
	
	// for TravelTimeEstimator
	private float[] linkVehicleCounts;
	private int[] linkEnterCounts;
	private int[] linkLeaveCounts;
	
	public MyLinkImpl(Id id, Node from, Node to, NetworkLayer network, double length, double freespeed, double capacity, double lanes)
	{
		super(id, from, to, network, length, freespeed, capacity, lanes);
	}
	
	public int getVehiclesCount() {
		return vehiclesCount;
	}

	public void setVehiclesCount(int vehiclesCount) {
		this.vehiclesCount = vehiclesCount;
	}

	public double getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	public double getTravelCost() {
		return travelCost;
	}

	public void setTravelCost(double travelCost) {
		this.travelCost = travelCost;
	}

	public float[] getLinkVehicleCounts() {
		return linkVehicleCounts;
	}

	public void setLinkVehicleCounts(float[] linkVehicleCounts) {
		this.linkVehicleCounts = linkVehicleCounts;
	}

	public int[] getLinkEnterCounts() {
		return linkEnterCounts;
	}

	public void setLinkEnterCounts(int[] linkEnterCounts) {
		this.linkEnterCounts = linkEnterCounts;
	}

	public int[] getLinkLeaveCounts() {
		return linkLeaveCounts;
	}

	public void setLinkLeaveCounts(int[] linkLeaveCounts) {
		this.linkLeaveCounts = linkLeaveCounts;
	}
}
