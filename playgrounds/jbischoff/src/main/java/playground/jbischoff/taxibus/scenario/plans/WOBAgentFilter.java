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

package playground.jbischoff.taxibus.scenario.plans;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class WOBAgentFilter {
public static void main(String[] args) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	new MatsimPopulationReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/tb.output_plans.xml.gz");
	Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Population pop2 = scenario2.getPopulation();
	int i = 0;
	for (Person p : scenario.getPopulation().getPersons().values()){

			boolean copyPerson = false;
			i++;
			if (i%10000 == 0) System.out.println(i);
			for (Plan plan : p.getPlans()){
				for (PlanElement pe : plan.getPlanElements()){
					if (pe instanceof Leg){
						if (((Leg) pe).getMode().equals("taxibus")){
							copyPerson = true;
						}
					}
				} 
			}
			if (copyPerson){
				pop2.addPerson(p);
			}
			
		
	}
	System.out.println(i + " persons found ; "+pop2.getPersons().size()+" persons copied");
	new PopulationWriter(pop2).write("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/tbonly.output_plans.xml.g");
}
}
