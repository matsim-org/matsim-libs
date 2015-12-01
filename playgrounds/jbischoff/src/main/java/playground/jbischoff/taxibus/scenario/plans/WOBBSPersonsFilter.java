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

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *
 */
public class WOBBSPersonsFilter {
	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		new MatsimPopulationReader(scenario).readFile(
				"C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/output/vw032.100pct/vw032.100pct.output_plans.xml.gz");
		// new
		// MatsimPopulationReader(scenario).readFile("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/initial_plans1.0.xml.gz");

		
		Scenario scenario2 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population pop2 = scenario2.getPopulation();
		Random r = MatsimRandom.getRandom();
		for (double threshold = 0.1; threshold < 0.6; threshold = threshold + 0.1) {
			Scenario scenario3 = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			
			
			
			
			for (Person p : scenario.getPopulation().getPersons().values()) {
				boolean possiblePerson = false;
				if ((p.getId().toString().startsWith("BS_WB")) && (p.getId().toString().endsWith("vw"))) {
					Plan plan = p.getSelectedPlan();

					String lastType = null;

					for (PlanElement pe : plan.getPlanElements()) {
						if (pe instanceof Activity) {
							Activity act = (Activity) pe;
							if (act.getType().startsWith("pt"))
								continue;

							if (lastType == null) {
								lastType = act.getType();
								continue;
							} else if (lastType.equals("work_vw_flexitime")) {
								if (act.getType().equals("home")) {
									possiblePerson = true;
									break;
								}
							}
							lastType = act.getType();
						}

					}
				}
					if ((possiblePerson)&&(r.nextDouble()<threshold)) {
						
							
							pop2.addPerson(p);
							
						}
						else {
							scenario3.getPopulation().addPerson(p);
						}
					

					// personen zaehlen mit akt = 3
					// aussortieen
			}

		for (Person p : pop2.getPersons().values()){
		
		}
		 new PopulationWriter(scenario3.getPopulation()).write("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/subpopulations/carplans_"+threshold+".xml.gz");
		 new PopulationWriter(pop2).write("C:/Users/Joschka/Documents/shared-svn/projects/vw_rufbus/scenario/input/subpopulations/taxibusplans_"+threshold+".xml.gz");
		 pop2.getPersons().clear();
		}
	}
}
