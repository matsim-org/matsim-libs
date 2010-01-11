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

import java.util.LinkedList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.misc.Time;

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
	protected double travelTime = Double.NaN;
	protected double travelCost;
	
	// for TravelTimeEstimator
	private float[] linkVehicleCounts;
	private int[] linkEnterCounts;
	private int[] linkLeaveCounts;
	
	private LinkedList<TripInfo> tripInfos = new LinkedList<TripInfo>();
	private int storedTravelTimesWindow = 570;	// How long do we store TravelTimes?

	private double addedTravelTimes = 0.0;
	private double sumTravelTimes = 0.0;	// We cache the sum of the TravelTimes
	private double freeSpeedTravelTime = Double.MAX_VALUE;	// We cache the FreeSpeedTravelTimes
	
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
	
	/*
	 * Caches the freeSpeedTravel Time.
	 * We round the value to the next Integer because that is what happens
	 * within the QueueSimulation.
	 * The caching can't be done when the Object is intialized because
	 * at that time the FreeSpeed as well as the Link Length are not set
	 * properly.
	 */
	public void cacheFreeSpeedTravelTime()
	{
		this.freeSpeedTravelTime = Math.ceil(this.getFreespeedTravelTime(Time.UNDEFINED_TIME));
		
//		if (this.travelTime == Double.NaN) this.travelTime = this.freeSpeedTravelTime;
	}
	
	public void updateMeanTravelTime(double time)
	{
		double removedTravelTimes = 0.0;
				
		// first remove old TravelTimes
//		Iterator<TripInfo> iter = this.tripInfos.iterator();
//		while (iter.hasNext())
//		{
//			TripInfo tripInfo = iter.next();
//			if (tripInfo.leaveTime + this.storedTravelTimesWindow < time)
//			{
//				removedTravelTimes = removedTravelTimes + tripInfo.travelTime;
//				iter.remove();
//			}
//			else break;
//		}
		
		//Is this faster?
		TripInfo tripInfo;
		while((tripInfo = this.tripInfos.peek()) != null)
		{
			if (tripInfo.leaveTime + this.storedTravelTimesWindow < time)
			{
				removedTravelTimes = removedTravelTimes + tripInfo.travelTime;
				this.tripInfos.poll();
			}
			else break;
		}
		
		/*
		 * We don't need an update if no Trips have been added or 
		 * removed within the current SimStep.
		 * The initial FreeSpeedTravelTime has to be set correctly via
		 * setTravelTime!
		 */
		if (removedTravelTimes == 0.0 && this.addedTravelTimes == 0.0) return;
		
		sumTravelTimes = sumTravelTimes - removedTravelTimes + this.addedTravelTimes;
		
		this.addedTravelTimes = 0.0;
		
		/* 
		 * Ensure, that we don't allow TravelTimes shorter than the
		 * FreeSpeedTravelTime.
		 */
		double meanTravelTime = freeSpeedTravelTime;
		if (this.tripInfos.size() > 0) meanTravelTime = sumTravelTimes / this.tripInfos.size();
		
		if (meanTravelTime < freeSpeedTravelTime)
		{
			System.out.println("Warning: Mean TravelTime to short?");
			this.setTravelTime(freeSpeedTravelTime);
		}
		else this.setTravelTime(meanTravelTime);
	}
	
	public void addTravelTime(double travelTime, double time)
	{
		TripInfo tripInfo = new TripInfo();
		tripInfo.travelTime = travelTime;
		tripInfo.leaveTime = time;
		this.tripInfos.add(tripInfo);
		
		this.addedTravelTimes = this.addedTravelTimes + travelTime;
	}
	
	private class TripInfo
	{
		double travelTime;
		double leaveTime;
	}
}