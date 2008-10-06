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
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.network.Link;

import playground.christoph.router.util.KnowledgeTravelTime;

public class KnowledgeTravelTimeCalculator extends KnowledgeTravelTime {
	
	protected double tbuffer = 2.0;			// time distance ("safe distance") between two vehicles
	protected double vehicleLength = 7.5;	// length of a vehicle
	protected boolean calcFreeSpeedTravelTimes = false;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelTimeCalculator.class);
	
	public KnowledgeTravelTimeCalculator(QueueNetwork queueNetwork)
	{
		this.queueNetwork = queueNetwork;
	}
	
	public KnowledgeTravelTimeCalculator()
	{
		log.info("No QueueNetwork was commited - FreeSpeedTravelTimes will be calculated and returned!");
	}
	
	// return travel time without account for the actual traffic
	@Override
	public double getLinkTravelTime(Link link, double time)
	{
		if(queueNetwork == null)
		{
			log.info("No QueueNetwork found - FreeSpeedTravelTime is calculated and returned!");
			return link.getFreespeedTravelTime(time);
		}
			
		double vehicles = getVehiclesOnLink(link);
		return getLinkTravelTime(link, time, vehicles);
	}
	
	// calculate "real" travel time on the link and return it
	public double getLinkTravelTime(Link link, double time, double vehicles)
	{
		// if there are currently no vehicles on the link -> return the freespeed travel time
		if(vehicles == 0.0) return link.getFreespeedTravelTime(time);
		
		// normalize link to one lane
		vehicles = vehicles / link.getLanes(time);
		
		double length = link.getLength();
		
		double vmax = link.getFreespeed(time);
		
		// velocity of a vehicle on the link
		double v = (length/vehicles - vehicleLength)/tbuffer;
		
		// limit the velocity if neccessary
		if(v > vmax) v = vmax;
		
		double travelTime = length / v;
//		log.info("vehicles " + vehicles + " length " + length + " vmax " + vmax + " v " + v + " traveltime " + travelTime);

/*		
		if(java.lang.Math.abs(travelTime - link.getFreespeedTravelTime(time)) > 0.005 )
		{
			log.info("Calculating TravelTime! TravelTime is " + travelTime + ", FreeSpeedTravelTime would be " + link.getFreespeedTravelTime(time));
		}	
*/	
		// check results
		if(travelTime < link.getFreespeedTravelTime(time))
		{
			log.info("TravelTime is shorter than FreeSpeedTravelTime - looks like something is wrong here. Using FreeSpeedTravelTime instead!");
			return link.getFreespeedTravelTime(time);
		}
		
		return travelTime;
	}

	protected double getVehiclesOnLink(Link link)
	{
		QueueLink queueLink = queueNetwork.getQueueLink(link.getId());
		
		// maximum number of vehicles on the link
		double maxVehiclesOnLink = queueLink.getSpaceCap();

		// TODO verify if the right number of vehicles is used (with or without the buffer)
		// Return value: vehicle count / space capacity -> * space capacity
		double vehiclesOnLink = queueLink.getDisplayableSpaceCapValue() * maxVehiclesOnLink;
		
		return vehiclesOnLink;
	}
	
	
	public void setCalcFreeSpeedTravelTimes(boolean value)
	{
		this.calcFreeSpeedTravelTimes = value;
	}
	
	public boolean getCalcFreeSpeedTravelTimes()
	{
		return this.calcFreeSpeedTravelTimes;
	}
	
}
