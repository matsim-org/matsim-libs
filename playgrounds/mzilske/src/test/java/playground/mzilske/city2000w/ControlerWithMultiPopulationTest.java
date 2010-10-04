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

package playground.mzilske.city2000w;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

public class ControlerWithMultiPopulationTest extends MatsimTestCase {


	/**
	 * Tests that the travel times are correctly calculated during the simulation.
	 *
	 * @author mrieser
	 */
//	public void testTravelTimeCalculation() {
//		Config config = loadConfig(null);
//		Fixture f = new Fixture(config);
//
//		/* Create 2 persons driving from link 1 to link 3, both starting at the
//		 * same time at 7am.  */
//		Population population = new PopulationImpl(f.scenario);
//		f.scenario.getPopulations().add(population);
//		PopulationFactory factory = population.getFactory();
//		Person person1 = null;
//
//		person1 = factory.createPerson(f.scenario.createId("1"));
//		Plan plan1 = factory.createPlan();
//		person1.addPlan(plan1);
//		Activity a1 = factory.createActivityFromLinkId("h", f.link1.getId());
//		a1.setEndTime(7.0*3600);
//		plan1.addActivity(a1);
//		Leg leg1 = factory.createLeg(TransportMode.car);
//		plan1.addLeg(leg1);
//		NetworkRoute route1 = (NetworkRoute)f.network.getFactory().createRoute(TransportMode.car, f.link1.getId(), f.link3.getId());
//		leg1.setRoute(route1);
//		ArrayList<Id> linkIds = new ArrayList<Id>();
//		linkIds.add(f.link2.getId());
//		route1.setLinkIds(f.link1.getId(), linkIds, f.link3.getId());
//		plan1.addActivity(factory.createActivityFromLinkId("h", f.link3.getId()));
//		population.addPerson(person1);
//
//		Person person2 = factory.createPerson(f.scenario.createId("2"));
//		Plan plan2 = factory.createPlan();
//		person2.addPlan(plan2);
//		Activity a2 = factory.createActivityFromLinkId("h", f.link1.getId());
//		a2.setEndTime(7.0*3600);
//		plan2.addActivity(a2);
//		Leg leg2 = factory.createLeg(TransportMode.car);
//		plan2.addLeg(leg2);
//		NetworkRoute route2 = (NetworkRoute)f.network.getFactory().createRoute(TransportMode.car, f.link1.getId(), f.link3.getId());
//		leg2.setRoute(route2);
//		route2.setLinkIds(f.link1.getId(), linkIds, f.link3.getId());
//		plan2.addActivity(factory.createActivityFromLinkId("h", f.link3.getId()));
//		population.addPerson(person2);
//
//		// Complete the configuration for our test case
//		// - set scoring parameters
//		ActivityParams actParams = new ActivityParams("h");
//		actParams.setTypicalDuration(8*3600);
//		actParams.setPriority(1.0);
//		config.charyparNagelScoring().addActivityParams(actParams);
//		// - define iterations
//		config.controler().setLastIteration(0);
//		// - make sure we don't use threads, as they are not deterministic
//		config.global().setNumberOfThreads(0);
//
//		// Now run the simulation
//		Controler controler = new Controler(f.scenario);
//		controler.setCreateGraphs(false);
//		controler.setWriteEventsInterval(0);
//		controler.run();
//
//		// test if we got the right result
//		// the actual result is 151sec, not 150, as each vehicle "loses" 1sec in the buffer
//		assertEquals("TravelTimeCalculator has wrong result",
//				151.0, controler.getTravelTimeCalculator().getLinkTravelTime(f.link2, 7*3600), 0.0);
//
//		// now test that the ReRoute-Strategy also knows about these travel times...
//		config.controler().setLastIteration(1);
//		Module strategyParams = config.getModule("strategy");
//		strategyParams.addParam("maxAgentPlanMemorySize", "4");
//		strategyParams.addParam("ModuleProbability_1", "1.0");
//		strategyParams.addParam("Module_1", "ReRoute");
//		// Run the simulation again
//		controler = new Controler(f.scenario);
//		controler.setCreateGraphs(false);
//		controler.setOverwriteFiles(true);
//		controler.setWriteEventsInterval(0);
//		controler.run();
//
//		// test that the plans have the correct times
//		assertEquals("ReRoute seems to have wrong travel times.",
//				151.0, ((Leg) (person1.getPlans().get(1).getPlanElements().get(1))).getTravelTime(), 0.0);
//	}

	/**
	 * Tests that a custom scoring function factory doesn't get overwritten
	 * in the initialization process of the Controler.
	 *
	 * @author mrieser
	 */
	public void testSetScoringFunctionFactory() {
		final Config config = loadConfig(null);
		config.controler().setLastIteration(0);

		ScenarioImpl scenario = new ScenarioImpl(config);
		// create a very simple network with one link only and an empty population
		Network network = scenario.getNetwork();
		Node node1 = network.getFactory().createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.getFactory().createNode(new IdImpl(2), new CoordImpl(100, 0));
		network.addNode(node1);
		network.addNode(node2);
		Link link = network.getFactory().createLink(new IdImpl(1), node1.getId(), node2.getId());
		link.setLength(100);
		link.setFreespeed(1);
		link.setCapacity(3600.0);
		link.setNumberOfLanes(1);

		final Controler controler = new Controler(scenario);
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
	 *
	 * @author mrieser
	 */
//	public void testCalcMissingRoutes() {
//		Config config = loadConfig(null);
//		Fixture f = new Fixture(config);
//
//		/* Create a person with two plans, driving from link 1 to link 3, starting at 7am.  */
//		Population population = new PopulationImpl(f.scenario);
//		f.scenario.getPopulations().add(population);
//		PopulationFactory factory = population.getFactory();
//		Person person1 = null;
//		Leg leg1 = null;
//		Leg leg2 = null;
//
//		person1 = factory.createPerson(f.scenario.createId("1"));
//		// --- plan 1 ---
//		Plan plan1 = factory.createPlan();
//		person1.addPlan(plan1);
//		Activity a1 = factory.createActivityFromLinkId("h", f.link1.getId());
//		a1.setEndTime(7.0*3600);
//		plan1.addActivity(a1);
//		leg1 = factory.createLeg(TransportMode.car);
//		plan1.addLeg(leg1);
//		// DO NOT CREATE A ROUTE FOR THE LEG!!!
//		plan1.addActivity(factory.createActivityFromLinkId("h", f.link3.getId()));
//		// --- plan 2 ---
//		Plan plan2 = factory.createPlan();
//		person1.addPlan(plan2);
//		Activity a2 = factory.createActivityFromLinkId("h", f.link1.getId());
//		a2.setEndTime(7.0*3600);
//		plan2.addActivity(a2);
//
//		leg2 = factory.createLeg(TransportMode.car);
//		plan2.addLeg(leg2);
//		// DO NOT CREATE A ROUTE FOR THE LEG!!!
//		plan2.addActivity(factory.createActivityFromLinkId("h", f.link3.getId()));
//		population.addPerson(person1);
//
//		// Complete the configuration for our test case
//		// - set scoring parameters
//		ActivityParams actParams = new ActivityParams("h");
//		actParams.setTypicalDuration(8*3600);
//		actParams.setPriority(1.0);
//		config.charyparNagelScoring().addActivityParams(actParams);
//		// - define iterations
//		config.controler().setLastIteration(0);
//		// - make sure we don't use threads, as they are not deterministic
//		config.global().setNumberOfThreads(1);
//
//		// Now run the simulation
//		Controler controler = new Controler(f.scenario);
//		controler.setCreateGraphs(false);
//		controler.setWriteEventsInterval(0);
//		controler.run();
//		/* if something goes wrong, there will be an exception we don't catch and the test fails,
//		 * otherwise, everything is fine. */
//
//		// check that BOTH plans have a route set, even when we only run 1 iteration where only one of them is used.
//		assertNotNull(leg1.getRoute());
//		assertNotNull(leg2.getRoute());
//	}

	/**
	 * Tests that plans with missing act locations are completed (=xy2links and routed) before the mobsim starts.
	 *
	 * @author mrieser
	 */
//	public void testCalcMissingActLinks() {
//		Config config = loadConfig(null);
//		Fixture f = new Fixture(config);
//
//		/* Create a person with two plans, driving from link 1 to link 3, starting at 7am.  */
//		Population population = new PopulationImpl(f.scenario);
//		f.scenario.getPopulations().add(population);
//		PopulationFactory factory = population.getFactory();
//		Person person1 = null;
//		Activity act1a = null;
//		Activity act1b = null;
//		Activity act2a = null;
//		Activity act2b = null;
//		Leg leg1 = null;
//		Leg leg2 = null;
//
//		person1 = new PersonImpl(new IdImpl(1));
//		// --- plan 1 ---
//		Plan plan1 = factory.createPlan();
//		person1.addPlan(plan1);
//		act1a = factory.createActivityFromCoord("h", f.scenario.createCoord(-50.0, 10.0));
//		act1a.setEndTime(7.0*3600);
//		plan1.addActivity(act1a);
//		leg1 = factory.createLeg(TransportMode.car);
//		plan1.addLeg(leg1);
//		// DO NOT CREATE A ROUTE FOR THE LEG!!!
//		act1b = factory.createActivityFromCoord("h", f.scenario.createCoord(1075.0, -10.0));
//		plan1.addActivity(act1b);
//		// --- plan 2 ---
//		Plan plan2 = factory.createPlan();
//		person1.addPlan(plan2);
//		act2a = factory.createActivityFromCoord("h", f.scenario.createCoord(-50.0, -10.0));
//		act2a.setEndTime(7.9*3600);
//		plan2.addActivity(act2a);
//		leg2 = factory.createLeg(TransportMode.car);
//		plan2.addLeg(leg2);
//		// DO NOT CREATE A ROUTE FOR THE LEG!!!
//		act2b = factory.createActivityFromCoord("h", f.scenario.createCoord(1111.1, 10.0));
//		plan2.addActivity(act2b);
//		population.addPerson(person1);
//
//		// Complete the configuration for our test case
//		// - set scoring parameters
//		ActivityParams actParams = new ActivityParams("h");
//		actParams.setTypicalDuration(8*3600);
//		actParams.setPriority(1.0);
//		config.charyparNagelScoring().addActivityParams(actParams);
//		// - define iterations
//		config.controler().setLastIteration(0);
//		// - make sure we don't use threads, as they are not deterministic
//		config.global().setNumberOfThreads(1);
//
//		// Now run the simulation
//		Controler controler = new Controler(f.scenario);
//		controler.setCreateGraphs(false);
//		controler.setWriteEventsInterval(0);
//		controler.run();
//		/* if something goes wrong, there will be an exception we don't catch and the test fails,
//		 * otherwise, everything is fine. */
//
//		// check that BOTH plans have their act-locations calculated
//		assertEquals(f.link1.getId(), act1a.getLinkId());
//		assertEquals(f.link3.getId(), act1b.getLinkId());
//		assertEquals(f.link1.getId(), act2a.getLinkId());
//		assertEquals(f.link3.getId(), act2b.getLinkId());
//
//		// check that BOTH plans have a route set, even when we only run 1 iteration where only one of them is used.
//		assertNotNull(leg1.getRoute());
//		assertNotNull(leg2.getRoute());
//	}

	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsInterval() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(10);

		final Controler controler = new Controler(config);
		assertFalse("Default for Controler.writeEventsInterval should be different from the interval we plan to use, otherwise it's hard to decide if it works correctly.",
				3 == controler.getWriteEventsInterval());
		controler.setWriteEventsInterval(3);
		assertEquals(3, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(1, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(2, Controler.FILENAME_EVENTS_TXT)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(3, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(4, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(5, Controler.FILENAME_EVENTS_TXT)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(6, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(7, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(8, Controler.FILENAME_EVENTS_TXT)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(9, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(10, Controler.FILENAME_EVENTS_TXT)).exists());
	}

	/**
	 * @author wrashid
	 */
	public void testSetWriteEventsIntervalConfig() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(10);

		final Controler controler = new Controler(config);
		assertFalse("Default for Controler.writeEventsInterval should be different from the interval we plan to use, otherwise it's hard to decide if it works correctly.",
				3 == controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();
		assertEquals(4, controler.getWriteEventsInterval());

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(1, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(2, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(3, Controler.FILENAME_EVENTS_TXT)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(4, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(5, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(6, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(7, Controler.FILENAME_EVENTS_TXT)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(8, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(9, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(10, Controler.FILENAME_EVENTS_TXT)).exists());
	}

	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsNever() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(1);

		final Controler controler = new Controler(config);
		assertFalse("Default for Controler.writeEventsInterval should be different from the interval we plan to use, otherwise it's hard to decide if it works correctly.",
				0 == controler.getWriteEventsInterval());
		controler.setWriteEventsInterval(0);
		assertEquals(0, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertFalse(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(1, Controler.FILENAME_EVENTS_TXT)).exists());
	}

	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsAlways() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(1);

		final Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		assertEquals(1, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_TXT)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(1, Controler.FILENAME_EVENTS_TXT)).exists());
	}

	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsTxt() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(0);
		config.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.txt));

		final Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		assertEquals(1, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_TXT)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_XML)).exists());
	}

	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsXml() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(0);
		config.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));

		final Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		assertEquals(1, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertFalse(new File(controler.getControlerIO().getIterationFilename(0,  Controler.FILENAME_EVENTS_TXT)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_XML)).exists());
	}

	/**
	 * @author mrieser
	 */
	public void testSetWriteEventsTxtXml() {
		final Config config = loadConfig("test/scenarios/equil/config_plans1.xml");
		config.controler().setLastIteration(0);
		config.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.txt, EventsFileFormat.xml));

		final Controler controler = new Controler(config);
		controler.setWriteEventsInterval(1);
		assertEquals(1, controler.getWriteEventsInterval());
		controler.setCreateGraphs(false);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_TXT)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_XML)).exists());
	}


	/** A helper class for testSetScoringFunctionFactory() */
	/*package*/ static class DummyScoringFunctionFactory implements ScoringFunctionFactory {

		public ScoringFunction createNewScoringFunction(final Plan plan) {
			return new ScoringFunctionAccumulator();
		}
	}

	/**
	 * @author mrieser
	 */
	private static class Fixture {
		final MultiPopulationScenario scenario;
		final NetworkImpl network;
		Node node1 = null;
		Node node2 = null;
		Node node3 = null;
		Node node4 = null;
		Link link1 = null;
		Link link2 = null;
		Link link3 = null;

		protected Fixture(final Config config) {
			this.scenario = new MultiPopulationScenario(config);
			this.network = scenario.getNetwork();

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
			this.network.setCapacityPeriod(Time.parseTime("01:00:00"));
			this.node1 = this.network.getFactory().createNode(new IdImpl(1), new CoordImpl(-100.0, 0.0));
			this.node2 = this.network.getFactory().createNode(new IdImpl(2), new CoordImpl(0.0, 0.0));
			this.node3 = this.network.getFactory().createNode(new IdImpl(3), new CoordImpl(1000.0, 0.0));
			this.node4 = this.network.getFactory().createNode(new IdImpl(4), new CoordImpl(1100.0, 0.0));
			this.network.addNode(this.node1);
			this.network.addNode(this.node2);
			this.network.addNode(this.node3);
			this.network.addNode(this.node4);
			this.link1 = this.network.getFactory().createLink(new IdImpl(1), this.node1.getId(), this.node2.getId());
			link1.setLength(100);
			link1.setFreespeed(10);
			link1.setCapacity(7200);
			link1.setNumberOfLanes(1);
			this.network.addLink(link1);
			this.link2 = this.network.getFactory().createLink(new IdImpl(2), this.node2.getId(), this.node3.getId());
			link2.setLength(1000);
			link2.setFreespeed(10);
			link2.setCapacity(36);
			link2.setNumberOfLanes(1);
			this.network.addLink(link2);
			this.link3 = this.network.getFactory().createLink(new IdImpl(3), this.node3.getId(), this.node4.getId());
			link3.setLength(100);
			link3.setFreespeed(10);
			link3.setCapacity(7200);
			link3.setNumberOfLanes(1);
			this.network.addLink(link3);
		}
	}

}
