/* *********************************************************************** *
 * project: org.matsim.*
 * TollTravelCostCalculatorTest.java
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
import org.matsim.basic.v01.BasicPlanImpl.LegIterator;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.roadpricing.RoadPricingScheme.Cost;
import org.matsim.router.PlansCalcRouteDijkstra;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests the correct working of {@link TollTravelCostCalculator} by using it
 * to calculate some routes with {@link PlansCalcRouteDijkstra} and
 * {@link PlansCalcRouteLandmarks}.
 *
 * @author mrieser
 */
public class TollTravelCostCalculatorTest extends MatsimTestCase {

	public void testDistanceTollRouter() {
		loadConfig(null);

		NetworkLayer network = Fixture.createNetwork2();
		Gbl.getWorld().setNetworkLayer(network);
		// a basic toll where only the morning hours are tolled
		RoadPricingScheme toll = new RoadPricingScheme(network);
		toll.setType("distance");
		toll.addLink("5");
		toll.addLink("11");
		Population population = Fixture.createPopulation2(network);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		TravelCost costCalc = new TollTravelCostCalculator(timeCostCalc, toll); // we use freespeedTravelCosts as base costs

		PreProcessLandmarks commonRouterData = new PreProcessLandmarks(timeCostCalc);
		commonRouterData.run(network);

		Leg leg = ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1)));

		// 1st case: without toll, agent chooses shortest path
		new PlansCalcRouteDijkstra(network, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1))).getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		assertNull(leg.getRoute()); // make sure the cleaning worked. we do this only once, then we believe it.
		new PlansCalcRouteLandmarks(network, commonRouterData, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg.getRoute());

		Cost morningCost = toll.addCost(6*3600, 10*3600, 0.0003); // 0.0003 * link_length(100m) = 0.03, which is slightly below the threshold of 0.0333
		// 2nd case: with a low toll, agent still chooses shortest path
		clearRoutes(population);
		new PlansCalcRouteDijkstra(network, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg.getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		new PlansCalcRouteLandmarks(network, commonRouterData, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg.getRoute());

		// 3rd case: with a higher toll, agent decides to drive around tolled link
		toll.removeCost(morningCost);
		toll.addCost(6*3600, 10*3600, 0.00035); // new morning toll, this should be slightly over the threshold
		clearRoutes(population);
		new PlansCalcRouteDijkstra(network, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 3 4 5", (CarRoute) leg.getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		new PlansCalcRouteLandmarks(network, commonRouterData, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 3 4 5", (CarRoute) leg.getRoute());
	}

	public void testCordonTollRouter() {
		loadConfig(null);

		NetworkLayer network = Fixture.createNetwork2();
		Gbl.getWorld().setNetworkLayer(network);
		// a basic toll where only the morning hours are tolled
		RoadPricingScheme toll = new RoadPricingScheme(network);
		toll.setType("cordon");
		toll.addLink("5");
		toll.addLink("11");
		Population population = Fixture.createPopulation2(network);
		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		TravelCost costCalc = new TollTravelCostCalculator(timeCostCalc, toll); // we use freespeedTravelCosts as base costs

		PreProcessLandmarks commonRouterData = new PreProcessLandmarks(timeCostCalc);
		commonRouterData.run(network);

		Leg leg = ((Leg) (population.getPerson("1").getPlans().get(0).getActsLegs().get(1)));

		// 1st case: without toll, agent chooses shortest path
		new PlansCalcRouteDijkstra(network, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg.getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		new PlansCalcRouteLandmarks(network, commonRouterData, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg.getRoute());

		// 2nd case: with a low toll, agent still chooses shortest path and pay the toll
		Cost morningCost = toll.addCost(6*3600, 10*3600, 0.03);
		clearRoutes(population);
		new PlansCalcRouteDijkstra(network, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg.getRoute());
		toll.removeCost(morningCost);
		// also test it with A*-Landmarks
		clearRoutes(population);
		new PlansCalcRouteLandmarks(network, commonRouterData, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 4 5", (CarRoute) leg.getRoute());

		// 3rd case: with a higher toll, agent decides to drive around tolled link
		toll.addCost(6*3600, 10*3600, 0.035);
		clearRoutes(population);
		new PlansCalcRouteDijkstra(network, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 3 4 5", (CarRoute) leg.getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		new PlansCalcRouteLandmarks(network, commonRouterData, costCalc, timeCostCalc).run(population);
		Fixture.compareRoutes("1 2 3 4 5", (CarRoute) leg.getRoute());
	}

	/**
	 * Clears all routes from all legs of all persons in the given population to make sure they are calculated from new.
	 *
	 * @param population
	 */
	private void clearRoutes(final Population population) {
		for (Person person : population) {
			for (Plan plan : person.getPlans()) {
				for (LegIterator i = plan.getIteratorLeg(); i.hasNext(); ) {
					BasicLeg leg = i.next();
					leg.setRoute(null);
				}
			}
		}
	}
}
