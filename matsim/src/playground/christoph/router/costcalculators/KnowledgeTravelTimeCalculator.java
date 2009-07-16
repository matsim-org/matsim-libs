/* *********************************************************************** *
 * project: org.matsim.*
 * KnowledgeTravelTimeCalculator.java
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

package playground.christoph.router.costcalculators;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.network.Link;

import playground.christoph.mobsim.MyQueueNetwork;
import playground.christoph.router.util.KnowledgeTravelTime;

public class KnowledgeTravelTimeCalculator extends KnowledgeTravelTime {
	
	//public static double tbuffer = 5.0;	// only for the batch runs
	protected double tbuffer = 20.0;			// time distance ("safe distance") between two vehicles
	protected double vehicleLength = 7.5;	// length of a vehicle
	protected boolean calcFreeSpeedTravelTimes = false;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelTimeCalculator.class);
	
	public KnowledgeTravelTimeCalculator(MyQueueNetwork myQueueNetwork)
	{
		this.myQueueNetwork = myQueueNetwork;
	}
	
	public KnowledgeTravelTimeCalculator()
	{
		log.info("No MyQueueNetwork was commited - FreeSpeedTravelTimes will be calculated and returned!");
	}
	
	// return travel time without account for the actual traffic
	public double getLinkTravelTime(Link link, double time)
	{
		if(myQueueNetwork == null)
		{
			log.info("No MyQueueNetwork found - FreeSpeedTravelTime is calculated and returned!");
			return link.getLength()/link.getFreespeed(time);
		}
			
		double vehicles = getVehiclesOnLink(link);
		return getLinkTravelTime(link, time, vehicles);
	}
	
	// calculate "real" travel time on the link and return it
	public double getLinkTravelTime(Link link, double time, double vehicles)
	{
		// if there are currently no vehicles on the link -> return the freespeed travel time
		if(vehicles == 0.0) {
			return link.getLength()/link.getFreespeed(time);
		}
		
		// normalize link to one lane
		vehicles = vehicles / link.getNumberOfLanes(time);
		
		double length = link.getLength();
		
		// At least one car can be on a link at a time.
		if (length < vehicleLength) length = vehicleLength;
		
		double vmax = link.getFreespeed(time);
		
		// velocity of a vehicle on the link
		double v = (length/vehicles - vehicleLength)/tbuffer;
		
		// Vehicles don't drive backwards.
		if (v < 0.0) v = 0.0;
		
		// limit the velocity if neccessary
		if(v > vmax) v = vmax;
		
		double travelTime;
		
		if (v > 0.0) travelTime = length / v;
		else travelTime = Double.MAX_VALUE;
		
//		log.info("vehicles " + vehicles + " length " + length + " vmax " + vmax + " v " + v + " traveltime " + travelTime);

/*		
		if(java.lang.Math.abs(travelTime - link.getFreespeedTravelTime(time)) > 0.005 )
		{
			log.info("Calculating TravelTime! TravelTime is " + travelTime + ", FreeSpeedTravelTime would be " + link.getFreespeedTravelTime(time));
		}	
*/	
		// check results
		double freespeedTravelTime = link.getLength()/link.getFreespeed(time);
		if(travelTime < freespeedTravelTime)
		{
			log.warn("TravelTime is shorter than FreeSpeedTravelTime - looks like something is wrong here. Using FreeSpeedTravelTime instead!");
			return freespeedTravelTime;
		}
		
		return travelTime;
	}

	protected double getVehiclesOnLink(Link link)
	{
//		QueueLink queueLink = myQueueNetwork.getQueueLink(link.getId());
				
		// maximum number of vehicles on the link
//		double maxVehiclesOnLink = queueLink.getSpaceCap();

		// TODO verify if the right number of vehicles is used (with or without the buffer)
		// Return value: vehicle count / space capacity -> * space capacity
//		double vehiclesOnLink = queueLink.getDisplayableSpaceCapValue() * maxVehiclesOnLink;
//		return vehiclesOnLink;
		
//		waitingList -> count (maybe)
//		parkingList -> count
//		vehQueue -> count
//		buffer -> count
		
		// number of vehicles that are on the link or that are already waiting to enter the link
		//double vehicles = queueLink.getAllVehicles().size() - queueLink.vehParkingCount();// - queueLink.getVehiclesOnParkingList().size();
		double vehicles = myQueueNetwork.getLinkVehiclesCounter().getLinkDrivingVehiclesCount(link.getId());
		
		return vehicles;
	}
	
	
	public void setCalcFreeSpeedTravelTimes(boolean value)
	{
		this.calcFreeSpeedTravelTimes = value;
	}
	
	public boolean getCalcFreeSpeedTravelTimes()
	{
		return this.calcFreeSpeedTravelTimes;
	}
	
	public KnowledgeTravelTimeCalculator clone()
	{

		KnowledgeTravelTimeCalculator clone = new KnowledgeTravelTimeCalculator(this.myQueueNetwork);

		clone.tbuffer = this.tbuffer;
		clone.vehicleLength = this.vehicleLength;
		clone.calcFreeSpeedTravelTimes = this.calcFreeSpeedTravelTimes;
		
		return clone;
	}
}
