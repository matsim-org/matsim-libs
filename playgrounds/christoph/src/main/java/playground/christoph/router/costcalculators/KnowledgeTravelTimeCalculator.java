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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.ptproject.qsim.interfaces.NetsimNetwork;
import org.matsim.ptproject.qsim.qnetsimengine.QNetwork;

import playground.christoph.network.MyLinkImpl;
import playground.christoph.network.SubLink;

public class KnowledgeTravelTimeCalculator implements PersonalizableTravelTime {
	
	//public static double tbuffer = 5.0;	// only for the batch runs
	protected double tbuffer = 35.0;		// time distance ("safety distance") between two vehicles
	protected double vehicleLength = 7.5;	// length of a vehicle
	protected boolean calcFreeSpeedTravelTimes = false;
	protected NetsimNetwork qNetwork;
	
	private static final Logger log = Logger.getLogger(KnowledgeTravelTimeCalculator.class);
	
	public KnowledgeTravelTimeCalculator(NetsimNetwork qNetwork) {
		if (qNetwork == null) log.warn("No QNetwork was commited - FreeSpeedTravelTimes will be calculated and returned!");
		this.qNetwork = qNetwork;
	}
	
	// return travel time without account for the actual traffic load
	public double getLinkTravelTime(Link link, double time) {
		if(qNetwork == null) {
			log.warn("No QueueNetwork found - FreeSpeedTravelTime is calculated and returned!");
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
		
		// We want that vehicles have at least a minimal speed
		if (v == 0.0) v = 0.1;
		
		// limit the velocity if neccessary
		if(v > vmax) v = vmax;
		
		double travelTime;
		
		if (v > 0.0) travelTime = length / v;
		else travelTime = Double.MAX_VALUE;
		
		// check results
		double freespeedTravelTime = link.getLength()/link.getFreespeed(time);
		if(travelTime < freespeedTravelTime) {
			log.warn("TravelTime is shorter than FreeSpeedTravelTime - looks like something is wrong here. Using FreeSpeedTravelTime instead!");
			return freespeedTravelTime;
		}
		
		return travelTime;
	}

	protected int getVehiclesOnLink(Link link) {	
		// now we have MyLinkImpls that have a VehiclesCount variable :)
		int vehicles;
		
		// Do we use SubNetworks?
		if (link instanceof SubLink) {
			Link parentLink = ((SubLink)link).getParentLink();
			vehicles = ((MyLinkImpl)parentLink).getVehiclesCount();
		}
		else {
			vehicles = ((MyLinkImpl)link).getVehiclesCount();
		}
		
		return vehicles;
	}
	
	public void setCalcFreeSpeedTravelTimes(boolean value) {
		this.calcFreeSpeedTravelTimes = value;
	}
	
	public boolean getCalcFreeSpeedTravelTimes() {
		return this.calcFreeSpeedTravelTimes;
	}

	@Override
	public void setPerson(Person person) {
		// TODO Auto-generated method stub
	}
}
