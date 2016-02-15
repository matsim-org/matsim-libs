/* *********************************************************************** *
 * project: org.matsim.*
 * DgCottbusPreprocessPlans
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
package playground.dgrether.signalsystems.cottbus.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.XY2Links;

import playground.dgrether.DgPaths;


/**
 * @author dgrether
 *
 */
public class DgCottbusPreprocessPlans {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String conf = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/sylvia/cottbus_sylvia_config.xml";
		String popOutFile = DgPaths.REPOS + "shared-svn/studies/dgrether/cottbus/Cottbus-BA/cb-plans_usual_0.2_routed.xml.gz";
		
		Config config = ConfigUtils.loadConfig(conf);
		MatsimRandom.reset(config.global().getRandomSeed());
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		
		
		((PopulationImpl) scenario.getPopulation()).addAlgorithm(new XY2Links(scenario.getNetwork(), null));
		FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(scenario.getConfig().planCalcScore());
		((PopulationImpl) scenario.getPopulation()).addAlgorithm(
				new PlanRouter(
						new TripRouterFactoryBuilderWithDefaults().build(
								scenario ).get(
						) ) );
		((PopulationImpl) scenario.getPopulation()).runAlgorithms();
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		writer.write(popOutFile);
	}

}
