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

package playground.jbischoff.taxibus.scenario.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitActsRemover;

/**
 * @author jbischoff
 *
 */
public class RemoveBSWBPTRoutes {

	public static void main(String[] args) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimPopulationReader(scenario)
				.readFile("../../../shared-svn/projects/vw_rufbus/scenario/input/vw079.output_plans.xml.gz");

		for (Person p : scenario.getPopulation().getPersons().values()) {
//			if (p.getId().toString().startsWith("BS_WB") || p.getId().toString().startsWith("WB_BS"))
			{
				for (Plan plan : p.getPlans()) {
					new TransitActsRemover().run(plan);
				}
			}
		}
		new PopulationWriter(scenario.getPopulation()).write("../../../shared-svn/projects/vw_rufbus/scenario/input/vw079.output_plansNoPTRoutes.xml.gz");
	}
}
