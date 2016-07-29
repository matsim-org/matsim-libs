/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.csberlin.plans.preprocess;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class CountPossibleCarOwners {
public static void main(String[] args) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	new PopulationReader(scenario).readFile("../../../shared-svn/projects/bmw_carsharing/data/scenario/mierendorff-plans.xml.gz");
	int i = 0;
	for (Person p : scenario.getPopulation().getPersons().values()){
		Plan plan = p.getSelectedPlan();
		for (PlanElement pe : plan.getPlanElements()){
			if (pe instanceof Leg){
				Leg leg = (Leg) pe;
				if (leg.getMode().equals("car")){
				i++;
				break;
				}
			}
		}
	}
	System.out.println(i);
}
}
