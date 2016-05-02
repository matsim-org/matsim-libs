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

package org.matsim.roadpricing;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.roadpricing.RoadPricingSchemeImpl.Cost;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests {@link PlansCalcRouteWithTollOrNot} as isolated as possible.
 *
 * @author mrieser
 */

public class PlansCalcRouteWithTollOrNotTest {
	private static final Logger log = Logger.getLogger( PlansCalcRouteWithTollOrNotTest.class );

	@Rule
	public MatsimTestUtils matsimTestUtils = new MatsimTestUtils();

	/**
	 * Tests a few cases where the router can decide if it is better to pay the toll or not.
	 */
	@Test
	public void testBestAlternatives() {
		Config config = matsimTestUtils.loadConfig(null);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		Fixture.createNetwork2(scenario);

		log.warn( "access/egress?" + config.plansCalcRoute().isInsertingAccessEgressWalk() );

		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = new RoadPricingSchemeImpl();
		toll.setType("area");
		toll.addLink(Id.createLinkId("5"));
		toll.addLink(Id.createLinkId("11"));
		Cost morningCost = toll.addCost(6 * 3600, 10 * 3600, 0.12);
		/* Start with a rather low toll. The toll is also so low, because we only
		 * have small network with short links: the cost to travel across one link
		 * is: 20s * (-6 EUR / h) = 20 * (-6) / 3600 = 0.03333
		 */

		Fixture.createPopulation2(scenario);
		Population population = scenario.getPopulation();

		Id id1 = Id.createPersonId("1");

		// case 1: toll only in morning, it is cheaper to drive around
		log.warn( "access/egress?" + config.plansCalcRoute().isInsertingAccessEgressWalk() );
		runOnAll(testee(scenario, toll), population);
		log.warn( "access/egress?" + config.plansCalcRoute().isInsertingAccessEgressWalk() );
		Fixture.compareRoutes("2 3 4 6", (NetworkRoute) getLeg1(config, population, id1).getRoute());
		Fixture.compareRoutes("8 11 12", (NetworkRoute) getLeg3(config, population, id1).getRoute());

		// case 2: now add a toll in the afternoon too, so it is cheaper to pay the toll
		Cost afternoonCost = toll.addCost(14*3600, 18*3600, 0.12);
		log.warn( "access/egress? " + config.plansCalcRoute().isInsertingAccessEgressWalk() );
		runOnAll(testee(scenario, toll), population);
		log.warn( "access/egress? " + config.plansCalcRoute().isInsertingAccessEgressWalk() );
		Fixture.compareRoutes("2 5 6", (NetworkRoute) getLeg1(config, population, id1).getRoute());
		Fixture.compareRoutes("8 11 12", (NetworkRoute) getLeg3(config, population, id1).getRoute());

		// case 3: change the second leg to a non-car mode, than it should be the same as case 1
		String oldMode = getLeg3(config, population, id1).getMode();
		getLeg3(config, population, id1).setMode(TransportMode.pt);
		runOnAll(testee(scenario, toll), population);
		Fixture.compareRoutes("2 3 4 6", (NetworkRoute) getLeg1(config, population, id1).getRoute());
		// and change the mode back
		getLeg3(config, population, id1).setMode(oldMode);

		// case 4: now remove the costs and add them again, but with a higher amount
		toll.removeCost(morningCost);
		toll.removeCost(afternoonCost);
		toll.addCost(6*3600, 10*3600, 0.7);
		toll.addCost(14*3600, 18*3600, 0.7);
		// the agent should now decide to drive around
		runOnAll(testee(scenario, toll), population);
		Fixture.compareRoutes("2 3 4 6", (NetworkRoute) getLeg1(config, population, id1).getRoute());
	}

	private static Leg getLeg1(Config config, Population population, Id id1) {
		final List<PlanElement> planElements = population.getPersons().get(id1).getPlans().get(0).getPlanElements();
		for ( PlanElement pe : planElements ) {
			log.warn( pe );
		}
		if ( !config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
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
		return Injector.createInjector(
				scenario.getConfig(),
				new ControlerDefaultsWithRoadPricingModule(toll),
				new ScenarioByInstanceModule(scenario),
				new ControlerDefaultCoreListenersModule(),
				new NewControlerModule())
				.getInstance(PlansCalcRouteWithTollOrNot.class);
	}

	/**
	 * Tests cases where the agent must pay the toll because one of its activities is on a tolled link
	 */
	@Test
	public void testTolledActLink() {
		Config config = matsimTestUtils.loadConfig(null);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		Fixture.createNetwork2(scenario);

		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = new RoadPricingSchemeImpl();
		toll.setType("area");
		Id.createLinkId("7");
		toll.addCost(6*3600, 10*3600, 0.06);

		Fixture.createPopulation2(scenario);
		Population population = scenario.getPopulation();

		runOnAll(testee(scenario, toll), population);
		Id id1 = Id.createPersonId("1");

		Fixture.compareRoutes("2 5 6", (NetworkRoute) getLeg1(config, population, id1).getRoute()); // agent should take shortest route
		Fixture.compareRoutes("8 11 12", (NetworkRoute) getLeg3(config, population, id1).getRoute());
	}

	/**
	 * Tests cases where the agent must pay the toll because all alternative routes from one location
	 * to the next include tolled links
	 */
	@Test
	public void testAllAlternativesTolled() {
		Config config = matsimTestUtils.loadConfig(null);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		Fixture.createNetwork2(scenario);

		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = new RoadPricingSchemeImpl();
		toll.setType("area");
		toll.addLink(Id.createLinkId("3"));
		toll.addLink(Id.createLinkId("5"));
		toll.addCost(6*3600, 10*3600, 0.06);

		Fixture.createPopulation2(scenario);
		Population population = scenario.getPopulation();


		runOnAll(testee(scenario, toll), population);
		Id id1 = Id.createPersonId("1");

		Fixture.compareRoutes("2 5 6", (NetworkRoute) getLeg1(config, population, id1).getRoute()); // agent should take shortest route
		Fixture.compareRoutes("8 11 12", (NetworkRoute) getLeg3(config, population, id1).getRoute());
	}

	private static Leg getLeg3(Config config, Population population, Id id1) {
		List<PlanElement> planElements = population.getPersons().get(id1).getPlans().get(0).getPlanElements() ;
		if ( !config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			return (Leg) (planElements.get(3));
		} else {
			StageActivityTypes adHocTypes = new StageActivityTypes(){
				@Override public boolean isStageActivity(String activityType) {
					if ( activityType.contains("interaction") ) {
						return true ;
					} else {
						return false ;
					}
				}
			} ;
			List<Trip> trips = TripStructureUtils.getTrips(planElements, adHocTypes) ;
			List<Leg> legs = trips.get(1).getLegsOnly() ;
			if ( legs.size()==1 ) {
				return legs.get(0) ;
			} else {
				return legs.get(1) ;
			}
		}
	}

	@Test
	public void testOutsideTollTime() {
		Config config = matsimTestUtils.loadConfig(null);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		Fixture.createNetwork2(scenario);

		// a basic toll where only the morning hours are tolled
		RoadPricingSchemeImpl toll = new RoadPricingSchemeImpl();
		toll.setType("area");
		toll.addLink(Id.createLinkId("5"));
		toll.addLink(Id.createLinkId("11"));
		toll.addCost(8*3600, 10*3600, 1.0); // high costs!

		Fixture.createPopulation2(scenario);
		Population population = scenario.getPopulation();


		runOnAll(testee(scenario, toll), population);
		Id id1 = Id.createPersonId("1");
		Leg leg1 = getLeg1(config, population, id1);
		Leg leg2 = getLeg3(config, population, id1);

		Fixture.compareRoutes("2 5 6", (NetworkRoute) leg1.getRoute()); // agent should take shortest route, as tolls are not active at that time
		Fixture.compareRoutes("8 11 12", (NetworkRoute) leg2.getRoute());
	}

}
