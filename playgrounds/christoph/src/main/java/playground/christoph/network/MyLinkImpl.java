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
import org.matsim.core.network.NetworkImpl;

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

	private LinkedList<TripInfo> tripInfos = new LinkedList<TripInfo>();

	private boolean fadingTravelTimes = false;
	private boolean binTravelTimes = true;

	private double fadingFactor = 1.0015;	// How much does the weight of a previous TravelTime decrease per SimStep?
//	private double fadingFactor = 1.0025;	// How much does the weight of a previous TravelTime decrease per SimStep?
	private double storedTravelTimes = 0.0;
	private int addedTrips = 0;

	private double storedTravelTimesBinSize = 600;

	private double addedTravelTimes = 0.0;
	private double sumTravelTimes = 0.0;	// We cache the sum of the TravelTimes
	private double freeSpeedTravelTime = Double.MAX_VALUE;	// We cache the FreeSpeedTravelTimes

	public MyLinkImpl(Id id, Node from, Node to, NetworkImpl network, double length, double freespeed, double capacity, double lanes)
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
		this.freeSpeedTravelTime = Math.ceil(this.getFreespeedTravelTime());

//		if (this.travelTime == Double.NaN) this.travelTime = this.freeSpeedTravelTime;
	}

	public void updateMeanTravelTime(double time)
	{
		if (fadingTravelTimes)
		{
			calcFadingMeanTravelTimes(time);
		}
		else if (binTravelTimes)
		{
			calcBinTravelTime(time);
		}
	}

	private void calcFadingMeanTravelTimes(double time)
	{
		this.storedTravelTimes = this.storedTravelTimes / this.fadingFactor;
		this.sumTravelTimes = this.sumTravelTimes / this.fadingFactor;

		/*
		 * We don't need an update if no Trips have been added.
		 */
		if (this.addedTrips == 0) return;

		this.sumTravelTimes = this.sumTravelTimes + this.addedTravelTimes;
		this.storedTravelTimes = this.storedTravelTimes + this.addedTrips;

		this.addedTravelTimes = 0.0;
		this.addedTrips = 0;

		/*
		 * Ensure, that we don't allow TravelTimes shorter than the
		 * FreeSpeedTravelTime.
		 */
		double meanTravelTime = freeSpeedTravelTime;
//		if (this.tripInfos.size() > 0) meanTravelTime = sumTravelTimes / this.storedTravelTimes;
		if (this.storedTravelTimes > 0) meanTravelTime = sumTravelTimes / this.storedTravelTimes;
		
		if (meanTravelTime * this.fadingFactor < freeSpeedTravelTime)
		{
			System.out.println("Warning: Mean TravelTime to short?");
			this.setTravelTime(freeSpeedTravelTime);
		}
		else this.setTravelTime(meanTravelTime);
	}

	private void calcBinTravelTime(double time)
	{
		double removedTravelTimes = 0.0;

		// first remove old TravelTimes
		TripInfo tripInfo;
		while((tripInfo = this.tripInfos.peek()) != null)
		{
			if (tripInfo.leaveTime + this.storedTravelTimesBinSize < time)
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

		this.sumTravelTimes = this.sumTravelTimes - removedTravelTimes + this.addedTravelTimes;

		this.addedTravelTimes = 0.0;

		/*
		 * Ensure, that we don't allow TravelTimes shorter than the
		 * FreeSpeedTravelTime.
		 */
		double meanTravelTime = freeSpeedTravelTime;
		if (!tripInfos.isEmpty()) meanTravelTime = sumTravelTimes / this.tripInfos.size();

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
		this.addedTrips++;
	}

	private class TripInfo
	{
		double travelTime;
		double leaveTime;
	}
}