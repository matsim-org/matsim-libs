/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateEquilPopulations.java
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

package playground.meisterk.phd;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.basic.v01.IdImpl;

public class GenerateEquilPopulations {

	public static final int NUM_AGENTS = 4000;
	
	public GenerateEquilPopulations() {
		// TODO Auto-generated constructor stub
	}

	protected void generateRandomCarOnly(ScenarioImpl scenario) {
		
		Population pop = scenario.getPopulation();
		PopulationFactory popFactory = pop.getFactory();

		ActivityFacilities facilities = scenario.getActivityFacilities();
		
		Person person = null;
		Plan plan = null;
		Activity act = null;
		for (int ii=0; ii < NUM_AGENTS; ii++) {
			
			person = popFactory.createPerson(new IdImpl(ii));
			pop.addPerson(person);
			
			plan = popFactory.createPlan();
			person.addPlan(plan);
			plan.setSelected(true);
			
			act = popFactory.createActivityFromCoord("h", facilities.getFacilities().get(new IdImpl(1)).getCoord());
			plan.addActivity(act);
		}
		
	}
	
}
