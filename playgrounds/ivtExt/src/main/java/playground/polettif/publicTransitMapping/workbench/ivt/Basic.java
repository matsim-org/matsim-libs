/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.polettif.publicTransitMapping.workbench.ivt;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationReaderMatsimV5;
import org.matsim.core.scenario.ScenarioUtils;
import playground.polettif.publicTransitMapping.tools.NetworkTools;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Basic {

	protected static Logger log = Logger.getLogger(Basic.class);

	public static void main(final String[] args) {
//		createConfig();
//		adaptPop();

		runScenario(args[0]);
	}


	private static void runScenario(String configFile) {
		final Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.run();
	}

	private static void createConfig() {
		Config config = ConfigUtils.createConfig();
		new ConfigWriter(config).write("config.xml");
	}

	/**
	 * modifiy population
	 */
	private static void adaptPop() {
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		PopulationReader reader = new PopulationReaderMatsimV5(sc);
		reader.readFile("population_original.xml.gz");
		Population population = sc.getPopulation();


		Network network = NetworkTools.readNetwork("network.xml.gz");

		Set<String> actTypes = new HashSet<>();

		// only home and work activities
		for(Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			List<PlanElement> elements = plan.getPlanElements();
			for(PlanElement e : elements) {
				if(e instanceof ActivityImpl) {
					Activity activity = (Activity) e;
					switch (activity.getType()) {
						case "home" :
							break;
						case "work" :
							break;
						default :
							activity.setType("work");
					}
				}
			}
		}

		new PopulationWriter(population, network).write("plans.xml.gz");
	}
	
}