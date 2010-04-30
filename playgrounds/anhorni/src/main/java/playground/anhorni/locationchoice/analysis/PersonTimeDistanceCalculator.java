/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.anhorni.locationchoice.analysis;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PersonImpl;

/*
 * This is a helper class for TravelTimeandDistanceStats
 * TODO: move this functionality to Person.java
 */

public class PersonTimeDistanceCalculator {

	private static double planTravelTime;
//	private static double planTravelDistance;
	private static int numberOfLegs;


	private static void init() {
		planTravelTime=0.0;
//		planTravelDistance=0.0;
		numberOfLegs=0;
	}

	public static void run(final PersonImpl person){

		init();

		for (PlanElement pe : person.getSelectedPlan().getPlanElements()) {
			if (pe instanceof Leg) {
				final Leg leg = (Leg) pe;
				planTravelTime+=leg.getTravelTime();
//				planTravelDistance+=leg.getRoute().getDistance();
				numberOfLegs++;
			}
		}
	}

	public static double getPlanTravelTime() {
		return planTravelTime;
	}

//	public static double getPlanTravelDistance() {
//		return planTravelDistance;
//	}

	public static int getNumberOfLegs() {
		return numberOfLegs;
	}
}
