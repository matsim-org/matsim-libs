/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcAreaTollRouteTest.java
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

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.LegImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.roadpricing.RoadPricingScheme.Cost;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests {@link PlansCalcAreaTollRoute} as isolated as possible.
 *
 * @author mrieser
 */
public class PlansCalcAreaTollRouteTest extends MatsimTestCase {

	/**
	 * Tests a few cases where the router can decide if it is better to pay the toll or not.
	 */
	public void testBestAlternatives() {
		Config config = loadConfig(null);
		NetworkLayer network = Fixture.createNetwork2();

		// a basic toll where only the morning hours are tolled
		RoadPricingScheme toll = new RoadPricingScheme(network);
		toll.setType("area");
		toll.addLink("5");
		toll.addLink("11");
		Cost morningCost = toll.addCost(6*3600, 10*3600, 0.12);
		/* Start with a rather low toll. The toll is also so low, because we only
		 * have small network with short links: the cost to travel across one link
		 * is: 20s * (-6 EUR / h) = 20 * (-6) / 3600 = 0.03333
		 */

		Population population = Fixture.createPopulation2(network);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(config.charyparNagelScoring());

		AStarLandmarksFactory factory = new AStarLandmarksFactory(network, timeCostCalc);

		Id id1 = new IdImpl("1");
		LegImpl leg1 = (LegImpl) (population.getPersons().get(id1).getPlans().get(0).getPlanElements().get(1));
		LegImpl leg2 = (LegImpl) (population.getPersons().get(id1).getPlans().get(0).getPlanElements().get(3));

		// case 1: toll only in morning, it is cheaper to drive around
		new PlansCalcAreaTollRoute(config.plansCalcRoute(), network, timeCostCalc, timeCostCalc, factory, toll).run(population);
		Fixture.compareRoutes("1 2 3 4 5", (NetworkRoute) leg1.getRoute());
		Fixture.compareRoutes("6 7 9 10", (NetworkRoute) leg2.getRoute());

		// case 2: now add a toll in the afternoon too, so it is cheaper to pay the toll
		Cost afternoonCost = toll.addCost(14*3600, 18*3600, 0.06);
		new PlansCalcAreaTollRoute(config.plansCalcRoute(), network, timeCostCalc, timeCostCalc, factory, toll).run(population);
		Fixture.compareRoutes("1 2 4 5", (NetworkRoute) leg1.getRoute());
		Fixture.compareRoutes("6 7 9 10", (NetworkRoute) leg2.getRoute());

		// case 3: change the second leg to a non-car mode, than it should be the same as case 1
		TransportMode oldMode = leg2.getMode();
		leg2.setMode(TransportMode.pt);
		new PlansCalcAreaTollRoute(config.plansCalcRoute(), network, timeCostCalc, timeCostCalc, factory, toll).run(population);
		Fixture.compareRoutes("1 2 3 4 5", (NetworkRoute) leg1.getRoute());
		// and change the mode back
		leg2.setMode(oldMode);

		// case 4: now remove the costs and add them again, but with a higher amount
		toll.removeCost(morningCost);
		toll.removeCost(afternoonCost);
		toll.addCost(6*3600, 10*3600, 0.7);
		toll.addCost(14*3600, 18*3600, 0.7);
		// the agent should now decide to drive around
		new PlansCalcAreaTollRoute(config.plansCalcRoute(), network, timeCostCalc, timeCostCalc, factory, toll).run(population);
		Fixture.compareRoutes("1 2 3 4 5", (NetworkRoute) leg1.getRoute());
	}

	/**
	 * Tests cases where the agent must pay the toll because one of its activities is on a tolled link
	 */
	public void testTolledActLink() {
		Config config = loadConfig(null);
		NetworkLayer network = Fixture.createNetwork2();

		// a basic toll where only the morning hours are tolled
		RoadPricingScheme toll = new RoadPricingScheme(network);
		toll.setType("area");
		toll.addLink("7");
		toll.addCost(6*3600, 10*3600, 0.06);

		Population population = Fixture.createPopulation2(network);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(config.charyparNagelScoring());

		AStarLandmarksFactory factory = new AStarLandmarksFactory(network, timeCostCalc);

		Id id1 = new IdImpl("1");
		LegImpl leg1 = (LegImpl) (population.getPersons().get(id1).getPlans().get(0).getPlanElements().get(1));
		LegImpl leg2 = (LegImpl) (population.getPersons().get(id1).getPlans().get(0).getPlanElements().get(3));

		new PlansCalcAreaTollRoute(config.plansCalcRoute(), network, timeCostCalc, timeCostCalc, factory, toll).run(population);
		Fixture.compareRoutes("1 2 4 5", (NetworkRoute) leg1.getRoute()); // agent should take shortest route
		Fixture.compareRoutes("6 7 9 10", (NetworkRoute) leg2.getRoute());
	}

	/**
	 * Tests cases where the agent must pay the toll because all alternative routes from one location
	 * to the next include tolled links
	 */
	public void testAllAlternativesTolled() {
		Config config = loadConfig(null);
		NetworkLayer network = Fixture.createNetwork2();

		// a basic toll where only the morning hours are tolled
		RoadPricingScheme toll = new RoadPricingScheme(network);
		toll.setType("area");
		toll.addLink("3");
		toll.addLink("5");
		toll.addCost(6*3600, 10*3600, 0.06);

		Population population = Fixture.createPopulation2(network);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(config.charyparNagelScoring());

		AStarLandmarksFactory factory = new AStarLandmarksFactory(network, timeCostCalc);
		
		Id id1 = new IdImpl("1");
		LegImpl leg1 = (LegImpl) (population.getPersons().get(id1).getPlans().get(0).getPlanElements().get(1));
		LegImpl leg2 = (LegImpl) (population.getPersons().get(id1).getPlans().get(0).getPlanElements().get(3));

		new PlansCalcAreaTollRoute(config.plansCalcRoute(), network, timeCostCalc, timeCostCalc, factory, toll).run(population);
		Fixture.compareRoutes("1 2 4 5", (NetworkRoute) leg1.getRoute()); // agent should take shortest route
		Fixture.compareRoutes("6 7 9 10", (NetworkRoute) leg2.getRoute());
	}

	public void testOutsideTollTime() {
		Config config = loadConfig(null);
		NetworkLayer network = Fixture.createNetwork2();

		// a basic toll where only the morning hours are tolled
		RoadPricingScheme toll = new RoadPricingScheme(network);
		toll.setType("area");
		toll.addLink("5");
		toll.addLink("11");
		toll.addCost(8*3600, 10*3600, 1.0); // high costs!

		Population population = Fixture.createPopulation2(network);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost(config.charyparNagelScoring());

		AStarLandmarksFactory factory = new AStarLandmarksFactory(network, timeCostCalc);

		Id id1 = new IdImpl("1");
		LegImpl leg1 = (LegImpl) (population.getPersons().get(id1).getPlans().get(0).getPlanElements().get(1));
		LegImpl leg2 = (LegImpl) (population.getPersons().get(id1).getPlans().get(0).getPlanElements().get(3));

		new PlansCalcAreaTollRoute(config.plansCalcRoute(), network, timeCostCalc, timeCostCalc, factory, toll).run(population);
		Fixture.compareRoutes("1 2 4 5", (NetworkRoute) leg1.getRoute()); // agent should take shortest route, as tolls are not active at that time
		Fixture.compareRoutes("6 7 9 10", (NetworkRoute) leg2.getRoute());
	}

}
