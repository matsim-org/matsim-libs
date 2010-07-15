/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalTravelTimeCost.java
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

package playground.christoph.multimodal.router.costcalculator;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;

public class MultiModalTravelTimeCost implements MultiModalTravelTime, PersonalizableTravelCost{

	private TravelTime travelTime;
	private PlansCalcRouteConfigGroup group;
	private double speed = 1.0;
	private Person person;
	
	/*
	 * By default: use Walk as TransportMode.
	 */
	public MultiModalTravelTimeCost(PlansCalcRouteConfigGroup group) {
		this(group, TransportMode.walk);
	}
	
	public MultiModalTravelTimeCost(PlansCalcRouteConfigGroup group, String transportMode) {
		this.group = group;
		
		if (transportMode.equals(TransportMode.bike)) speed = group.getBikeSpeed();
		else if (transportMode.equals(TransportMode.walk)) speed = group.getWalkSpeed();
		else if (transportMode.equals(TransportMode.pt)) ;	// nothing to do here
		else throw new RuntimeException("Not supported TransportMode: " + transportMode);
		
		/*
		 * By default: us FreeSpeedTravelTimes.
		 */
		travelTime = new FreeSpeedTravelTime();
	}
	
	public void setTravelTime(TravelTime travelTime) {
		this.travelTime = travelTime;
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time) {
		return link.getLength() / speed;
	}
	
	@Override
	public double getLinkTravelCost(Link link, double time) {
		return getLinkTravelTime(link, time);
	}

	public double getModalLinkTravelTime(Link link, double time, String transportMode) {
		double speed = 1.0;
		
		if (transportMode.equals(TransportMode.bike)) speed = group.getBikeSpeed();
		else if (transportMode.equals(TransportMode.walk)) speed = group.getWalkSpeed();
		else if (transportMode.equals(TransportMode.pt)) return travelTime.getLinkTravelTime(link, time);
		else throw new RuntimeException("Not supported TransportMode: " + transportMode);
		
		return link.getLength() / speed;
	}

	/*
	 * E.g. one could adapt the walk speed of a person depending on the person's age.
	 */
	@Override
	public void setPerson(Person person) {
		this.person = person;
	}
	
	private class FreeSpeedTravelTime implements TravelTime {
		
		@Override
		public double getLinkTravelTime(Link link, double time) {
			return link.getLength() / link.getFreespeed(time);
		}
	}

}
