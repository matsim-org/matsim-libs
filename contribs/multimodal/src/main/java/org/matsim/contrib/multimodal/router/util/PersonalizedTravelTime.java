/* *********************************************************************** *
 * project: org.matsim.*
 * PersonalizedTravelTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.router.util;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Travel time calculator that uses a personalized speed for each person. 
 */
public class PersonalizedTravelTime implements TravelTime {
	
	private final Map<Id, Double> personSpeeds;
		
	public PersonalizedTravelTime() {
		personSpeeds = new ConcurrentHashMap<>();
	}
	
	public void setPersonSpeed(Id personId, double speed) {
		this.personSpeeds.put(personId, speed);
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		
		Double personSpeed = this.personSpeeds.get(person.getId());
		if (personSpeed == null) {
			throw new RuntimeException("No speed was found for person " + person.getId().toString() + ". Aborting!");
		} else return link.getLength() / personSpeed;
	}
}
