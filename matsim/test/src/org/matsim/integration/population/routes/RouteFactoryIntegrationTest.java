/* *********************************************************************** *
 * project: org.matsim.*
 * RouteFactoryIntegrationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.integration.population.routes;

import java.util.Collection;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.api.basic.v01.population.BasicRoute;
import org.matsim.api.basic.v01.population.PlanElement;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.CompressedNetworkRoute;
import org.matsim.core.population.routes.CompressedNetworkRouteFactory;
import org.matsim.core.population.routes.NodeNetworkRoute;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class RouteFactoryIntegrationTest extends MatsimTestCase {

	/**
	 * Tests that the plans-reader and ReRoute-strategy module use the specified RouteFactory.
	 */
	public void testRouteFactoryIntegration() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.plans().setInputFile("test/scenarios/equil/plans2.xml");
		Collection<StrategySettings> settings = config.strategy().getStrategySettings();
		for (StrategySettings setting: settings) {
			if ("ReRoute".equals(setting.getModuleName())) {
				setting.setProbability(1.0);
			} else {
				setting.setProbability(0.0);
			}
		}
		config.controler().setLastIteration(1);

//		 test the default
		config.controler().setOutputDirectory(getOutputDirectory() + "/default");
		Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.run();

		PopulationImpl population = controler.getPopulation();
		for (PersonImpl person : population.getPersons().values()) {
			for (PlanImpl plan : person.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof BasicLeg) {
						BasicLeg leg = (BasicLeg) pe;
						BasicRoute route = leg.getRoute();
						assertTrue(route instanceof NodeNetworkRoute); // that must be different from the class used below
					}
				}
			}
		}

		// test another setting
		Gbl.reset();
		config.controler().setOutputDirectory(getOutputDirectory() + "/variant1");
		ScenarioImpl scenario = new ScenarioImpl(config);
		((NetworkLayer)scenario.getNetwork()).getFactory().setRouteFactory(TransportMode.car, new CompressedNetworkRouteFactory(scenario.getNetwork()));
		ScenarioLoader loader = new ScenarioLoader(scenario);
		loader.loadScenario();
		
		Controler controler2 = new Controler(scenario);
		controler2.setCreateGraphs(false);
		controler2.setWriteEventsInterval(0);
		controler2.run();

		PopulationImpl population2 = controler2.getPopulation();
		for (PersonImpl person : population2.getPersons().values()) {
			int planCounter = 0;
			for (PlanImpl plan : person.getPlans()) {
				planCounter++;
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof BasicLeg) {
						BasicLeg leg = (BasicLeg) pe;
						BasicRoute route = leg.getRoute();
						assertTrue("person: " + person.getId() + "; plan: " + planCounter, route instanceof CompressedNetworkRoute);
					}
				}
			}
		}

	}	

}
