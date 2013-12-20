/* *********************************************************************** *
 * project: org.matsim.*
 * EvacuationPTTravelTime.java
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

package playground.christoph.evacuation.trafficmonitoring;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * Travel time calculator e.g. for PT trips. If a person's speed is defined,
 * it is used to calculate the person's travel time on a link. Otherwise
 * a fixed speed or speed factor is used to calculate the travel time on a link.
 */
public class EvacuationPTTravelTime implements TravelTime {
	
	private final Map<Id, Double> personSpeeds;
	private final boolean speed;
	private final boolean speedFactor;
	private final double value;
		
	public EvacuationPTTravelTime(String mode, PlansCalcRouteConfigGroup plansCalcRouteConfigGroup) {
		this.personSpeeds = new ConcurrentHashMap<Id, Double>();
		
		Double speed = plansCalcRouteConfigGroup.getTeleportedModeSpeeds().get(mode);
		Double speedFactor = plansCalcRouteConfigGroup.getTeleportedModeFreespeedFactors().get(mode);
		
		if (speed != null && speedFactor != null) {
			throw new RuntimeException("Speed as well as speed factor was found for mode " + mode + 
					"!  Don't know which should be used. Aborting.");
		} else if (speed == null && speedFactor == null) {
			throw new RuntimeException("Neither speed nor speed factor was found for mode " + mode + "! Aborting.");
		} else if (speed != null) {
			this.value = speed;
			this.speed = true;
			this.speedFactor = false;
		} else {
			this.value = speedFactor;
			this.speed = false;
			this.speedFactor = true;
		}
	}
	
	public void setPersonSpeed(Id personId, double speed) {
		this.personSpeeds.put(personId, speed);
	}
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		
		Double personSpeed = this.personSpeeds.get(person.getId());
		if (personSpeed != null) return link.getLength() / personSpeed;
		else {
			if (speed) return link.getLength() / this.value;			
			else if (speedFactor) return (link.getLength() / link.getFreespeed()) * this.value;
			else throw new RuntimeException("Neither person speed nor mode speed nor mode speed factor was found! Aborting.");
		}
	}
}