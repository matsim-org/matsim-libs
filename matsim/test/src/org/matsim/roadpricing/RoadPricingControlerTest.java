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

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests the integration of the roadpricing-package into the Controler.
 *
 * @author mrieser
 */
public class RoadPricingControlerTest extends MatsimTestCase {

	/**
	 * Make sure that the road-pricing classes are not loaded when no road pricing is simulated.
	 */
	public void testBaseCase() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		Controler controler = new TestControler(config);
		controler.run();
		assertNull("RoadPricing shouldn't be loaded in case case.", controler.getRoadPricing());
		PlanAlgorithmI router = controler.getRoutingAlgorithm();
		assertFalse("BaseCase should not use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		ScoringFunctionFactory sff = controler.getScoringFunctionFactory();
		assertFalse("BaseCase should not use roadpricing-scoring function.", sff instanceof RoadPricingScoringFunctionFactory);
	}

	public void testDistanceToll() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "distanceToll.xml");
		Controler controler = new TestControler(config);
		controler.run();
		assertNotNull("RoadPricing should be loaded in distance toll case.", controler.getRoadPricing());
		PlanAlgorithmI router = controler.getRoutingAlgorithm();
		assertFalse("distance toll should not use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		ScoringFunctionFactory sff = controler.getScoringFunctionFactory();
		assertTrue("distance toll should use the roadpricing-scoring function.", sff instanceof RoadPricingScoringFunctionFactory);
	}

	public void testCordonToll() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "cordonToll.xml");
		Controler controler = new TestControler(config);
		controler.run();
		assertNotNull("RoadPricing should be loaded in cordon toll case.", controler.getRoadPricing());
		PlanAlgorithmI router = controler.getRoutingAlgorithm();
		assertFalse("cordon toll should not use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		ScoringFunctionFactory sff = controler.getScoringFunctionFactory();
		assertTrue("cordon toll should use the roadpricing-scoring function.", sff instanceof RoadPricingScoringFunctionFactory);
	}

	public void testAreaToll() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "areaToll.xml");
		Controler controler = new TestControler(config);
		controler.run();
		assertNotNull("RoadPricing should be loaded in area toll case.", controler.getRoadPricing());
		PlanAlgorithmI router = controler.getRoutingAlgorithm();
		assertTrue("area toll should use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		ScoringFunctionFactory sff = controler.getScoringFunctionFactory();
		assertTrue("area toll should use the roadpricing-scoring function.", sff instanceof RoadPricingScoringFunctionFactory);
	}

	/** Just a simple Controler that does not run the mobsim, as we're not interested in that part here. */
	private static class TestControler extends Controler {

		public TestControler(final Config config) {
			super(config);
			setCreateGraphs(false);
		}

		@Override
		protected void runMobSim() {
			// do not run the mobsim, as we're not interested in that here, so we can save this time.
		}

	}

}
