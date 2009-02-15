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

import org.matsim.basic.v01.BasicLeg;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.roadpricing.RoadPricingScheme.Cost;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests {@link PlansCalcAreaTollRoute} as isolated as possible.
 *
 * @author mrieser
 */
public class PlansCalcAreaTollRouteTest extends MatsimTestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		loadConfig(null);
	}

	/**
	 * Tests a few cases where the router can decide if it is better to pay the toll or not.
	 */
	public void testBestAlternatives() {
		NetworkLayer network = Fixture.createNetwork2();

		// a basic toll where only the morning hours are tolled
		RoadPricingScheme toll = new RoadPricingScheme(network);
		toll.setType("area");
		toll.addLink("5");
		toll.addLink("11");
		Cost morningCost = toll.addCost(6*3600, 10*3600, 0.06);
		/* Start with a rather low toll. The toll is also so low, because we only
		 * have small network with short links: the cost to travel across one link
		 * is: 20s * (-6 EUR / h) = 20 * (-6) / 3600 = 0.03333
		 */

		Population population = Fixture.createPopulation2(network);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();

		PreProcessLandmarks commonRouterData = new PreProcessLandmarks(timeCostCalc);
		commonRouterData.run(network);

		Leg leg1 = (Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1));
		Leg leg2 = (Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(3));

		// case 1: toll only in morning, it is cheaper to drive around
		new PlansCalcAreaTollRoute(network, commonRouterData, timeCostCalc, timeCostCalc, toll).run(population);
		Fixture.compareRoutes("1 2 3 4 5", (CarRoute) leg1.getRoute());
		Fixture.compareRoutes("6 7 9 10", (CarRoute) leg2.getRoute());

		// case 2: now add a toll in the afternoon too, so it is cheaper to pay the toll
		Cost afternoonCost = toll.addCost(14*3600, 18*3600, 0.06);
		new PlansCalcAreaTollRoute(network, commonRouterData, timeCostCalc, timeCostCalc, toll).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg1.getRoute());
		Fixture.compareRoutes("6 7 9 10", (CarRoute) leg2.getRoute());

		// case 3: change the second leg to a non-car mode, than it should be the same as case 1
		BasicLeg.Mode oldMode = leg2.getMode();
		leg2.setMode(BasicLeg.Mode.pt);
		new PlansCalcAreaTollRoute(network, commonRouterData, timeCostCalc, timeCostCalc, toll).run(population);
		Fixture.compareRoutes("1 2 3 4 5", (CarRoute) leg1.getRoute());
		Fixture.compareRoutes("6 7 9 10", (CarRoute) leg2.getRoute());
		// and change the mode back
		leg2.setMode(oldMode);

		// case 4: now remove the costs and add them again, but with a higher amount
		toll.removeCost(morningCost);
		toll.removeCost(afternoonCost);
		toll.addCost(6*3600, 10*3600, 0.7);
		toll.addCost(14*3600, 18*3600, 0.7);
		// the agent should now decide to drive around
		new PlansCalcAreaTollRoute(network, commonRouterData, timeCostCalc, timeCostCalc, toll).run(population);
		Fixture.compareRoutes("1 2 3 4 5", (CarRoute) leg1.getRoute());
		Fixture.compareRoutes("6 7 8 9 10", (CarRoute) leg2.getRoute());
	}

	/**
	 * Tests cases where the agent must pay the toll because one of its activities is on a tolled link
	 */
	public void testTolledActLink() {
		NetworkLayer network = Fixture.createNetwork2();

		// a basic toll where only the morning hours are tolled
		RoadPricingScheme toll = new RoadPricingScheme(network);
		toll.setType("area");
		toll.addLink("7");
		toll.addCost(6*3600, 10*3600, 0.06);

		Population population = Fixture.createPopulation2(network);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();

		PreProcessLandmarks commonRouterData = new PreProcessLandmarks(timeCostCalc);
		commonRouterData.run(network);

		Leg leg1 = (Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1));
		Leg leg2 = (Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(3));

		new PlansCalcAreaTollRoute(network, commonRouterData, timeCostCalc, timeCostCalc, toll).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg1.getRoute()); // agent should take shortest route
		Fixture.compareRoutes("6 7 9 10", (CarRoute) leg2.getRoute());
	}

	/**
	 * Tests cases where the agent must pay the toll because all alternative routes from one location
	 * to the next include tolled links
	 */
	public void testAllAlternativesTolled() {
		NetworkLayer network = Fixture.createNetwork2();

		// a basic toll where only the morning hours are tolled
		RoadPricingScheme toll = new RoadPricingScheme(network);
		toll.setType("area");
		toll.addLink("3");
		toll.addLink("5");
		toll.addCost(6*3600, 10*3600, 0.06);

		Population population = Fixture.createPopulation2(network);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();

		PreProcessLandmarks commonRouterData = new PreProcessLandmarks(timeCostCalc);
		commonRouterData.run(network);

		Leg leg1 = (Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1));
		Leg leg2 = (Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(3));

		new PlansCalcAreaTollRoute(network, commonRouterData, timeCostCalc, timeCostCalc, toll).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg1.getRoute()); // agent should take shortest route
		Fixture.compareRoutes("6 7 9 10", (CarRoute) leg2.getRoute());
	}

	public void testOutsideTollTime() {
		NetworkLayer network = Fixture.createNetwork2();

		// a basic toll where only the morning hours are tolled
		RoadPricingScheme toll = new RoadPricingScheme(network);
		toll.setType("area");
		toll.addLink("5");
		toll.addLink("11");
		toll.addCost(8*3600, 10*3600, 1.0); // high costs!

		Population population = Fixture.createPopulation2(network);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();

		PreProcessLandmarks commonRouterData = new PreProcessLandmarks(timeCostCalc);
		commonRouterData.run(network);

		Leg leg1 = (Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1));
		Leg leg2 = (Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(3));

		new PlansCalcAreaTollRoute(network, commonRouterData, timeCostCalc, timeCostCalc, toll).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg1.getRoute()); // agent should take shortest route, as tolls are not active at that time
		Fixture.compareRoutes("6 7 9 10", (CarRoute) leg2.getRoute());
	}

}
