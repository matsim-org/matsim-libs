/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.agentSpecificActivityScheduling;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scoring.functions.OpeningIntervalCalculator;

/**
* @author ikaddoura
*/

public class AgentSpecificOpeningIntervalCalculator implements OpeningIntervalCalculator {

	private final Person person;
	private final CountActEventHandler actCounter;
	private double tolerance;
	
	public AgentSpecificOpeningIntervalCalculator(Person person, CountActEventHandler actCount, double tolerance) {
		this.person = person;
		this.actCounter = actCount;
		this.tolerance = tolerance;
	}

	@Override
	public double[] getOpeningInterval(Activity act) {
		
		// identify the correct activity position in the plan
		int activityCounter = this.actCounter.getActivityCounter(person.getId());
		
		// get the original start/end times from survey / initial demand which is written in the person attributes
		String activityOpeningIntervals = (String) person.getAttributes().getAttribute("OpeningClosingTimes");
		
		if (activityOpeningIntervals == null || activityOpeningIntervals == "") {
			throw new RuntimeException("Person " + person.getId().toString() + " doesn't have any opening / closing times in the person attributes. Aborting...");
		}
		
		String activityOpeningTimes[] = activityOpeningIntervals.split(";");
	
		double openingTime = Double.valueOf(activityOpeningTimes[activityCounter * 2]) - tolerance;
		double closingTime = Double.valueOf(activityOpeningTimes[(activityCounter * 2) + 1]) + tolerance;

		return new double[]{openingTime, closingTime};
	}

}

