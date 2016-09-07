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

/**
 * 
 */
package playground.jbischoff.csberlin.plans.preprocess;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class RemoveWVAgents {
public static void main(String[] args) {
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
	new PopulationReader(scenario).readFile("../../../shared-svn/projects/bmw_carsharing/data/scenario/untersuchungsraum-plans.xml.gz");
	for (Person p : scenario.getPopulation().getPersons().values()){
		if (!p.getId().toString().startsWith("wv")){
			pop2.addPerson(p);
			
			Plan plan = p.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Activity){
					Activity act = (Activity) pe;
					act.setFacilityId(null);
				}
			}
		}
	}
	new PopulationWriter(pop2).write("../../../shared-svn/projects/bmw_carsharing/data/scenario/untersuchungsraum-plans-nowv.xml.gz");
	
	
}
}
