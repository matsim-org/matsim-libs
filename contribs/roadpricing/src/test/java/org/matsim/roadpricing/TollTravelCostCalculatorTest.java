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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.ControlerDefaults;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.testcases.MatsimTestUtils;

import javax.inject.Provider;

/**
 * Tests the correct working of {@link TravelDisutilityIncludingToll} by using it
 * to calculate some routes with {@link PlansCalcRoute}.
 *
 * @author mrieser
 */
public class TollTravelCostCalculatorTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testDisutilityResults() {
		Config config = ConfigUtils.createConfig() ;

		Scenario scenario = ScenarioUtils.createScenario(config) ;
		Fixture.createNetwork2((MutableScenario)scenario);
		Network net = scenario.getNetwork() ;
		
		RoadPricingSchemeImpl scheme = new RoadPricingSchemeImpl();
		scheme.setType(RoadPricingScheme.TOLL_TYPE_DISTANCE);
		final Id<Link> link5Id = Id.create("5", Link.class);
		scheme.addLink(link5Id);
		final Id<Link> link11Id = Id.create("11", Link.class);
		scheme.addLink(link11Id);
		scheme.addCost(0, 10*3600, 0.0007); 


		TravelTime timeCalculator = new FreespeedTravelTimeAndDisutility(config.planCalcScore());

		double margUtlOfMoney = 1. ;
        final TravelDisutilityFactory defaultDisutilityFactory = ControlerDefaults.createDefaultTravelDisutilityFactory(scenario);
        
		RoadPricingTravelDisutilityFactory travelDisutilityFactory = new RoadPricingTravelDisutilityFactory(
				defaultDisutilityFactory, scheme, margUtlOfMoney );
//        travelDisutilityFactory.setSigma( 0. ) ;
		TravelDisutility travelDisutility = travelDisutilityFactory.createTravelDisutility(timeCalculator ) ;
		
		PopulationFactory pf = scenario.getPopulation().getFactory() ;
		
		double[] results = {0.06678174427374567,0.06723862746529602,0.06666671672366475,0.09362901704565804,0.06701542218998154,
				0.06668312397557824,0.06750047010988,0.06669740530590448,0.4069154594253435} ;
		double delta = Double.MIN_VALUE * 100. ;
		
		for ( int ii=0 ; ii<results.length ; ii++ ) {
			Person person = pf.createPerson( Id.createPersonId(ii)  ) ;

			{
				double value = travelDisutility.getLinkTravelDisutility(net.getLinks().get(link5Id),10., person, null ) ;
				System.out.println( "value=" + value ) ;
//				Assert.assertEquals( results[ii], value, delta);
			}
			{
				double value = travelDisutility.getLinkTravelDisutility(net.getLinks().get(link11Id), 10, person, null ) ;
				System.out.println( "value=" + value ) ;
//				Assert.assertEquals( results[ii], value, delta);
			}
		}
		
	}

	@Test
	public void testDistanceTollRouter() {
		Config config = utils.loadConfig(null);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Fixture.createNetwork2(scenario);
		Network network = scenario.getNetwork();
		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = new RoadPricingSchemeImpl();
		toll.setType(RoadPricingScheme.TOLL_TYPE_DISTANCE);
		toll.addLink(Id.create("5", Link.class));
		toll.addLink(Id.create("11", Link.class));
		Fixture.createPopulation2(scenario);
		Population population = scenario.getPopulation();
		TravelTime timeCalc = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
		// yy note: this returns a combined TravelTime and TravelDisutility object.  The TravelDisutility object is used in the next three lines to be wrapped, 
		// and then never again.  Would be nice to be able to get them separately ...  kai, oct'13
		
		TravelDisutility costCalc = new TravelDisutilityIncludingToll((TravelDisutility)timeCalc, toll, config); // we use freespeedTravelCosts as base costs

		AStarLandmarksFactory aStarLandmarksFactory = new AStarLandmarksFactory(network, (TravelDisutility)timeCalc);

		PreProcessLandmarks commonRouterData = new PreProcessLandmarks((TravelDisutility)timeCalc);
		commonRouterData.run(network);
		
		int carLegIndex = 1 ;
		if ( config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			carLegIndex = 3 ;
		}

		Person person1 = population.getPersons().get(Id.create("1", Person.class));
		// 1st case: without toll, agent chooses shortest path
		routePopulation(
				scenario,
				new DijkstraFactory(),
				timeCalc,
				costCalc );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		assertNull(((LegImpl) (person1.getPlans().get(0).getPlanElements().get(1))).getRoute()); // make sure the cleaning worked. we do this only once, then we believe it.
		routePopulation(
				scenario,
				aStarLandmarksFactory,
				timeCalc,
				costCalc );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());

		Cost morningCost = toll.addCost(6*3600, 10*3600, 0.0006); // 0.0006 * link_length(100m) = 0.06, which is slightly below the threshold of 0.0666
		// 2nd case: with a low toll, agent still chooses shortest path
		clearRoutes(population);
		routePopulation(
				scenario,
				new DijkstraFactory(),
				timeCalc,
				costCalc );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		routePopulation(
				scenario,
				aStarLandmarksFactory,
				timeCalc,
				costCalc );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());

		// 3rd case: with a higher toll, agent decides to drive around tolled link
		toll.removeCost(morningCost);
		toll.addCost(6*3600, 10*3600, 0.0007); // new morning toll, this should be slightly over the threshold
		clearRoutes(population);
		routePopulation(
				scenario,
				new DijkstraFactory(),
				timeCalc,
				costCalc );
		Fixture.compareRoutes("2 3 4 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		routePopulation(
				scenario,
				aStarLandmarksFactory,
				timeCalc,
				costCalc );
		Fixture.compareRoutes("2 3 4 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());
	}

	@Test
	public void testLinkTollRouter() {
		Config config = utils.loadConfig(null);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Fixture.createNetwork2(scenario);
		Network network = scenario.getNetwork();
		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = new RoadPricingSchemeImpl();
		toll.setType(RoadPricingScheme.TOLL_TYPE_LINK);
		toll.addLink(Id.create("5", Link.class));
		toll.addLink(Id.create("11", Link.class));
		Fixture.createPopulation2(scenario);
		Population population = scenario.getPopulation();
		FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
		TravelDisutility costCalc = new TravelDisutilityIncludingToll(timeCostCalc, toll, config); // we use freespeedTravelCosts as base costs

		AStarLandmarksFactory routerFactory = new AStarLandmarksFactory(network, timeCostCalc);

		PreProcessLandmarks commonRouterData = new PreProcessLandmarks(timeCostCalc);
		commonRouterData.run(network);
		
		int carLegIndex = 1 ;
		if ( config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			carLegIndex = 3 ;
		}

		Person person1 = population.getPersons().get(Id.create("1", Person.class));
		// 1st case: without toll, agent chooses shortest path
		routePopulation(
				scenario,
				new DijkstraFactory(),
				timeCostCalc,
				costCalc );
		final List<PlanElement> planElements = person1.getPlans().get(0).getPlanElements();
		System.err.println( "\n1st: " ) ;
		for ( PlanElement pe : planElements ) {
			System.err.println( pe );
		}
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((Leg) (planElements.get(carLegIndex))).getRoute());

		// also test it with A*-Landmarks
		clearRoutes(population);
//		assertNull(((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute()); // make sure the cleaning worked. we do this only once, then we believe it.
		routePopulation(
				scenario,
				routerFactory,
				timeCostCalc,
				costCalc );
		final List<PlanElement> planElements2 = person1.getPlans().get(0).getPlanElements();
		System.err.println( "\n2nd: " );
		for ( PlanElement pe : planElements2 ) {
			System.err.println( pe );
		}
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((Leg) (planElements2.get(carLegIndex))).getRoute());

		Cost morningCost = toll.addCost(6*3600, 10*3600, 0.06); // 0.06, which is slightly below the threshold of 0.0666
		// 2nd case: with a low toll, agent still chooses shortest path
		clearRoutes(population);
		routePopulation(
				scenario,
				new DijkstraFactory(),
				timeCostCalc,
				costCalc );
		final List<PlanElement> planElements3 = person1.getPlans().get(0).getPlanElements();
		System.err.println( "3rd: " + planElements3 );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((Leg) (planElements3.get(carLegIndex))).getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		routePopulation(
				scenario,
				routerFactory,
				timeCostCalc,
				costCalc );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());

		// 3rd case: with a higher toll, agent decides to drive around tolled link
		toll.removeCost(morningCost);
		toll.addCost(6*3600, 10*3600, 0.07); // new morning toll, this should be slightly over the threshold
		clearRoutes(population);
		routePopulation(
				scenario,
				new DijkstraFactory(),
				timeCostCalc,
				costCalc );
		Fixture.compareRoutes("2 3 4 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		routePopulation(
				scenario,
				routerFactory,
				timeCostCalc,
				costCalc );
		Fixture.compareRoutes("2 3 4 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());
	}
	
	@Test
	public void testCordonTollRouter() {
		Config config = utils.loadConfig(null);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Fixture.createNetwork2(scenario);
		Network network = scenario.getNetwork();
		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = new RoadPricingSchemeImpl();
		toll.setType(RoadPricingScheme.TOLL_TYPE_CORDON);
		toll.addLink(Id.create("5", Link.class));
		toll.addLink(Id.create("11", Link.class));
		Fixture.createPopulation2(scenario);
		Population population = scenario.getPopulation();
		FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(config.planCalcScore());
		TravelDisutility costCalc = new TravelDisutilityIncludingToll(timeCostCalc, toll, config); // we use freespeedTravelCosts as base costs

		AStarLandmarksFactory routerFactory = new AStarLandmarksFactory(network, timeCostCalc);

		int carLegIndex = 1 ;
		if ( config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			carLegIndex = 3 ;
		}

		Person person1 = population.getPersons().get(Id.create("1", Person.class));
		// 1st case: without toll, agent chooses shortest path
		routePopulation(
				scenario,
				new DijkstraFactory(),
				timeCostCalc,
				costCalc );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		routePopulation(
				scenario,
				routerFactory,
				timeCostCalc,
				costCalc );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());

		// 2nd case: with a low toll, agent still chooses shortest path and pay the toll
		Cost morningCost = toll.addCost(6*3600, 10*3600, 0.06);
		clearRoutes(population);
		routePopulation(
				scenario,
				new DijkstraFactory(),
				timeCostCalc,
				costCalc );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());
		toll.removeCost(morningCost);
		// also test it with A*-Landmarks
		clearRoutes(population);
		routePopulation(
				scenario,
				routerFactory,
				timeCostCalc,
				costCalc );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());

		// 3rd case: with a higher toll, agent decides to drive around tolled link
		toll.addCost(6*3600, 10*3600, 0.067);
		clearRoutes(population);
		routePopulation(
				scenario,
				new DijkstraFactory(),
				timeCostCalc,
				costCalc );
		Fixture.compareRoutes("2 3 4 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());
		// also test it with A*-Landmarks
		clearRoutes(population);
		routePopulation(
				scenario,
				routerFactory,
				timeCostCalc,
				costCalc );
		Fixture.compareRoutes("2 3 4 6", (NetworkRoute) ((LegImpl) (person1.getPlans().get(0).getPlanElements().get(carLegIndex))).getRoute());
	}

	/**
	 * Clears all routes from all legs of all persons in the given population to make sure they are calculated from new.
	 *
	 * @param population
	 */
	private static void clearRoutes(final Population population) {
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement pe : plan.getPlanElements()) {
					if (pe instanceof Leg) {
						((Leg) pe).setRoute(null);
					}
				}
			}
		}
	}

	private static void routePopulation(
			final Scenario scenario,
			final LeastCostPathCalculatorFactory routerFactory,
			final TravelTime travelTime,
			final TravelDisutility travelDisutility ) {
		assertNotNull(routerFactory);

		final TripRouterFactoryBuilderWithDefaults builder = new TripRouterFactoryBuilderWithDefaults();
		builder.setLeastCostPathCalculatorFactory( routerFactory );
		builder.setTravelDisutility(travelDisutility);
		builder.setTravelTime(travelTime);
		final Provider<TripRouter> factory = builder.build( scenario );
		final TripRouter tripRouter = factory.get( );
		final PersonAlgorithm router = new PlanRouter( tripRouter );

		for ( Person p : scenario.getPopulation().getPersons().values() ) {
			router.run( p );
		}
	}
}
