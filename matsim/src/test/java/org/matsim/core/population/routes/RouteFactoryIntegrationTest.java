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

package org.matsim.core.population.routes;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.*;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Collection;

/**
 * @author mrieser
 */
public class RouteFactoryIntegrationTest {

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Tests that the plans-reader and ReRoute-strategy module use the specified RouteFactory.
	 */
	@Test
	public void testRouteFactoryIntegration() {
		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.plans().setInputFile("plans2.xml");
		Collection<StrategySettings> settings = config.strategy().getStrategySettings();
		for (StrategySettings setting: settings) {
			if ("ReRoute".equals(setting.getStrategyName())) {
				setting.setWeight(1.0);
			} else {
				setting.setWeight(0.0);
			}
		}
		config.controler().setLastIteration(1);

//		 test the default
		config.controler().setOutputDirectory(utils.getOutputDirectory() + "/default");
		Controler controler = new Controler(config);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.getConfig().controler().setWriteEventsInterval(0);
		controler.run();

        Population population = controler.getScenario().getPopulation();
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						Route route = leg.getRoute();
						Assert.assertTrue(route instanceof NetworkRoute  || route instanceof GenericRouteImpl ); // that must be different from the class used below
						// yy I added the "|| route instanceof GenericRouteImpl" to compensate for the added walk legs; a more precise 
						// test would be better. kai, feb'16
					}
				}
			}
		}

		// test another setting
		config.controler().setOutputDirectory(utils.getOutputDirectory() + "/variant1");
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(NetworkRoute.class, new CompressedNetworkRouteFactory(scenario.getNetwork()));
		ScenarioUtils.loadScenario(scenario);

		Controler controler2 = new Controler(scenario);
        controler2.getConfig().controler().setCreateGraphs(false);
        controler2.getConfig().controler().setWriteEventsInterval(0);
		controler2.run();

        Population population2 = controler2.getScenario().getPopulation();
		for (Person person : population2.getPersons().values()) {
			int planCounter = 0;
			for (Plan plan : person.getPlans()) {
				planCounter++;
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						Leg leg = (Leg) pe;
						Route route = leg.getRoute();
						Assert.assertTrue("person: " + person.getId() + "; plan: " + planCounter,
								route instanceof CompressedNetworkRouteImpl || route instanceof GenericRouteImpl );
						// yy I added the "|| route instanceof GenericRouteImpl" to compensate for the added walk legs; a more precise 
						// test would be better. kai, feb'16
					}
				}
			}
		}

	}

}
