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
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;

public class MultiModalTravelTimeCost implements MultiModalTravelTime, PersonalizableTravelCost {

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
		else if (transportMode.equals(TransportMode.ride)) ; // nothing to do here
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
		return calculateAgeScaleFactor() * link.getLength() / speed;
	}
	
	/*
	 * Scale the speed of walk/bike legs depending on the age
	 * of an Agent.
	 * 
	 * We use for:
	 * 		0-6 years: 		0.75	(we assume that babies are carried by their parents)
	 * 		6-10 years: 	0.85
	 * 		10-15 years:	0.95
	 * 		15-50 years:	1.00
	 * 		50-55 years:	0.95
	 * 		55-60 years:	0.90
	 * 		60-65 years:	0.85
	 * 		65-70 years:	0.80
	 * 		70-75 years:	0.75
	 * 		75-80 years:	0.70
	 * 		80-85 years:	0.65
	 * 		85-90 years:	0.60
	 * 		90-95 years:	0.55
	 * 		95+ years:		0.50
	 */
	private double calculateAgeScaleFactor() {
		if (person != null && person instanceof PersonImpl) {
			int age = ((PersonImpl)person).getAge();
			
			if (age <= 6) return 0.75;
			else if (age <= 10) return 0.85;
			else if (age <= 15) return 0.95;
			else if (age <= 50) return 1.00;
			else if (age <= 55) return 0.95;
			else if (age <= 60) return 0.90;
			else if (age <= 65) return 0.85;
			else if (age <= 70) return 0.80;
			else if (age <= 75) return 0.75;
			else if (age <= 80) return 0.70;
			else if (age <= 85) return 0.65;
			else if (age <= 90) return 0.60;
			else if (age <= 95) return 0.55;
			else return 0.50;
		}
		else return 1.0;
	}
	
	@Override
	public double getLinkTravelCost(Link link, double time) {
		return getLinkTravelTime(link, time);
	}

	public double getModalLinkTravelTime(Link link, double time, String transportMode) {
		double speed = 1.0;
		
		if (transportMode.equals(TransportMode.bike)) {
			/*
			 * If the link allows bike trips, we use bike speed. 
			 * Otherwise we check whether walk trips are allowed and return walk speed.
			 * If neither bike nor walk is allowed on the link, the default speed of 1.0
			 * is used.
			 */
			if (link.getAllowedModes().contains(TransportMode.bike)) speed = group.getBikeSpeed();
			else if (link.getAllowedModes().contains(TransportMode.walk)) speed = group.getWalkSpeed();
		}
		else if (transportMode.equals(TransportMode.walk)) {
			/*
			 * If the link allows walk or bike trips, we use walk speed. 
			 * If both modes are not allowed, use the default speed of 1.0 instead.
			 */
			if (link.getAllowedModes().contains(TransportMode.walk) || 
				link.getAllowedModes().contains(TransportMode.bike)) speed = group.getWalkSpeed();
		}
		else if (transportMode.equals(TransportMode.pt)) {
			/*
			 * If it is a car link, we use car travel times. Else we check whether it is
			 * a bike / walk link - if it is one, we use walk travel times.
			 */
			if (link.getAllowedModes().contains(TransportMode.car)) return travelTime.getLinkTravelTime(link, time);
			else if (link.getAllowedModes().contains(TransportMode.bike) ||
					link.getAllowedModes().contains(TransportMode.walk)) speed = group.getWalkSpeed();
		}
		else if (transportMode.equals(TransportMode.ride)) {
			/*
			 * If it is a car link, we use car travel times. Else we check whether it is
			 * a bike / walk link - if it is one, we use walk travel times.
			 */
			if (link.getAllowedModes().contains(TransportMode.car)) return travelTime.getLinkTravelTime(link, time);
			else if (link.getAllowedModes().contains(TransportMode.bike) ||
					link.getAllowedModes().contains(TransportMode.walk)) speed = group.getWalkSpeed();
		}
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
