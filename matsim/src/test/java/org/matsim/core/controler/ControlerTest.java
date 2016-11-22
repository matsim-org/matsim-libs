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

package org.matsim.core.controler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Provider;

@RunWith(Parameterized.class)
public class ControlerTest {

	private final static Logger log = Logger.getLogger(ControlerTest.class);
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	private final boolean isUsingFastCapacityUpdate;
	
	public ControlerTest(boolean isUsingFastCapacityUpdate) {
		this.isUsingFastCapacityUpdate = isUsingFastCapacityUpdate;
	}
	
	@Parameters(name = "{index}: isUsingfastCapacityUpdate == {0}")
	public static Collection<Object> parameterObjects () {
		Object [] capacityUpdates = new Object [] { false, true };
				return Arrays.asList(capacityUpdates);
		// yyyy I am not sure why it is doing this ... it is necessary to do this around the qsim, but why here?  kai, aug'16
	}
	
	@Test
	public void testScenarioLoading() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		Controler controler = new Controler( config );

		// need to run the controler to get Scenario initilized
		controler.getConfig().controler().setLastIteration( 0 );
		controler.run();

        assertNotNull(controler.getScenario().getNetwork()); // is required, e.g. for changing the factories
        assertNotNull(controler.getScenario().getPopulation());
        assertEquals(23, controler.getScenario().getNetwork().getLinks().size());
        assertEquals(15, controler.getScenario().getNetwork().getNodes().size());
        assertEquals(100, controler.getScenario().getPopulation().getPersons().size());
		assertNotNull(controler.getEvents());
	}

	@Test
	public void testTerminationCriterion() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		Controler controler = new Controler(config);
		controler.setTerminationCriterion(new TerminationCriterion() {
			@Override
			public boolean continueIterations(int iteration) {
				return false;
			}
		});
		controler.run();
	}

	@Test
	public void testConstructor_EventsManagerTypeImmutable() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		MatsimServices controler = new Controler(config);
		try {
			controler.getConfig().setParam("parallelEventHandling", "numberOfThreads", "2");
			Assert.fail("Expected exception");
		} catch (Exception e) {
			log.info("catched expected exception", e);
		}
		try {
			controler.getConfig().setParam("parallelEventHandling", "estimatedNumberOfEvents", "200000");
			Assert.fail("Expected exception");
		} catch (Exception e) {
			log.info("catched expected exception", e);
		}
	}

	/**
	 * Tests that the travel times are correctly calculated during the simulation.
	 *
	 * @author mrieser
	 */
	@Test
	public void testTravelTimeCalculation() {
		Config config = this.utils.loadConfig((String) null);
		Fixture f = new Fixture(config);

		/* Create 2 persons driving from link 1 to link 3, both starting at the
		 * same time at 7am.  */
		Population population = f.scenario.getPopulation();
		PopulationFactory factory = population.getFactory();
		Person person1 = null;

		person1 = factory.createPerson(Id.create("1", Person.class));
		Plan plan1 = factory.createPlan();
		person1.addPlan(plan1);
		Activity a1 = factory.createActivityFromLinkId("h", f.link1.getId());
		a1.setEndTime(7.0*3600);
		plan1.addActivity(a1);
		Leg leg1 = factory.createLeg(TransportMode.car);
		plan1.addLeg(leg1);
		NetworkRoute route1 = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
		leg1.setRoute(route1);
		ArrayList<Id<Link>> linkIds = new ArrayList<Id<Link>>();
		linkIds.add(f.link2.getId());
		route1.setLinkIds(f.link1.getId(), linkIds, f.link3.getId());
		plan1.addActivity(factory.createActivityFromLinkId("h", f.link3.getId()));
		population.addPerson(person1);

		Person person2 = factory.createPerson(Id.create("2", Person.class));
		Plan plan2 = factory.createPlan();
		person2.addPlan(plan2);
		Activity a2 = factory.createActivityFromLinkId("h", f.link1.getId());
		a2.setEndTime(7.0*3600);
		plan2.addActivity(a2);
		Leg leg2 = factory.createLeg(TransportMode.car);
		plan2.addLeg(leg2);
		NetworkRoute route2 = f.scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, f.link1.getId(), f.link3.getId());
		leg2.setRoute(route2);
		route2.setLinkIds(f.link1.getId(), linkIds, f.link3.getId());
		plan2.addActivity(factory.createActivityFromLinkId("h", f.link3.getId()));
		population.addPerson(person2);

		// Complete the configuration for our test case
		// - set scoring parameters
		ActivityParams actParams = new ActivityParams("h");
		actParams.setTypicalDuration(8*3600);
		actParams.setPriority(1.0);
		config.planCalcScore().addActivityParams(actParams);
		// - define iterations
		config.controler().setLastIteration(0);
		// - make sure we don't use threads, as they are not deterministic
		config.global().setNumberOfThreads(0);

		config.qsim().setUsingFastCapacityUpdate(this.isUsingFastCapacityUpdate);
		
		// Now run the simulation
		Controler controler = new Controler(f.scenario);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.getConfig().controler().setWriteEventsInterval(0);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();

		// test if we got the right result
		if ( this.isUsingFastCapacityUpdate ) {
			// the actual result is 151sec, not 150, as each vehicle "loses" 1sec in the buffer
			assertEquals("TravelTimeCalculator has wrong result",
					150.5, controler.getLinkTravelTimes().getLinkTravelTime(f.link2, 7*3600, null, null), 0.0);
		} else {
			assertEquals("TravelTimeCalculator has wrong result",
					151.0, controler.getLinkTravelTimes().getLinkTravelTime(f.link2, 7*3600, null, null), 0.0);
		}

		// now test that the ReRoute-Strategy also knows about these travel times...
		config.controler().setLastIteration(1);
		ConfigGroup strategyParams = config.getModule("strategy");
		strategyParams.addParam("maxAgentPlanMemorySize", "4");
		strategyParams.addParam("ModuleProbability_1", "1.0");
		strategyParams.addParam("Module_1", "ReRoute");
		// Run the simulation again
		controler = new Controler(f.scenario);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controler().setWriteEventsInterval(0);
		controler.run();

		// test that the plans have the correct times
		if ( this.isUsingFastCapacityUpdate ) {
			assertEquals("ReRoute seems to have wrong travel times.",
					150.0, ((Leg) (person1.getPlans().get(1).getPlanElements().get(1))).getTravelTime(), 0.0);
		} else {
			assertEquals("ReRoute seems to have wrong travel times.",
					151.0, ((Leg) (person1.getPlans().get(1).getPlanElements().get(1))).getTravelTime(), 0.0);
		}
	}

	/**
	 * Tests that a custom scoring function factory doesn't get overwritten
	 * in the initialization process of the Controler.
	 *
	 * @author mrieser
	 */
	@Test
	public void testSetScoringFunctionFactory() {
		final Config config = this.utils.loadConfig((String) null);
		config.controler().setLastIteration(0);

		config.qsim().setUsingFastCapacityUpdate( this.isUsingFastCapacityUpdate );
		
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		// create a very simple network with one link only and an empty population
		Network network = scenario.getNetwork();
		Node node1 = network.getFactory().createNode(Id.create(1, Node.class), new Coord(0, 0));
		Node node2 = network.getFactory().createNode(Id.create(2, Node.class), new Coord(100, 0));
		network.addNode(node1);
		network.addNode(node2);
		Link link = network.getFactory().createLink(Id.create(1, Link.class), node1, node2);
		link.setLength(100);
		link.setFreespeed(1);
		link.setCapacity(3600.0);
		link.setNumberOfLanes(1);

		final Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.getConfig().controler().setWriteEventsInterval(0);
		controler.setScoringFunctionFactory(new DummyScoringFunctionFactory());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new FakeMobsim();
					}
				});
			}
		});
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();

		assertTrue("Custom ScoringFunctionFactory was not set.",
				controler.getScoringFunctionFactory() instanceof DummyScoringFunctionFactory);
	}

	/**
	 * Tests that plans with missing routes are completed (=routed) before the mobsim starts.
	 *
	 * @author mrieser
	 */
	@Test
	public void testCalcMissingRoutes() {
		Config config = this.utils.loadConfig((String) null);
		Fixture f = new Fixture(config);

		/* Create a person with two plans, driving from link 1 to link 3, starting at 7am.  */
		Population population = f.scenario.getPopulation();
		PopulationFactory factory = population.getFactory();
		Person person1 = null;
		Leg leg1 = null;
		Leg leg2 = null;

		person1 = factory.createPerson(Id.create("1", Person.class));
		// --- plan 1 ---
		Plan plan1 = factory.createPlan();
		person1.addPlan(plan1);
		Activity a1 = factory.createActivityFromLinkId("h", f.link1.getId());
		a1.setEndTime(7.0*3600);
		plan1.addActivity(a1);
		leg1 = factory.createLeg(TransportMode.car);
		plan1.addLeg(leg1);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		plan1.addActivity(factory.createActivityFromLinkId("h", f.link3.getId()));
		// --- plan 2 ---
		Plan plan2 = factory.createPlan();
		person1.addPlan(plan2);
		Activity a2 = factory.createActivityFromLinkId("h", f.link1.getId());
		a2.setEndTime(7.0*3600);
		plan2.addActivity(a2);

		leg2 = factory.createLeg(TransportMode.car);
		plan2.addLeg(leg2);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		plan2.addActivity(factory.createActivityFromLinkId("h", f.link3.getId()));
		population.addPerson(person1);

		// Complete the configuration for our test case
		// - set scoring parameters
		ActivityParams actParams = new ActivityParams("h");
		actParams.setTypicalDuration(8*3600);
		actParams.setPriority(1.0);
		config.planCalcScore().addActivityParams(actParams);
		// - define iterations
		config.controler().setLastIteration(0);
		// - make sure we don't use threads, as they are not deterministic
		config.global().setNumberOfThreads(1);

		config.qsim().setUsingFastCapacityUpdate( this.isUsingFastCapacityUpdate );
		
		// Now run the simulation
		Controler controler = new Controler(f.scenario);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.getConfig().controler().setWriteEventsInterval(0);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new FakeMobsim();
					}
				});
			}
		});
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		/* if something goes wrong, there will be an exception we don't catch and the test fails,
		 * otherwise, everything is fine. */

		// check that BOTH plans have a route set, even when we only run 1 iteration where only one of them is used.
		//assertNotNull(leg1.getRoute());
		//assertNotNull(leg2.getRoute());
		// but do not assume that the leg will be the same instance...
		for (Plan plan : new Plan[]{plan1, plan2}) {
			assertEquals(
					"unexpected plan length in "+plan.getPlanElements(),
					3,
					plan.getPlanElements().size());
			assertNotNull(
					"null route in plan "+plan.getPlanElements(),
					((Leg) plan.getPlanElements().get( 1 )).getRoute());
		}
	}

	/**
	 * Tests that plans with missing act locations are completed (=xy2links and routed) before the mobsim starts.
	 *
	 * @author mrieser
	 */
	@Test
	public void testCalcMissingActLinks() {
		Config config = this.utils.loadConfig((String) null);
		Fixture f = new Fixture(config);

		/* Create a person with two plans, driving from link 1 to link 3, starting at 7am.  */
		Population population = f.scenario.getPopulation();
		PopulationFactory factory = population.getFactory();
		Person person1 = null;
		Activity act1a = null;
		Activity act1b = null;
		Activity act2a = null;
		Activity act2b = null;
		Leg leg1 = null;
		Leg leg2 = null;

		person1 = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		// --- plan 1 ---
		Plan plan1 = factory.createPlan();
		person1.addPlan(plan1);
		double x1 = -50.0;
		act1a = factory.createActivityFromCoord("h", new Coord(x1, 10.0));
		act1a.setEndTime(7.0*3600);
		plan1.addActivity(act1a);
		leg1 = factory.createLeg(TransportMode.car);
		plan1.addLeg(leg1);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		double y1 = -10.0;
		act1b = factory.createActivityFromCoord("h", new Coord(1075.0, y1));
		plan1.addActivity(act1b);
		// --- plan 2 ---
		Plan plan2 = factory.createPlan();
		person1.addPlan(plan2);
		double x = -50.0;
		double y = -10.0;
		act2a = factory.createActivityFromCoord("h", new Coord(x, y));
		act2a.setEndTime(7.9*3600);
		plan2.addActivity(act2a);
		leg2 = factory.createLeg(TransportMode.car);
		plan2.addLeg(leg2);
		// DO NOT CREATE A ROUTE FOR THE LEG!!!
		act2b = factory.createActivityFromCoord("h", new Coord(1111.1, 10.0));
		plan2.addActivity(act2b);
		population.addPerson(person1);

		// Complete the configuration for our test case
		// - set scoring parameters
		ActivityParams actParams = new ActivityParams("h");
		actParams.setTypicalDuration(8*3600);
		actParams.setPriority(1.0);
		config.planCalcScore().addActivityParams(actParams);
		// - define iterations
		config.controler().setLastIteration(0);
		// - make sure we don't use threads, as they are not deterministic
		config.global().setNumberOfThreads(1);

		config.qsim().setUsingFastCapacityUpdate( this.isUsingFastCapacityUpdate );
		
		// Now run the simulation
		Controler controler = new Controler(f.scenario);
        controler.getConfig().controler().setCreateGraphs(false);
        controler.getConfig().controler().setWriteEventsInterval(0);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new FakeMobsim();
					}
				});
			}
		});
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		/* if something goes wrong, there will be an exception we don't catch and the test fails,
		 * otherwise, everything is fine. */

		// check that BOTH plans have their act-locations calculated
		assertEquals(f.link1.getId(), act1a.getLinkId());
		assertEquals(f.link3.getId(), act1b.getLinkId());
		assertEquals(f.link1.getId(), act2a.getLinkId());
		assertEquals(f.link3.getId(), act2b.getLinkId());
		
		int expectedPlanLength = 3 ;
		if ( f.scenario.getConfig().plansCalcRoute().isInsertingAccessEgressWalk() ) {
			// now 7 instead of earlier 3: h-wlk-iact-car-iact-walk-h
			expectedPlanLength = 7 ;
		}

		// check that BOTH plans have a route set, even when we only run 1 iteration where only one of them is used.
		//assertNotNull(leg1.getRoute());
		//assertNotNull(leg2.getRoute());
		// but do not assume that the leg will be the same instance...
		for (Plan plan : new Plan[]{plan1, plan2}) {
			assertEquals(
					"unexpected plan length in "+plan.getPlanElements(),
					expectedPlanLength,
					plan.getPlanElements().size());
			assertNotNull(
					"null route in plan "+plan.getPlanElements(),
					((Leg) plan.getPlanElements().get( 1 )).getRoute());
			if ( f.scenario.getConfig().plansCalcRoute().isInsertingAccessEgressWalk() ) {
				assertNotNull(
					"null route in plan "+plan.getPlanElements(),
					((Leg) plan.getPlanElements().get( 3 )).getRoute());
			}
		}
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testSetWriteEventsInterval() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(10);
		config.controler().setWritePlansInterval(0);

		final Controler controler = new Controler(config);
		assertFalse("Default for Controler.writeEventsInterval should be different from the interval we plan to use, otherwise it's hard to decide if it works correctly.",
				3 == controler.getConfig().controler().getWriteEventsInterval());

		controler.getConfig().controler().setWriteEventsInterval(3);
		assertEquals(3, controler.getConfig().controler().getWriteEventsInterval());

		controler.getConfig().controler().setCreateGraphs(false);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new FakeMobsim();
					}
				});
			}
		});
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(1, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(2, Controler.FILENAME_EVENTS_XML)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(3, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(4, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(5, Controler.FILENAME_EVENTS_XML)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(6, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(7, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(8, Controler.FILENAME_EVENTS_XML)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(9, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(10, Controler.FILENAME_EVENTS_XML)).exists());
	}

	/**
	 * @author wrashid
	 */
	@Test
	public void testSetWriteEventsIntervalConfig() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(10);
		config.controler().setWritePlansInterval(0);

		final Controler controler = new Controler(config);
		assertFalse("Default for Controler.writeEventsInterval should be different from the interval we plan to use, otherwise it's hard to decide if it works correctly.",
				3 == controler.getConfig().controler().getWriteEventsInterval());
        controler.getConfig().controler().setCreateGraphs(false);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new FakeMobsim();
					}
				});
			}
		});
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
		assertEquals(4, controler.getConfig().controler().getWriteEventsInterval());

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(1, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(2, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(3, Controler.FILENAME_EVENTS_XML)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(4, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(5, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(6, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(7, Controler.FILENAME_EVENTS_XML)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(8, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(9, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(10, Controler.FILENAME_EVENTS_XML)).exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testSetWriteEventsNever() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(1);
		config.controler().setWritePlansInterval(0);

		final Controler controler = new Controler(config);
		assertFalse("Default for Controler.writeEventsInterval should be different from the interval we plan to use, otherwise it's hard to decide if it works correctly.",
				0 == controler.getConfig().controler().getWriteEventsInterval());
		controler.getConfig().controler().setWriteEventsInterval(0);
		assertEquals(0, controler.getConfig().controler().getWriteEventsInterval());
        controler.getConfig().controler().setCreateGraphs(false);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new FakeMobsim();
					}
				});
			}
		});
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();

		assertFalse(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_XML)).exists());
		assertFalse(new File(controler.getControlerIO().getIterationFilename(1, Controler.FILENAME_EVENTS_XML)).exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testSetWriteEventsAlways() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(1);
		config.controler().setWritePlansInterval(0);

		final Controler controler = new Controler(config);
		controler.getConfig().controler().setWriteEventsInterval(1);
		assertEquals(1, controler.getConfig().controler().getWriteEventsInterval());
        controler.getConfig().controler().setCreateGraphs(false);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new FakeMobsim();
					}
				});
			}
		});
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_XML)).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(1, Controler.FILENAME_EVENTS_XML)).exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testSetWriteEventsXml() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setEventsFileFormats(EnumSet.of(EventsFileFormat.xml));

		final Controler controler = new Controler(config);
		controler.getConfig().controler().setWriteEventsInterval(1);
		assertEquals(1, controler.getConfig().controler().getWriteEventsInterval());
        controler.getConfig().controler().setCreateGraphs(false);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new FakeMobsim();
					}
				});
			}
		});
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, Controler.FILENAME_EVENTS_XML)).exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testSetDumpDataAtEnd_true() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(0);
		config.controler().setWritePlansInterval(0);

		final Controler controler = new Controler(config);
		controler.getConfig().controler().setWriteEventsInterval(0);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new FakeMobsim();
					}
				});
			}
		});

		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getOutputFilename(Controler.FILENAME_POPULATION)).exists());
	}

	/**
	 * @author mrieser
	 */
	@Test
	public void testSetDumpDataAtEnd_false() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(0);
		config.controler().setWritePlansInterval(0);

		final Controler controler = new Controler(config);
		controler.getConfig().controler().setWriteEventsInterval(0);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().toProvider(new Provider<Mobsim>() {
					@Override
					public Mobsim get() {
						return new FakeMobsim();
					}
				});
			}
		});

		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();


		assertFalse(new File(controler.getControlerIO().getOutputFilename(Controler.FILENAME_POPULATION)).exists());
	}

	@Test(expected = RuntimeException.class)
	public void testShutdown_UncaughtException() throws InterruptedException {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(1);

		Controler controler = new Controler(config);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindMobsim().to(CrashingMobsim.class);
			}
		});
		controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();
	}

	@Test
	public void test_ExceptionOnMissingPopulationFile() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(0);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.plans().setInputFile("dummy/non-existing/population.xml");

		try {
			Controler controler = new Controler(config);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindMobsim().toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return new FakeMobsim();
						}
					});
				}
			});
			controler.getConfig().controler().setCreateGraphs(false);
			controler.getConfig().controler().setDumpDataAtEnd(false);
			controler.run();
			Assert.fail("expected exception, got none.");
			
			// note: I moved loadScenario in the controler from run() into the constructor to mirror the loading sequence one has
			// when calling new Controler(scenario).  In consequence, it fails already in the constructor; one could stop after that.  
			// kai, apr'15
			
		} catch (RuntimeException e) {
			log.info("catched expected exception.", e);
		}
	}

	@Test
	public void test_ExceptionOnMissingNetworkFile() {
		try {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(0);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.network().setInputFile("dummy/non-existing/network.xml");

		final Controler controler = new Controler(config);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindMobsim().toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return new FakeMobsim();
						}
					});
				}
			});
			controler.getConfig().controler().setCreateGraphs(false);
			controler.getConfig().controler().setDumpDataAtEnd(false);
			controler.run();
			Assert.fail("expected exception, got none.");
			
			// note: I moved loadScenario in the controler from run() into the constructor to mirror the loading sequence one has
			// when calling new Controler(scenario).  In consequence, it fails already in the constructor; one could stop after that.  
			// kai, apr'15
			
		} catch (RuntimeException e) {
			log.info("catched expected exception.", e);
		}
	}

	@Test
	public void test_ExceptionOnMissingFacilitiesFile() {
		try {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(0);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.facilities().setInputFile("dummy/non-existing/network.xml");

		final Controler controler = new Controler(config);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindMobsim().toProvider(new Provider<Mobsim>() {
						@Override
						public Mobsim get() {
							return new FakeMobsim();
						}
					});
				}
			});
			controler.getConfig().controler().setCreateGraphs(false);
			controler.getConfig().controler().setDumpDataAtEnd(false);
			controler.run();
			Assert.fail("expected exception, got none.");
			
			// note: I moved loadScenario in the controler from run() into the constructor to mirror the loading sequence one has
			// when calling new Controler(scenario).  In consequence, it fails already in the constructor; one could stop after that.  
			// kai, apr'15
			
		} catch (RuntimeException e) {
			log.info("catched expected exception.", e);
		}
	}

	@Test
	public void testKMLSnapshotWriterOnQSim() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(2);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setMobsim("qsim");
		config.controler().setSnapshotFormat(Arrays.asList("googleearth"));
		config.qsim().setSnapshotPeriod(600);
		config.qsim().setSnapshotStyle( SnapshotStyle.equiDist ) ;

		final Controler controler = new Controler(config);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, "googleearth.kmz")).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(1, "googleearth.kmz")).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(2, "googleearth.kmz")).exists());
	}

	@Test
	public void testOneSnapshotWriterInConfig() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(0);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.qsim().setSnapshotPeriod(10);
		config.qsim().setSnapshotStyle(SnapshotStyle.equiDist) ;;

		final Controler controler = new Controler(config);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, "T.veh.gz")).exists());
	}

	@Test
	public void testTransimsSnapshotWriterOnQSim() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration(2);
		config.controler().setWriteEventsInterval(0);
		config.controler().setWritePlansInterval(0);
		config.controler().setMobsim("qsim");
		config.controler().setSnapshotFormat(Arrays.asList("transims"));
		config.qsim().setSnapshotPeriod(600);
		config.qsim().setSnapshotStyle( SnapshotStyle.equiDist ) ;;

		final Controler controler = new Controler(config);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.run();

		assertTrue(new File(controler.getControlerIO().getIterationFilename(0, "T.veh.gz")).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(1, "T.veh.gz")).exists());
		assertTrue(new File(controler.getControlerIO().getIterationFilename(2, "T.veh.gz")).exists());
	}

	/**
	 * This might sound (or be) silly, but we had this problem in zurich when using a mix of old code and Guice-based code:
	 * old code wrapped into Guice modules eventually called Controler.setScoringFunctionFactory(),
	 * which itself adds a Guice module... but too late.
	 *
	 * @thibautd
	 * 
	 */
	@Ignore // see email 22/nov/16 by kai
	@Test( expected = RuntimeException.class )
	public void testGuiceModulesCannotAddModules() {
		final Config config = utils.loadConfig(IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("equil"), "config_plans1.xml"));
		config.controler().setLastIteration( 0 );
		final Controler controler = new Controler( config );

		controler.addOverridingModule(
				new AbstractModule() {
					@Override
					public void install() {
						controler.setScoringFunctionFactory( null );
					}
				}
		);

		controler.run();
	}

	static class FakeMobsim implements Mobsim {
		@Override
		public void run() {
			// nothing to do
		}
	}

	static class CrashingMobsim implements Mobsim {
		@Override
		public void run() {
			// Evil: Create and join an unmanaged thread on which there is an Exception.
			// Normally, this silently exits, but we want it to test our infrastructure where
			// Exceptions on wild threads are collected and dispatched to the Controler.
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					throw new NullPointerException("Just for testing...");
				}
			});
			thread.start();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/** A helper class for testSetScoringFunctionFactory() */
	static class DummyScoringFunctionFactory implements ScoringFunctionFactory {
		@Override
		public ScoringFunction createNewScoringFunction(final Person person) {
			return new SumScoringFunction();
		}
	}

	/**
	 * @author mrieser
	 */
	private static class Fixture {
		final MutableScenario scenario;
		final Network network;
		Node node1 = null;
		Node node2 = null;
		Node node3 = null;
		Node node4 = null;
		Link link1 = null;
		Link link2 = null;
		Link link3 = null;

		protected Fixture(final Config config) {
			this.scenario = (MutableScenario) ScenarioUtils.createScenario(config);
			this.network = this.scenario.getNetwork();

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
//			this.network.setCapacityPeriod(Time.parseTime("01:00:00"));
			final double x = -100.0;
			this.node1 = this.network.getFactory().createNode(Id.create(1, Node.class), new Coord(x, 0.0));
			this.node2 = this.network.getFactory().createNode(Id.create(2, Node.class), new Coord(0.0, 0.0));
			this.node3 = this.network.getFactory().createNode(Id.create(3, Node.class), new Coord(1000.0, 0.0));
			this.node4 = this.network.getFactory().createNode(Id.create(4, Node.class), new Coord(1100.0, 0.0));
			this.network.addNode(this.node1);
			this.network.addNode(this.node2);
			this.network.addNode(this.node3);
			this.network.addNode(this.node4);
			this.link1 = this.network.getFactory().createLink(Id.create(1, Link.class), this.node1, this.node2);
			this.link1.setLength(100);
			this.link1.setFreespeed(10);
			this.link1.setCapacity(7200);
			this.link1.setNumberOfLanes(1);
			this.network.addLink(this.link1);
			this.link2 = this.network.getFactory().createLink(Id.create(2, Link.class), this.node2, this.node3);
			this.link2.setLength(1000);
			this.link2.setFreespeed(10);
			this.link2.setCapacity(36);
			this.link2.setNumberOfLanes(1);
			this.network.addLink(this.link2);
			this.link3 = this.network.getFactory().createLink(Id.create(3, Link.class), this.node3, this.node4);
			this.link3.setLength(100);
			this.link3.setFreespeed(10);
			this.link3.setCapacity(7200);
			this.link3.setNumberOfLanes(1);
			this.network.addLink(this.link3);
		}
	}

}
