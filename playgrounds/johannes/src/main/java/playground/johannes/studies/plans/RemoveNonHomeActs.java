/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveNonHomeActs.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.studies.plans;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author illenberger
 *
 */
public class RemoveNonHomeActs {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		MatsimConfigReader creader = new MatsimConfigReader(config);
		creader.readFile(args[0]);
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
		
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(config.getParam("network", "inputNetworkFile"));
		MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
		popReader.readFile(config.getParam("plans", "inputPlansFile"));
		
		Population pop = scenario.getPopulation();
		
		for(Person p : pop.getPersons().values()) {
			for(int i = 1; i < p.getPlans().size(); i = 1)
				p.getPlans().remove(i);

			Plan selected = p.getSelectedPlan();
			for(int i = 1; i < selected.getPlanElements().size(); i = 1) {
				selected.getPlanElements().remove(i);
			}
		}

		new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(scenario.getConfig().getParam("popfilter", "outputPlansFile"));
	}

}
