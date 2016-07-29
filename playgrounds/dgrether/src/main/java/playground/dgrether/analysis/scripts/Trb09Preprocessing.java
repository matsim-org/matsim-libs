/* *********************************************************************** *
 * project: org.matsim.*
 * Trb09Analysis
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.analysis.scripts;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.dgrether.DgPaths;


public class Trb09Preprocessing {

	private static final Logger log = Logger.getLogger(Trb09Preprocessing.class);


	/**
	 * @param args
	 */
	public static void main(String[] args) {
			String runNumber1 = "879";
//			String runNumber1 = "732";
//			String runNumber2 = "733";

			String runid1 = "run" + runNumber1;

			String runiddot1 = runid1 + ".";

			String netfile = DgPaths.RUNBASE + runid1 + "/" + runNumber1 + ".output_network.xml.gz";
			String plans1file = DgPaths.RUNBASE + runid1 + "/" + runNumber1 + ".output_plans.xml.gz";
			String plans1fileOut = DgPaths.RUNBASE + runid1 + "/" + runNumber1 + ".output_plans_wo_routes.xml.gz";

			MutableScenario sc = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			Config conf = sc.getConfig();
			conf.network().setInputFile(netfile);
			conf.plans().setInputFile(plans1file);
			ScenarioUtils.loadScenario(sc);
			Population pop = sc.getPopulation();
			for (Person p : pop.getPersons().values()){
				for (Plan plan : p.getPlans()) {
					for (PlanElement pe : plan.getPlanElements()){
						if (pe instanceof Leg) {
							Leg l = (Leg)pe;
							l.setRoute(null);
						}
					}
				}
			}

			PopulationWriter writer = new PopulationWriter(pop, sc.getNetwork());
			writer.write(plans1fileOut);
			log.debug("ya esta ;-)");

	}

}
