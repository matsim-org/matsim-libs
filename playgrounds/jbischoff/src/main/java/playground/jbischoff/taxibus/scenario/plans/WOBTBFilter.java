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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author  jbischoff
 *
 */
public class WOBTBFilter {
public static void main(String[] args) {
	Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	
	new MatsimPopulationReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vw028.100pct/vw028.100pct.output_plans.xml.gz");
	Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
	Population pop2 = scenario2.getPopulation();
	int i = 0;
	for (Person p : scenario.getPopulation().getPersons().values()){
		if (p.getId().toString().endsWith("vw")){
			boolean copyPerson = true;
			i++;
			if (i%10000 == 0) System.out.println(i);
			for (Plan plan : p.getPlans()){
				int pes = plan.getPlanElements().size();
				Activity act = (Activity) plan.getPlanElements().get(pes-1);
				if (act.getStartTime()>23*3600) copyPerson = false;
			}
			if (copyPerson){
				pop2.addPerson(p);
			}
			
		}
	}
	System.out.println(i + " persons found ; "+pop2.getPersons().size()+" persons copied");
	new PopulationWriter(pop2).write("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vw028.100pct/filtered_plans.xml.gz");
}
}
