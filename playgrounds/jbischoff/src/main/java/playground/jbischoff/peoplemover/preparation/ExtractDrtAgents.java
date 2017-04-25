/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package playground.jbischoff.peoplemover.preparation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
/**
 *
 */
public class ExtractDrtAgents {
public static void main(String[] args) {
	
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	StreamingPopulationReader spr = new StreamingPopulationReader(scenario);
	Population pop2 = PopulationUtils.createPopulation(ConfigUtils.createConfig());
	spr.addAlgorithm(new PersonAlgorithm() {
		
		@Override
		public void run(Person person) {
			Plan plan = person.getSelectedPlan();
			for (PlanElement pe : plan.getPlanElements()){
				if (pe instanceof Leg){
					if (((Leg) pe).getMode().equals("drt")){
						if(!pop2.getPersons().containsKey(person.getId())){
						Person p1 = pop2.getFactory().createPerson(person.getId());
						p1.addPlan(plan);
						pop2.addPerson(p1);
						}
					}
					
				}
			}	
			
		}
	});
	spr.readFile("../../../shared-svn/projects/vw_rufbus/projekt2/input/peoplemover/testscenario/runPM120.10.150.plans.xml.gz");
	new PopulationWriter(pop2).write("../../../shared-svn/projects/vw_rufbus/projekt2/input/peoplemover/testscenario/drtPlans.xml");
}
}
