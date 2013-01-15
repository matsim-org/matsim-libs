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

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.population.algorithms.PlanAlgorithm;
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
		config.controler().setWritePlansInterval(0);
		Controler controler = new Controler(config);
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(false);
		controler.getConfig().controler().setWriteEventsInterval(0);
		controler.run();
		PlanAlgorithm router = controler.createRoutingAlgorithm();
		assertFalse("BaseCase must not use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		TravelDisutility travelCosts = controler.createTravelCostCalculator();
		assertFalse("BaseCase must not use TollTravelCostCalculator.", travelCosts instanceof TravelDisutilityIncludingToll);
	}

	public void testDistanceToll() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.controler().setWritePlansInterval(0);
		config.scenario().setUseRoadpricing(true);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "distanceToll.xml");
		Controler controler = new Controler(config);
		controler.addControlerListener(new RoadPricing());
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(false);
		controler.getConfig().controler().setWriteEventsInterval(0);
		controler.run();
		PlanAlgorithm router = controler.createRoutingAlgorithm();
		assertFalse("Distance toll must not use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		TravelDisutility travelCosts = controler.createTravelCostCalculator();
		assertTrue("Distance toll must use TollTravelCostCalculator.", travelCosts instanceof TravelDisutilityIncludingToll);
	}

	public void testCordonToll() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.controler().setWritePlansInterval(0);
		config.scenario().setUseRoadpricing(true);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "cordonToll.xml");
		Controler controler = new Controler(config);
		controler.addControlerListener(new RoadPricing());
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(false);
		controler.getConfig().controler().setWriteEventsInterval(0);
		controler.run();
		PlanAlgorithm router = controler.createRoutingAlgorithm();
		assertFalse("Cordon toll must not use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		TravelDisutility travelCosts = controler.createTravelCostCalculator();
		assertTrue("Cordon toll must use TollTravelCostCalculator.", travelCosts instanceof TravelDisutilityIncludingToll);
	}

	public void testAreaToll() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.controler().setWritePlansInterval(0);
		config.scenario().setUseRoadpricing(true);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "areaToll.xml");
		Controler controler = new AreaTollControler(config);
		controler.addControlerListener(new RoadPricing());
		controler.setCreateGraphs(false);
		controler.setDumpDataAtEnd(false);
		controler.getConfig().controler().setWriteEventsInterval(0);
		controler.run();
		PlanAlgorithm router = controler.createRoutingAlgorithm();
		assertTrue("Area toll should use area-toll router.", router instanceof PlansCalcAreaTollRoute);
		TravelDisutility travelCosts = controler.createTravelCostCalculator();
		assertFalse("Area toll must not use TollTravelCostCalculator.", travelCosts instanceof TravelDisutilityIncludingToll);
	}


	/** Tests that paid tolls end up in the score. */
	public void testTollScores() {
		// first run basecase
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.plans().setInputFile("test/scenarios/equil/plans1.xml");
		config.controler().setOutputDirectory(getOutputDirectory() + "/basecase/");
		config.controler().setWritePlansInterval(0);
		Controler controler1 = new Controler(config);
		controler1.setCreateGraphs(false);
		controler1.setDumpDataAtEnd(false);
		controler1.getConfig().controler().setWriteEventsInterval(0);
		controler1.run();
		double scoreBasecase = controler1.getPopulation().getPersons().get(new IdImpl("1")).getPlans().get(0).getScore().doubleValue();

		// now run toll case
		config.scenario().setUseRoadpricing(true);
		config.roadpricing().setTollLinksFile(getInputDirectory() + "distanceToll.xml");
		config.controler().setOutputDirectory(getOutputDirectory() + "/tollcase/");
		Controler controler2 = new Controler(config);
		controler2.addControlerListener(new RoadPricing());
		controler2.setCreateGraphs(false);
		controler2.setDumpDataAtEnd(false);
		controler2.getConfig().controler().setWriteEventsInterval(0);
		controler2.run();
		double scoreTollcase = controler2.getPopulation().getPersons().get(new IdImpl("1")).getPlans().get(0).getScore().doubleValue();

		// there should be a score difference
		assertEquals(3.0, scoreBasecase - scoreTollcase, EPSILON); // toll amount: 10000*0.00020 + 5000*0.00020
	}


}
