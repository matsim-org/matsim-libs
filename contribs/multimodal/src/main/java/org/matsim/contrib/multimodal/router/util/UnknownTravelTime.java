/* *********************************************************************** *
 * project: org.matsim.*
 * UnknownTravelTime.java
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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * Travel time calculator for unknown modes. Agents move with constant speed. No
 * agent specific parameters are taken into account.
 */
public class UnknownTravelTime implements TravelTime {
	
	private final boolean speed;
	private final boolean speedFactor;
	private final double value;
		
	public UnknownTravelTime(String mode, PlansCalcRouteConfigGroup plansCalcRouteConfigGroup) {
		
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
	
	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		if (speed) return link.getLength() / this.value;			
		else if (speedFactor) return (link.getLength() / link.getFreespeed()) * this.value;
		else throw new RuntimeException("Neither speed nor speed factor was found! Aborting.");
	}
}
