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

package org.matsim.core.mobsim.jdeqsim.scenarios;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;

public class EquilPopulationPlans1Modified1 {

	public void modifyPopulation(Population population) {
		// modify population: a plan was needed, which contained some properties to be compared with C++
		Person p = population.getPersons().get(Id.create("1", Person.class));
		Plan plan = p.getSelectedPlan();
		List<? extends PlanElement> actsLegs = plan.getPlanElements();
		((Activity)actsLegs.get(0)).setEndTime(360);
		((Activity)actsLegs.get(2)).setEndTime(900); // this requires immediate departure after arrival
		((Activity)actsLegs.get(4)).setEndTime(2000);
	}

}
