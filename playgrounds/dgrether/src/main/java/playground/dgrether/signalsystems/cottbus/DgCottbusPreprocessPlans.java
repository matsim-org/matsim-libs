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
package playground.dgrether.signalsystems.cottbus;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.scenario.ScenarioLoaderImpl;
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
		ScenarioLoader loader = new ScenarioLoaderImpl(conf);
		Scenario scenario = loader.loadScenario();
		((PopulationImpl) scenario.getPopulation()).addAlgorithm(new XY2Links((NetworkImpl) scenario.getNetwork()));
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(scenario.getConfig().charyparNagelScoring());
		((PopulationImpl) scenario.getPopulation()).addAlgorithm(new PlansCalcRoute(scenario.getConfig().plansCalcRoute(), scenario.getNetwork(), timeCostCalc, timeCostCalc, new DijkstraFactory()));
		((PopulationImpl) scenario.getPopulation()).runAlgorithms();
		PopulationWriter writer = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		writer.write(popOutFile);
	}

}
