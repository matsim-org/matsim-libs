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

import java.util.List;

import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
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
		List<Plan> plans = person.getPlans();
		for (int i=0; i<plans.size(); i++) {
			Plan plan = plans.get(i);
			this.run(plan);
		}
	}

	public void run(Plan plan) {

		Act firstAct = plan.getFirstActivity();
		firstAct.setEndTime(this.firstActEndTime);
		Leg firstLeg = plan.getNextLeg(firstAct);
		firstLeg.setDepartureTime(this.firstActEndTime);
	
	}

}
