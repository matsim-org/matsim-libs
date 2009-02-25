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

import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.config.Config;
import org.matsim.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.interfaces.basic.v01.BasicRoute;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.network.NetworkFactory;
import org.matsim.population.Population;
import org.matsim.population.routes.CompressedCarRoute;
import org.matsim.population.routes.CompressedCarRouteFactory;
import org.matsim.population.routes.NodeCarRoute;
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

		// test the default
		config.controler().setOutputDirectory(getOutputDirectory() + "/default");
		Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		controler.run();

		Population population = controler.getPopulation();
		for (Person person : population) {
			for (Plan plan : person.getPlans()) {
				for (LegIterator iter = plan.getIteratorLeg(); iter.hasNext(); ) {
					BasicLeg leg = iter.next();
					BasicRoute route = leg.getRoute();
					assertTrue(route instanceof NodeCarRoute); // that must be different from the class used below
				}
			}
		}

		// test another setting
		Gbl.reset();
		config.controler().setOutputDirectory(getOutputDirectory() + "/variant1");
		Controler controler2 = new TestControler(config);
		controler2.setCreateGraphs(false);
		controler2.setWriteEventsInterval(0);
		controler2.run();

		Population population2 = controler2.getPopulation();
		for (Person person : population2) {
			int planCounter = 0;
			for (Plan plan : person.getPlans()) {
				planCounter++;
				for (LegIterator iter = plan.getIteratorLeg(); iter.hasNext(); ) {
					BasicLeg leg = iter.next();
					BasicRoute route = leg.getRoute();
					assertTrue("person: " + person.getId() + "; plan: " + planCounter, route instanceof CompressedCarRoute);
				}
			}
		}

	}

	private final static class TestControler extends Controler {
		/*package*/ TestControler(final Config config) {
			super(config);
		}

		@Override
		protected Population loadPopulation() {
			NetworkFactory factory = getNetworkFactory();
			factory.setRouteFactory(BasicLeg.Mode.car, new CompressedCarRouteFactory(getNetwork()));
			return super.loadPopulation();
		}
	}

}
