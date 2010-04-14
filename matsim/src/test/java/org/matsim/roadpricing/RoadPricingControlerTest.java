/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingControlerTest.java
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

package org.matsim.roadpricing;

import org.apache.log4j.Logger;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.TravelCost;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests the integration of the roadpricing-package into the Controler.
 *
 * @author mrieser
 */
public class RoadPricingControlerTest extends MatsimTestCase {

	static private final Logger log = Logger.getLogger(RoadPricingControlerTest.class);

	/**
	 * Make sure that the road-pricing classes are not loaded when no road pricing is simulated.
	 */
	public void testBaseCase() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		Controler controler = new TestControler(config);
		controler.run();
		assertNull("RoadPricing must not be loaded in case case.", controler.getRoadPricing());
		PlanAlgorithm router = controler.createRoutingAlgorithm();
		assertFalse("BaseCase must not use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		TravelCost travelCosts = controler.createTravelCostCalculator();
		assertFalse("BaseCase must not use TollTravelCostCalculator.", travelCosts instanceof TollTravelCostCalculator);
	}

	public void testDistanceToll() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.scenario().setUseRoadpricing(true);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "distanceToll.xml");
		Controler controler = new TestControler(config);
		controler.run();
		assertNotNull("RoadPricing must be loaded in distance toll case.", controler.getRoadPricing());
		PlanAlgorithm router = controler.createRoutingAlgorithm();
		assertFalse("Distance toll must not use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		TravelCost travelCosts = controler.createTravelCostCalculator();
		assertTrue("Distance toll must use TollTravelCostCalculator.", travelCosts instanceof TollTravelCostCalculator);
	}

	public void testCordonToll() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.scenario().setUseRoadpricing(true);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "cordonToll.xml");
		Controler controler = new TestControler(config);
		controler.run();
		assertNotNull("RoadPricing must be loaded in cordon toll case.", controler.getRoadPricing());
		PlanAlgorithm router = controler.createRoutingAlgorithm();
		assertFalse("Cordon toll must not use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		TravelCost travelCosts = controler.createTravelCostCalculator();
		assertTrue("Cordon toll must use TollTravelCostCalculator.", travelCosts instanceof TollTravelCostCalculator);
	}

	public void testAreaToll() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.scenario().setUseRoadpricing(true);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "areaToll.xml");
		Controler controler = new TestControler(config);
		controler.run();
		assertNotNull("RoadPricing should be loaded in area toll case.", controler.getRoadPricing());
		PlanAlgorithm router = controler.createRoutingAlgorithm();
		assertTrue("Area toll should use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		TravelCost travelCosts = controler.createTravelCostCalculator();
		assertFalse("Area toll must not use TollTravelCostCalculator.", travelCosts instanceof TollTravelCostCalculator);
	}

	/** Checks that inconsistencies in the configuration are recognized, namely with the replanning strategies.
	 *
	 * @author mrieser
	 */
	public void testAreaTollStrategyConfig() {
		// start with the default configuration, which is ok.
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.scenario().setUseRoadpricing(true);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "areaToll.xml");
		try {
			Controler controler = new TestControler(config);
			controler.run();
		} catch (RuntimeException unexpected) {
			throw unexpected;
		}

		// now add a strategy that is not okay
		int index = config.strategy().getStrategySettings().size() + 1;
		config.strategy().addParam("Module_" + index, "ReRoute_Landmarks");
		config.strategy().addParam("ModuleProbability_" + index, "0.1");
		try {
			Gbl.reset();
			Controler controler = new TestControler(config);
			controler.setOverwriteFiles(true);
			controler.run();
			fail("Expected RuntimeException because of ReRoute_Landmarks, but got none.");
		} catch (RuntimeException expected) {
			log.info("Catched RuntimeException as expected: " + expected.getMessage());
		}

		// try to fix the strategy, but fail again
		config.strategy().addParam("Module_" + index, "ReRoute_Dijkstra");
		try {
			Gbl.reset();
			Controler controler = new TestControler(config);
			controler.setOverwriteFiles(true);
			controler.run();
			fail("Expected RuntimeException because of ReRoute_Dijkstra, but got none.");
		} catch (RuntimeException expected) {
			log.info("Catched RuntimeException as expected: " + expected.getMessage());
		}

		// now fix it
		config.strategy().addParam("Module_" + index, "ReRoute");
		try {
			Gbl.reset();
			Controler controler = new TestControler(config);
			controler.setOverwriteFiles(true);
			controler.run();
		} catch (RuntimeException unexpected) {
			throw unexpected;
		}
	}

	/** Tests that paid tolls end up in the score. */
	public void testTollScores() {
		// first run basecase
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.plans().setInputFile("test/scenarios/equil/plans1.xml");
		config.controler().setOutputDirectory(getOutputDirectory() + "/basecase/");
		Controler controler1 = new Controler(config);
		controler1.setCreateGraphs(false);
		controler1.setWriteEventsInterval(0);
		controler1.run();
		double scoreBasecase = controler1.getPopulation().getPersons().get(new IdImpl("1")).getPlans().get(0).getScore().doubleValue();

		// now run toll case
		Gbl.reset();
		config.scenario().setUseRoadpricing(true);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "distanceToll.xml");
		config.controler().setOutputDirectory(getOutputDirectory() + "/tollcase/");
		Controler controler2 = new Controler(config);
		controler2.setCreateGraphs(false);
		controler2.setWriteEventsInterval(0);
		controler2.run();
		double scoreTollcase = controler2.getPopulation().getPersons().get(new IdImpl("1")).getPlans().get(0).getScore().doubleValue();

		// there should be a score difference
		assertEquals(3.0, scoreBasecase - scoreTollcase, EPSILON); // toll amount: 10000*0.00020 + 5000*0.00020
	}

	/** Just a simple Controler that does not run the mobsim, as we're not interested in that part here. */
	private static class TestControler extends Controler {

		protected TestControler(final Config config) {
			super(config);
			setCreateGraphs(false);
			setWriteEventsInterval(0);
		}

		@Override
		protected void runMobSim() {
			// do not run the mobsim, as we're not interested in that here, so we can save this time.
		}

	}

}
