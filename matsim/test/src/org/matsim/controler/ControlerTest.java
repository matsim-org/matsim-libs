/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.controler;

import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.config.Module;
import org.matsim.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Plans;
import org.matsim.population.Route;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.shared.Coord;

public class ControlerTest extends MatsimTestCase {

	/**
	 * Tests that the travel times are correctly calculated during the simulation.
	 */
	public void testTravelTimeCalculation() {

		Config config = loadConfig(null);

		/* Create a simple network with 4 nodes and 3 links:
		 *
		 * (1)---1---(2)-----------2-------------(3)---3---(4)
		 *
		 * Link 2 has a capacity of 1veh/100secs, links 1 and 3 of 2veh/1sec.
		 * This way, 2 vehicles can start at the same time on link 2, of which
		 * one can leave link 2 without waiting, while the other one has to
		 * wait an additional 100sec. Given a free speed travel time of 100sec,
		 * the average travel time on that link should be 150sec for the two cars
		 * (one having 100secs, the other having 200secs to cross the link).
		 */
		NetworkLayer network = new NetworkLayer();
		Gbl.createWorld().setNetworkLayer(network);
		network.setCapacityPeriod("01:00:00");
		Node node1 = network.createNode(new IdImpl(1), new Coord(-100.0, 0.0));
		Node node2 = network.createNode(new IdImpl(2), new Coord(0.0, 0.0));
		Node node3 = network.createNode(new IdImpl(3), new Coord(1000.0, 0.0));
		Node node4 = network.createNode(new IdImpl(4), new Coord(1100.0, 0.0));
		network.createLink(new IdImpl(1), node1, node2, 100, 10, 7200, 1);
		Link link2 = network.createLink(new IdImpl(2), node2, node3, 1000, 10, 36, 1);
		network.createLink(new IdImpl(3), node3, node4, 100, 10, 7200, 1);

		/* Create 2 persons driving from link 1 to link 3, both starting at the
		 * same time at 7am.  */
		Plans population = new Plans(Plans.NO_STREAMING);
		Person person1 = null;
		try {
			person1 = new Person(new IdImpl(1));
			Plan plan1 = person1.createPlan(true);
			plan1.createAct("h", (String)null, null, "1", "00:00:00", "07:00:00", "07:00:00", "no");
			Leg leg1 = plan1.createLeg("car", "07:00:00", "00:00:00", null);
			Route route1 = leg1.createRoute(null, null);
			route1.setRoute("2 3");
			plan1.createAct("h", (String)null, null, "3", "07:00:00", null, null, "no");
			population.addPerson(person1);

			Person person2 = new Person(new IdImpl(2));
			Plan plan2 = person2.createPlan(true);
			plan2.createAct("h", (String)null, null, "1", "00:00:00", "07:00:00", "07:00:00", "no");
			Leg leg2 = plan2.createLeg("car", "07:00:00", "00:00:00", null);
			Route route2 = leg2.createRoute(null, null);
			route2.setRoute("2 3");
			plan2.createAct("h", (String)null, null, "3", "07:00:00", null, null, "no");
			population.addPerson(person2);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Complete the configuration for our test case
		// - set scoring parameters
		ActivityParams actParams = new ActivityParams("h");
		actParams.setTypicalDuration(8*3600);
		actParams.setPriority(1.0);
		config.charyparNagelScoring().addActivityParams(actParams);
		// - define iterations
		config.controler().setLastIteration(0);
		// - make sure we don't use threads, as they are not deterministic
		config.global().setNumberOfThreads(0);

		// Now run the simulation
		Controler controler = new Controler(config, network, population);
		controler.setCreateGraphs(false);
		controler.run();

		// test if we got the right result
		// the actual result is 151sec, not 150, as each vehicle "loses" 1sec in the buffer
		assertEquals("TravelTimeCalculator has wrong result",
				151.0, controler.getLinkTravelTimes().getLinkTravelTime(link2, 7*3600), 0.0);

		// now test that the ReRoute-Strategy also knows about these travel times...
		config.controler().setLastIteration(1);
		Module strategyParams = config.getModule("strategy");
		strategyParams.addParam("maxAgentPlanMemorySize", "4");
		strategyParams.addParam("ModuleProbability_1", "1.0");
		strategyParams.addParam("Module_1", "ReRoute");
		// Run the simulation again
		controler = new Controler(config, network, population);
		controler.setCreateGraphs(false);
		controler.setOverwriteFiles(true);
		controler.run();

		// test that the plans have the correct times
		assertEquals("ReRoute seems to have wrong travel times.",
				151.0, ((Leg) (person1.getPlans().get(1).getActsLegs().get(1))).getTravTime(), 0.0);
	}

	/**
	 * Tests that a custom scoring function factory doesn't get overwritten
	 * in the initialization process of the Controler.
	 */
	public void testSetScoringFunctionFactory() {
		final Config config = loadConfig(null);
		config.controler().setLastIteration(0);

		// create a very simple network with one link only and an empty population
		NetworkLayer network = new NetworkLayer();
		Node node1 = network.createNode(new IdImpl(1), new Coord(0, 0));
		Node node2 = network.createNode(new IdImpl(2), new Coord(100, 0));
		network.createLink(new IdImpl(1), node1, node2, 100, 1, 3600, 1);
		Plans population = new Plans(Plans.NO_STREAMING);

		final Controler controler = new Controler(config, network, population);
		controler.setCreateGraphs(false);
		controler.setScoringFunctionFactory(new DummyScoringFunctionFactory());
		assertTrue("Custom ScoringFunctionFactory was not set.",
				controler.getScoringFunctionFactory() instanceof DummyScoringFunctionFactory);

		controler.run();

		assertTrue("Custom ScoringFunctionFactory got overwritten.",
				controler.getScoringFunctionFactory() instanceof DummyScoringFunctionFactory);
	}

	/**
	 * Tests that plans with missing routes are completed (=routed) before the mobsim starts.
	 */
	public void testCalcMissingRoutes() {
		Config config = loadConfig(null);

		/* Create a simple network with 4 nodes and 3 links:
		 *
		 * (1)---1---(2)-----------2-------------(3)---3---(4)
		 *
		 * Link 2 has a capacity of 1veh/100secs, links 1 and 3 of 2veh/1sec.
		 * This way, 2 vehicles can start at the same time on link 2, of which
		 * one can leave link 2 without waiting, while the other one has to
		 * wait an additional 100sec. Given a free speed travel time of 100sec,
		 * the average travel time on that link should be 150sec for the two cars
		 * (one having 100secs, the other having 200secs to cross the link).
		 */
		NetworkLayer network = new NetworkLayer();
		Gbl.createWorld().setNetworkLayer(network);
		network.setCapacityPeriod("01:00:00");
		Node node1 = network.createNode(new IdImpl(1), new Coord(-100.0, 0.0));
		Node node2 = network.createNode(new IdImpl(2), new Coord(0.0, 0.0));
		Node node3 = network.createNode(new IdImpl(3), new Coord(1000.0, 0.0));
		Node node4 = network.createNode(new IdImpl(4), new Coord(1100.0, 0.0));
		network.createLink(new IdImpl(1), node1, node2,  100, 10, 7200, 1);
		network.createLink(new IdImpl(2), node2, node3, 1000, 10,   36, 1);
		network.createLink(new IdImpl(3), node3, node4,  100, 10, 7200, 1);

		/* Create a person with two plans, driving from link 1 to link 3, starting at 7am.  */
		Plans population = new Plans(Plans.NO_STREAMING);
		Person person1 = null;
		Leg leg1 = null;
		Leg leg2 = null;
		try {
			person1 = new Person(new IdImpl(1));
			// --- plan 1 ---
			Plan plan1 = person1.createPlan(true);
			plan1.createAct("h", (String)null, null, "1", "00:00:00", "07:00:00", "07:00:00", "no");
			leg1 = plan1.createLeg("car", "07:00:00", "00:00:00", null);
			// DO NOT CREATE A ROUTE FOR THE LEG!!!
			plan1.createAct("h", (String)null, null, "3", "07:00:00", null, null, "no");
			// --- plan 2 ---
			Plan plan2 = person1.createPlan(true);
			plan2.createAct("h", (String)null, null, "1", "00:00:00", "07:00:00", "07:00:00", "no");
			leg2 = plan2.createLeg("car", "07:00:00", "00:00:00", null);
			// DO NOT CREATE A ROUTE FOR THE LEG!!!
			plan2.createAct("h", (String)null, null, "3", "07:00:00", null, null, "no");
			population.addPerson(person1);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Complete the configuration for our test case
		// - set scoring parameters
		ActivityParams actParams = new ActivityParams("h");
		actParams.setTypicalDuration(8*3600);
		actParams.setPriority(1.0);
		config.charyparNagelScoring().addActivityParams(actParams);
		// - define iterations
		config.controler().setLastIteration(0);
		// - make sure we don't use threads, as they are not deterministic
		config.global().setNumberOfThreads(1);

		// Now run the simulation
		Controler controler = new Controler(config, network, population);
		controler.setCreateGraphs(false);
		controler.run();
		/* if something goes wrong, there will be an exception we don't catch and the test fails,
		 * otherwise, everything is fine. */

		// check that BOTH plans have a route set, even when we only run 1 iteration where only one of them is used.
		assertNotNull(leg1.getRoute());
		assertNotNull(leg2.getRoute());
	}

	/**
	 * Tests that plans with missing act locations are completed (=xy2links and routed) before the mobsim starts.
	 */
	public void testCalcMissingActLinks() {
		Config config = loadConfig(null);

		/* Create a simple network with 4 nodes and 3 links:
		 *
		 * (1)---1---(2)-----------2-------------(3)---3---(4)
		 *
		 * Link 2 has a capacity of 1veh/100secs, links 1 and 3 of 2veh/1sec.
		 * This way, 2 vehicles can start at the same time on link 2, of which
		 * one can leave link 2 without waiting, while the other one has to
		 * wait an additional 100sec. Given a free speed travel time of 100sec,
		 * the average travel time on that link should be 150sec for the two cars
		 * (one having 100secs, the other having 200secs to cross the link).
		 */
		NetworkLayer network = new NetworkLayer();
		Gbl.createWorld().setNetworkLayer(network);
		network.setCapacityPeriod("01:00:00");
		Node node1 = network.createNode(new IdImpl(1), new Coord(-100.0, 0.0));
		Node node2 = network.createNode(new IdImpl(2), new Coord(0.0, 0.0));
		Node node3 = network.createNode(new IdImpl(3), new Coord(+1000.0, 0.0));
		Node node4 = network.createNode(new IdImpl(4), new Coord(+1100.0, 0.0));
		Link link1 = network.createLink(new IdImpl(1), node1, node2, 100, 10, 7200, 1);
		network.createLink(new IdImpl(2), node2, node3, 1000, 10, 36, 1);
		Link link3 = network.createLink(new IdImpl(3), node3, node4,  100, 10, 7200, 1);

		/* Create a person with two plans, driving from link 1 to link 3, starting at 7am.  */
		Plans population = new Plans(Plans.NO_STREAMING);
		Person person1 = null;
		Act act1a = null;
		Act act1b = null;
		Act act2a = null;
		Act act2b = null;
		Leg leg1 = null;
		Leg leg2 = null;
		try {
			person1 = new Person(new IdImpl(1));
			// --- plan 1 ---
			Plan plan1 = person1.createPlan(true);
			act1a = plan1.createAct("h", "-50.0", "10.0", null, "00:00:00", "07:00:00", "07:00:00", "no");
			leg1 = plan1.createLeg("car", "07:00:00", "00:00:00", null);
			// DO NOT CREATE A ROUTE FOR THE LEG!!!
			act1b = plan1.createAct("h", "1075.0", "-10.0", null, "07:00:00", null, null, "no");
			// --- plan 2 ---
			Plan plan2 = person1.createPlan(true);
			act2a = plan2.createAct("h", "-50.0", "-10.0", null, "00:00:00", "07:00:00", "07:00:00", "no");
			leg2 = plan2.createLeg("car", "07:00:00", "00:00:00", null);
			// DO NOT CREATE A ROUTE FOR THE LEG!!!
			act2b = plan2.createAct("h", "1111.1", "10.0", null, "07:00:00", null, null, "no");
			population.addPerson(person1);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		// Complete the configuration for our test case
		// - set scoring parameters
		ActivityParams actParams = new ActivityParams("h");
		actParams.setTypicalDuration(8*3600);
		actParams.setPriority(1.0);
		config.charyparNagelScoring().addActivityParams(actParams);
		// - define iterations
		config.controler().setLastIteration(0);
		// - make sure we don't use threads, as they are not deterministic
		config.global().setNumberOfThreads(1);

		// Now run the simulation
		Controler controler = new Controler(config, network, population);
		controler.setCreateGraphs(false);
		controler.run();
		/* if something goes wrong, there will be an exception we don't catch and the test fails,
		 * otherwise, everything is fine. */

		// check that BOTH plans have their act-locations calculated
		assertEquals(link1, act1a.getLink());
		assertEquals(link3, act1b.getLink());
		assertEquals(link1, act2a.getLink());
		assertEquals(link3, act2b.getLink());

		// check that BOTH plans have a route set, even when we only run 1 iteration where only one of them is used.
		assertNotNull(leg1.getRoute());
		assertNotNull(leg2.getRoute());
	}

	/** A helper class for testSetScoringFunctionFactory() */
	private static class DummyScoringFunctionFactory implements ScoringFunctionFactory {
		public DummyScoringFunctionFactory() {
			/* empty public constructor for private inner class */
		}

		public ScoringFunction getNewScoringFunction(final Plan plan) {
			return new CharyparNagelScoringFunction(plan);
		}

	}

}
