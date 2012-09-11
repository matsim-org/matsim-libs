/* *********************************************************************** *
 * project: org.matsim.*
 * WalkTravelTime.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.multimodalsimengine.router.util;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * This is just a first implementation. It will be replaced with a 
 * more accurate implementation in the near future!
 * 
 * @author cdobler
 */
public class WalkTravelTime implements PersonalizableTravelTime {

	private final double walkSpeed;
	
	private double ageScaleFactor;
	
	public WalkTravelTime(PlansCalcRouteConfigGroup plansCalcRouteConfigGroup) {
		this.walkSpeed = plansCalcRouteConfigGroup.getWalkSpeed();
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		this.ageScaleFactor = calculateAgeScaleFactor(person);
		return link.getLength() / (walkSpeed * ageScaleFactor);
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
	 * <p/>
	 * yy Is there a reference for these values somewhere?  kai, jun'11
	 */
	private double calculateAgeScaleFactor(Person person) {
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

}