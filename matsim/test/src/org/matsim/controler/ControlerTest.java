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

import java.io.File;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicLeg.Mode;
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
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.scoring.CharyparNagelScoringFunction;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;

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
		Node node1 = network.createNode(new IdImpl(1), new CoordImpl(-100.0, 0.0));
		Node node2 = network.createNode(new IdImpl(2), new CoordImpl(0.0, 0.0));
		Node node3 = network.createNode(new IdImpl(3), new CoordImpl(1000.0, 0.0));
		Node node4 = network.createNode(new IdImpl(4), new CoordImpl(1100.0, 0.0));
		Link link1 = network.createLink(new IdImpl(1), node1, node2, 100, 10, 7200, 1);
		Link link2 = network.createLink(new IdImpl(2), node2, node3, 1000, 10, 36, 1);
		Link link3 = network.createLink(new IdImpl(3), node3, node4, 100, 10, 7200, 1);

		/* Create 2 persons driving from link 1 to link 3, both starting at the
		 * same time at 7am.  */
		Population population = new Population(Population.NO_STREAMING);
		Person person1 = null;

		person1 = new PersonImpl(new IdImpl(1));
		Plan plan1 = person1.createPlan(true);
		Act a1 = plan1.createAct("h", link1);
		a1.setEndTime(7.0*3600);
		Leg leg1 = plan1.createLeg(Mode.car);
		CarRoute route1 = (CarRoute)network.getFactory().createRoute(BasicLeg.Mode.car);
		leg1.setRoute(route1);
		route1.setNodes("2 3");
		plan1.createAct("h", link3);
		population.addPerson(person1);

		Person person2 = new PersonImpl(new IdImpl(2));
		Plan plan2 = person2.createPlan(true);
		Act a2 = plan2.createAct("h", link1);
		a2.setEndTime(7.0*3600);
		Leg leg2 = plan2.createLeg(Mode.car);
		CarRoute route2 = (CarRoute)network.getFactory().createRoute(BasicLeg.Mode.car);
		leg2.setRoute(route2);
		route2.setNodes("2 3");
		plan2.createAct("h", link3);
		population.addPerson(person2);

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
		controler.setWriteEventsInterval(0);
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
		controler.setWriteEventsInterval(0);
		controler.run();

		// test that the plans have the correct times
		assertEquals("ReRoute seems to have wrong travel times.",
				151.0, ((Leg) (person1.getPlans().get(1).getActsLegs().get(1))).getTravelTime(), 0.0);
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
		Node node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl(2), new CoordImpl(100, 0));
		network.createLink(new IdImpl(1), node1, node2, 100, 1, 3600, 1);
		Population population = new Population(Population.NO_STREAMING);

		final Controler controler = new Controler(config, network, population);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
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
		Node node1 = network.createNode(new IdImpl(1), new CoordImpl(-100.0, 0.0));
		Node node2 = network.createNode(new IdImpl(2), new CoordImpl(0.0, 0.0));
		Node node3 = network.createNode(new IdImpl(3), new CoordImpl(1000.0, 0.0));
		Node node4 = network.createNode(new IdImpl(4), new CoordImpl(1100.0, 0.0));
		Link link1 = network.createLink(new IdImpl(1), node1, node2,  100, 10, 7200, 1);
		network.createLink(new IdImpl(2), node2, node3, 1000, 10,   36, 1);
		Link link3 = network.createLink(new IdImpl(3), node3, node4,  100, 10, 7200, 1);

		/* Create a person with two plans, driving from link 1 to link 3, starting at 7am.  */
		Population population = new Population(Population.NO_STREAMING);
		Person person1 = null;
		Leg leg1 = null;
		Leg leg2 = null;

		person1 = new PersonImpl(new IdImpl(1));
		// --- plan 1 ---
		Plan plan1 = person1.createPlan(true);
		Act a1 = plan1.createAct("h", link1);//(String)null, null, "1", "00:00:00", "07:00:00", "07:00:00", "no");
		a1.setEndTime(7.0*3600);
		leg1 = plan1.createLeg(Mode.car);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		plan1.createAct("h", link3);
		// --- plan 2 ---
		Plan plan2 = person1.createPlan(true);
		Act a2 = plan2.createAct("h", link1);//(String)null, null, "1", "00:00:00", "07:00:00", "07:00:00", "no");
		a2.setEndTime(7.0*3600);

		leg2 = plan2.createLeg(Mode.car);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		plan2.createAct("h", link3);
		population.addPerson(person1);

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
		controler.setWriteEventsInterval(0);
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
		Node node1 = network.createNode(new IdImpl(1), new CoordImpl(-100.0, 0.0));
		Node node2 = network.createNode(new IdImpl(2), new CoordImpl(0.0, 0.0));
		Node node3 = network.createNode(new IdImpl(3), new CoordImpl(+1000.0, 0.0));
		Node node4 = network.createNode(new IdImpl(4), new CoordImpl(+1100.0, 0.0));
		Link link1 = network.createLink(new IdImpl(1), node1, node2, 100, 10, 7200, 1);
		network.createLink(new IdImpl(2), node2, node3, 1000, 10, 36, 1);
		Link link3 = network.createLink(new IdImpl(3), node3, node4,  100, 10, 7200, 1);

		/* Create a person with two plans, driving from link 1 to link 3, starting at 7am.  */
		Population population = new Population(Population.NO_STREAMING);
		Person person1 = null;
		Act act1a = null;
		Act act1b = null;
		Act act2a = null;
		Act act2b = null;
		Leg leg1 = null;
		Leg leg2 = null;

		person1 = new PersonImpl(new IdImpl(1));
		// --- plan 1 ---
		Plan plan1 = person1.createPlan(true);
		act1a = plan1.createAct("h", new CoordImpl(-50.0, 10.0));
		act1a.setEndTime(7.0*3600);
		leg1 = plan1.createLeg(Mode.car);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		act1b = plan1.createAct("h", new CoordImpl(1075.0, -10.0));
		// --- plan 2 ---
		Plan plan2 = person1.createPlan(true);
		act2a = plan2.createAct("h", new CoordImpl(-50.0, -10.0));
		act2a.setEndTime(7.9*3600);
		leg2 = plan2.createLeg(Mode.car);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		act2b = plan2.createAct("h", new CoordImpl(1111.1, 10.0));
		population.addPerson(person1);

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
		controler.setWriteEventsInterval(0);
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

	public void testSetWriteEventsInterval() {
		final Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(10);

		final Controler controler = new Controler(config);
		assertFalse("Default for Controler.writeEventsInterval should be different from the interval we plan to use, otherwise it's hard to decide if it works correctly.",
				3 == controler.getWriteEventsInterval());
		controler.setWriteEventsInterval(3);
		assertEquals(3, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 0)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 1)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 2)).exists());
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 3)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 4)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 5)).exists());
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 6)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 7)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 8)).exists());
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 9)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 10)).exists());
	}

	public void testSetWriteEventsNever() {
		final Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(1);

		final Controler controler = new Controler(config);
		assertFalse("Default for Controler.writeEventsInterval should be different from the interval we plan to use, otherwise it's hard to decide if it works correctly.",
				0 == controler.getWriteEventsInterval());
		controler.setWriteEventsInterval(0);
		assertEquals(0, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 0)).exists());
		assertFalse(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 1)).exists());
	}

	public void testSetWriteEventsAlways() {
		final Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(1);

		final Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		assertEquals(1, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 0)).exists());
		assertTrue(new File(Controler.getIterationFilename(Controler.FILENAME_EVENTS, 1)).exists());
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
