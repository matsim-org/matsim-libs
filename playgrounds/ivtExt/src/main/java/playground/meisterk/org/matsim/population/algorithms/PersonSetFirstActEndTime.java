/* *********************************************************************** *
 * project: org.matsim.*
 * PersonSetFirstActEndTime.java
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

package playground.meisterk.org.matsim.population.algorithms;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

public class PersonSetFirstActEndTime extends AbstractPersonAlgorithm implements PlanAlgorithm {

	private double firstActEndTime = Double.MIN_VALUE;

	public PersonSetFirstActEndTime(double firstActEndTime) {
		super();
		this.firstActEndTime = firstActEndTime;
	}

	@Override
	public void run(Person person) {
		for (Plan plan : person.getPlans()) {
			this.run(plan);
		}
	}

	public void run(Plan plan) {
		Activity firstAct = ((PlanImpl) plan).getFirstActivity();
		firstAct.setEndTime(this.firstActEndTime);
		Leg firstLeg = ((PlanImpl) plan).getNextLeg(firstAct);
		firstLeg.setDepartureTime(this.firstActEndTime);
	}

}
