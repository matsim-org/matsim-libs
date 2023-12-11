/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PlansCalcRouteWithTollOrNotTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.roadpricing;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Provider;

/**
 * Tests {@link PlansCalcRouteWithTollOrNot} as isolated as possible.
 *
 * @author mrieser
 */

public class PlansCalcRouteWithTollOrNotTest {
	private static final Logger log = LogManager.getLogger( PlansCalcRouteWithTollOrNotTest.class );

	@RegisterExtension
	private MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	/**
	 * Tests a few cases where the router can decide if it is better to pay the
	 * toll or not.
	 */
	@Test
	void testBestAlternatives() {
		Config config = matsimTestUtils.createConfig();
		config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		RoadPricingTestUtils.createNetwork2(scenario);

		log.warn( "access/egress?" + config.routing().getAccessEgressType() );

		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );
		toll.setType("area");
		toll.addLink(Id.createLinkId("5"));
		toll.addLink(Id.createLinkId("11"));
		Cost morningCost = toll.createAndAddCost(6 * 3600, 10 * 3600, 0.12);
		/* Start with a rather low toll. The toll is also so low, because we only
		 * have small network with short links: the cost to travel across one link
		 * is: 20s * (-6 EUR / h) = 20 * (-6) / 3600 = 0.03333
		 */

		RoadPricingTestUtils.createPopulation2(scenario);
		Population population = scenario.getPopulation();

		Id<Person> id1 = Id.createPersonId("1");

		// case 1: toll only in morning, it is cheaper to drive around
		log.warn( "access/egress?" + config.routing().getAccessEgressType() );
		runOnAll(testee(scenario, toll), population);
		log.warn( "access/egress?" + config.routing().getAccessEgressType() );
		RoadPricingTestUtils.compareRoutes("2 3 4 6", (NetworkRoute) getLeg1(config, population, id1).getRoute());
		RoadPricingTestUtils.compareRoutes("8 11 12", (NetworkRoute) getLeg3(config, population, id1).getRoute());

		// case 2: now add a toll in the afternoon too, so it is cheaper to pay the toll
		Cost afternoonCost = toll.createAndAddCost(14*3600, 18*3600, 0.12);
		log.warn( "access/egress? " + config.routing().getAccessEgressType() );
		runOnAll(testee(scenario, toll), population);
		log.warn( "access/egress? " + config.routing().getAccessEgressType() );
		RoadPricingTestUtils.compareRoutes("2 5 6", (NetworkRoute) getLeg1(config, population, id1).getRoute());
		RoadPricingTestUtils.compareRoutes("8 11 12", (NetworkRoute) getLeg3(config, population, id1).getRoute());

		// case 3: change the second leg to a non-car mode, than it should be the same as case 1
		String oldLegMode = getLeg3(config, population, id1).getMode();
		String oldRoutingMode = TripStructureUtils.getRoutingMode(getLeg3(config, population, id1));
		Leg leg = getLeg3(config, population, id1);
//		leg.setMode(TransportMode.pt);
		leg.setRoute(null);
		TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
		runOnAll(testee(scenario, toll), population);
		RoadPricingTestUtils.compareRoutes("2 3 4 6", (NetworkRoute) getLeg1(config, population, id1).getRoute());
		// and change the mode back
		Leg leg3 = getLeg3(config, population, id1);
		leg3.setMode(oldLegMode);
		TripStructureUtils.setRoutingMode(leg3, oldRoutingMode);

		// case 4: now remove the costs and add them again, but with a higher amount
		toll.removeCost(morningCost);
		toll.removeCost(afternoonCost);
		toll.createAndAddCost(6*3600, 10*3600, 0.7);
		toll.createAndAddCost(14*3600, 18*3600, 0.7);
		// the agent should now decide to drive around
		runOnAll(testee(scenario, toll), population);
		RoadPricingTestUtils.compareRoutes("2 3 4 6", (NetworkRoute) getLeg1(config, population, id1).getRoute());
	}

	private static Leg getLeg1(Config config, Population population, Id<Person> id1) {
		final List<PlanElement> planElements = population.getPersons().get(id1).getPlans().get(0).getPlanElements();
		for ( PlanElement pe : planElements ) {
			log.warn( pe );
		}
		if ( config.routing().getAccessEgressType().equals(AccessEgressType.none) ) {
			return (Leg) (planElements.get(1));
		} else {
			return (Leg) (planElements.get(3));
		}
	}

	private static void runOnAll(PlanAlgorithm testee, Population population) {
		for (Person p : population.getPersons().values()) {
			testee.run(p.getSelectedPlan());
		}
	}

	private PlansCalcRouteWithTollOrNot testee(final Scenario scenario, final RoadPricingScheme toll) {
//		return Injector.createInjector(
//				scenario.getConfig(),
//				new RoadPricingModuleDefaults(toll),
//				/* FIXME Check/understand why the following is INcorrect, jwj '19. What's the difference? */
////				RoadPricingUtils.createModule(toll),
//				new ScenarioByInstanceModule(scenario),
//				new ControlerDefaultCoreListenersModule(),
//				new NewControlerModule())
//				.getInstance(PlansCalcRouteWithTollOrNot.class);

		Provider<TripRouter> tripRouterProvider = Injector.createInjector(scenario.getConfig(),
				new RoadPricingModuleDefaults(toll),
				new ScenarioByInstanceModule(scenario),
				new ControlerDefaultCoreListenersModule(),
				new NewControlerModule()).getProvider(TripRouter.class);

			return new PlansCalcRouteWithTollOrNot( toll, tripRouterProvider, TimeInterpretation.create(scenario.getConfig()) ) ;
			// yy might be more plausible to get the full class out of the injector, but that ain't that easy ...  kai, oct'19
	}

	/**
	 * Tests cases where the agent must pay the toll because one of its activities is on a tolled link
	 */
	@Test
	void testTolledActLink() {
		Config config = matsimTestUtils.createConfig();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		RoadPricingTestUtils.createNetwork2(scenario);

		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );
		toll.setType("area");
		Id.createLinkId("7");
		toll.createAndAddCost(6*3600, 10*3600, 0.06);

		RoadPricingTestUtils.createPopulation2(scenario);
		Population population = scenario.getPopulation();

		runOnAll(testee(scenario, toll), population);
		Id<Person> id1 = Id.createPersonId("1");

		RoadPricingTestUtils.compareRoutes("2 5 6", (NetworkRoute) getLeg1(config, population, id1).getRoute()); // agent should take shortest route
		RoadPricingTestUtils.compareRoutes("8 11 12", (NetworkRoute) getLeg3(config, population, id1).getRoute());
	}

	/**
	 * Tests cases where the agent must pay the toll because all alternative routes from one location
	 * to the next include tolled links
	 */
	@Test
	void testAllAlternativesTolled() {
		Config config = matsimTestUtils.createConfig();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		RoadPricingTestUtils.createNetwork2(scenario);

		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );
		toll.setType("area");
		toll.addLink(Id.createLinkId("3"));
		toll.addLink(Id.createLinkId("5"));
		toll.createAndAddCost(6*3600, 10*3600, 0.06);

		RoadPricingTestUtils.createPopulation2(scenario);
		Population population = scenario.getPopulation();


		runOnAll(testee(scenario, toll), population);
		Id<Person> id1 = Id.createPersonId("1");

		RoadPricingTestUtils.compareRoutes("2 5 6", (NetworkRoute) getLeg1(config, population, id1).getRoute()); // agent should take shortest route
		RoadPricingTestUtils.compareRoutes("8 11 12", (NetworkRoute) getLeg3(config, population, id1).getRoute());
	}

	private static Leg getLeg3(Config config, Population population, Id<Person> id1) {
		List<PlanElement> planElements = population.getPersons().get(id1).getPlans().get(0).getPlanElements() ;
		if ( config.routing().getAccessEgressType().equals(AccessEgressType.none) ) {
			return (Leg) (planElements.get(3));
		} else {
			List<Trip> trips = TripStructureUtils.getTrips(planElements) ;
			List<Leg> legs = trips.get(1).getLegsOnly() ;
			if ( legs.size()==1 ) {
				return legs.get(0) ;
			} else {
				return legs.get(1) ;
			}
		}
	}

	@Test
	void testOutsideTollTime() {
		Config config = matsimTestUtils.createConfig();
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		RoadPricingTestUtils.createNetwork2(scenario);

		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = RoadPricingUtils.addOrGetMutableRoadPricingScheme(scenario );
		toll.setType("area");
		toll.addLink(Id.createLinkId("5"));
		toll.addLink(Id.createLinkId("11"));
		toll.createAndAddCost(8*3600, 10*3600, 1.0); // high costs!

		RoadPricingTestUtils.createPopulation2(scenario);
		Population population = scenario.getPopulation();


		runOnAll(testee(scenario, toll), population);
		Id<Person> id1 = Id.createPersonId("1");
		Leg leg1 = getLeg1(config, population, id1);
		Leg leg2 = getLeg3(config, population, id1);

		RoadPricingTestUtils.compareRoutes("2 5 6", (NetworkRoute) leg1.getRoute()); // agent should take shortest route, as tolls are not active at that time
		RoadPricingTestUtils.compareRoutes("8 11 12", (NetworkRoute) leg2.getRoute());
	}

}
