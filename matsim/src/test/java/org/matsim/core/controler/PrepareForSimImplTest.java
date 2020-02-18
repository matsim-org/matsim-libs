/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup.HandlingOfPlansWithoutRoutingMode;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;

import com.google.inject.Provider;

/**
 * Mostly tests adaptation of old plans to routing mode and the related replacement of helper modes for access and egress
 * to pt/drt and the related replacement of fallback modes for pt/drt (if no route could be found).
 * 
 * Does not test the combination of already a routing mode and outdated helper / fallback modes, because those never existed
 * in the code at the same time.
 * 
 * TODO: add tests for other methods of {@link PrepareForSimImpl}.
 * 
 * @author vsp-gleich
 */
public class PrepareForSimImplTest {

	@Test
	public void testSingleLegTripRoutingMode() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.createScenario(config);
		createAndAddNetwork(scenario);
		Population pop = scenario.getPopulation();

		// test routing mode not set, such as after TripsToLegsAlgorithm + replanning strategy
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("1", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg = pf.createLeg(TransportMode.pt);
			// ensure routing mode is missing
			TripStructureUtils.setRoutingMode(leg, null);
			plan.addLeg(leg);
			Activity activity2 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity2);
			person.addPlan(plan);
			pop.addPerson(person);

			final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
					pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
					config.plans(), new MainModeIdentifierImpl());
			
			prepareForSimImpl.run();

			Assert.assertEquals("wrong routing mode!", TransportMode.pt,
					TripStructureUtils.getRoutingMode(leg));
		}
		
		// test routing mode set, such as after TripsToLegsAlgorithm + replanning strategy
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("2", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg = pf.createLeg(TransportMode.walk);
			// ensure routing mode is missing
			TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
			plan.addLeg(leg);
			Activity activity2 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity2);
			person.addPlan(plan);
			pop.addPerson(person);

			final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
					pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
					config.plans(), new MainModeIdentifierImpl());
			
			prepareForSimImpl.run();

			Assert.assertEquals("wrong routing mode!", TransportMode.pt,
					TripStructureUtils.getRoutingMode(leg));
		}
	}
	
	@Test
	public void testSingleFallbackModeLegTrip() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.createScenario(config);
		createAndAddNetwork(scenario);
		Population pop = scenario.getPopulation();

		// test outdated fallback mode single leg trip (pt)
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("1", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg = pf.createLeg(TransportMode.transit_walk);
			// ensure routing mode is missing
			TripStructureUtils.setRoutingMode(leg, null);
			plan.addLeg(leg);
			Activity activity2 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity2);
			person.addPlan(plan);
			pop.addPerson(person);

			final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
					pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
					config.plans(), new MainModeIdentifierImpl());
			
			prepareForSimImpl.run();

			Assert.assertEquals("wrong leg mode replacement!", TransportMode.walk, leg.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.pt,
					TripStructureUtils.getRoutingMode(leg));
		}
		
		// test outdated fallback mode single leg trip (arbitrary drt mode)
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("2", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg = pf.createLeg("drt67_fallback");
			// ensure routing mode is missing
			TripStructureUtils.setRoutingMode(leg, null);
			plan.addLeg(leg);
			Activity activity2 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity2);
			person.addPlan(plan);
			pop.addPerson(person);

			final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
					pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
					config.plans(), new MainModeIdentifierImpl());
			
			prepareForSimImpl.run();

			Assert.assertEquals("wrong leg mode replacement", TransportMode.walk, leg.getMode());
			Assert.assertEquals("wrong routing mode!", "drt67",
					TripStructureUtils.getRoutingMode(leg));
		}
	}
	
	@Test
	public void testCorrectTripsRemainUnchanged() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.createScenario(config);
		createAndAddNetwork(scenario);
		Population pop = scenario.getPopulation();
		
		// test car trip with access/egress walk legs
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("1", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg1 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg1, TransportMode.car);
			plan.addLeg(leg1);
			Activity activity2 = pf.createActivityFromCoord("car interaction", new Coord((double) 0, -10));
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.car);
			TripStructureUtils.setRoutingMode(leg2, TransportMode.car);
			plan.addLeg(leg2);
			Activity activity3 = pf.createActivityFromCoord("car interaction", new Coord((double) -10, -10));
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg3, TransportMode.car);
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity4);
			person.addPlan(plan);
			pop.addPerson(person);
			
			final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
					pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
					config.plans(), new MainModeIdentifierImpl());
			
			prepareForSimImpl.run();
			
			// Check leg modes remain unchanged
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg1.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.car, leg2.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg3.getMode());
			
			// Check routing mode:
			Assert.assertEquals("wrong routing mode!", TransportMode.car, TripStructureUtils.getRoutingMode(leg1));
			Assert.assertEquals("wrong routing mode!", TransportMode.car, TripStructureUtils.getRoutingMode(leg2));
			Assert.assertEquals("wrong routing mode!", TransportMode.car, TripStructureUtils.getRoutingMode(leg3));
		}
		
		// test complicated intermodal trip with consistent routing modes passes unchanged
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("2", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg1 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg1, "intermodal pt");
			plan.addLeg(leg1);
			Activity activity2 = pf.createActivityFromCoord("walk interaction", new Coord((double) 0, -10));
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg2, "intermodal pt");
			plan.addLeg(leg2);
			Activity activity3 = pf.createActivityFromCoord("walk interaction", new Coord((double) 0, -10));
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg3, "intermodal pt");
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("drt interaction", new Coord((double) 0, -10));
			plan.addActivity(activity4);
			Leg leg4 = pf.createLeg(TransportMode.drt);
			TripStructureUtils.setRoutingMode(leg4, "intermodal pt");
			plan.addLeg(leg4);
			Activity activity5 = pf.createActivityFromCoord("drt interaction", new Coord((double) -10, -10));
			plan.addActivity(activity5);
			Leg leg5 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg5, "intermodal pt");
			plan.addLeg(leg5);
			Activity activity6 = pf.createActivityFromCoord("walk interaction", new Coord((double) -10, -10));
			plan.addActivity(activity6);
			Leg leg6 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg6, "intermodal pt");
			plan.addLeg(leg6);
			Activity activity7 = pf.createActivityFromCoord("walk interaction", new Coord((double) -20, -10));
			plan.addActivity(activity7);
			Leg leg7 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg7, "intermodal pt");
			plan.addLeg(leg7);
			Activity activity8 = pf.createActivityFromCoord("pt interaction", new Coord((double) -20, -10));
			plan.addActivity(activity8);
			Leg leg8 = pf.createLeg(TransportMode.pt);
			TripStructureUtils.setRoutingMode(leg8, "intermodal pt");
			plan.addLeg(leg8);
			Activity activity9 = pf.createActivityFromCoord("pt interaction", new Coord((double) 1800, -10));
			plan.addActivity(activity9);
			Leg leg9 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg9, "intermodal pt");
			plan.addLeg(leg9);
			Activity activity10 = pf.createActivityFromCoord("walk interaction", new Coord((double) 1800, -10));
			plan.addActivity(activity10);
			Leg leg10 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg10, "intermodal pt");
			plan.addLeg(leg10);
			Activity activity11 = pf.createActivityFromCoord("walk interaction", new Coord((double) 1900, -10));
			plan.addActivity(activity11);
			Leg leg11 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg11, "intermodal pt");
			plan.addLeg(leg11);
			Activity activity12 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity12);
			person.addPlan(plan);
			pop.addPerson(person);
			
			final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
					pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
					config.plans(), new MainModeIdentifierImpl());
			
			prepareForSimImpl.run();
			
			// Check leg modes remain unchanged
			
			// TODO: Currently all TransportMode.non_network_walk legs are replaced by TransportMode.walk, so we cannot check
			// the correct handling of them right now.
//			Assert.assertEquals("wrong routing mode!", TransportMode.non_network_walk, leg1.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg2.getMode());
//			Assert.assertEquals("wrong routing mode!", TransportMode.non_network_walk, leg3.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, leg4.getMode());
//			Assert.assertEquals("wrong routing mode!", TransportMode.non_network_walk, leg5.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg6.getMode());
//			Assert.assertEquals("wrong routing mode!", TransportMode.non_network_walk, leg7.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.pt, leg8.getMode());
//			Assert.assertEquals("wrong routing mode!", TransportMode.non_network_walk, leg9.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg10.getMode());
//			Assert.assertEquals("wrong routing mode!", TransportMode.non_network_walk, leg11.getMode());
			
			// Check routing mode remains unchanged
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg1));
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg2));
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg3));
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg4));
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg5));
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg6));
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg7));
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg8));
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg9));
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg10));
			Assert.assertEquals("wrong routing mode!", "intermodal pt", TripStructureUtils.getRoutingMode(leg11));
		}
	}
	
	@Test
	public void testRoutingModeConsistency() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		Scenario scenario = ScenarioUtils.createScenario(config);
		createAndAddNetwork(scenario);
		Population pop = scenario.getPopulation();
		
		// test trip with inconsistent routing modes causes exception
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("3", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg1 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg1, TransportMode.pt);
			plan.addLeg(leg1);
			Activity activity2 = pf.createActivityFromCoord("drt interaction", new Coord((double) 0, -10));
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.drt);
			TripStructureUtils.setRoutingMode(leg2, TransportMode.drt);
			plan.addLeg(leg2);
			Activity activity3 = pf.createActivityFromCoord("drt interaction", new Coord((double) -20, -10));
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg3, TransportMode.drt);
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity4);
			person.addPlan(plan);
			pop.addPerson(person);
			
			try {
				final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
						pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
						config.plans(), new MainModeIdentifierImpl());
				
				prepareForSimImpl.run();
				Assert.fail("expected Exception, got none.");
			} catch (RuntimeException expected) {}
		}
		
		// test trip with legs with and others without routing modes causes exception
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("4", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg1 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg1, null);
			plan.addLeg(leg1);
			Activity activity2 = pf.createActivityFromCoord("drt interaction", new Coord((double) 0, -10));
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.drt);
			TripStructureUtils.setRoutingMode(leg2, TransportMode.drt);
			plan.addLeg(leg2);
			Activity activity3 = pf.createActivityFromCoord("drt interaction", new Coord((double) -20, -10));
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg3, TransportMode.drt);
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity4);
			person.addPlan(plan);
			pop.addPerson(person);
			
			try {
				final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
						pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
						config.plans(), new MainModeIdentifierImpl());
				
				prepareForSimImpl.run();
				Assert.fail("expected Exception, got none.");
			} catch (RuntimeException expected) {}
		}
	}
	
	@Test
	public void testOutdatedHelperModesReplacement() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.reject);
		Scenario scenario = ScenarioUtils.createScenario(config);
		createAndAddNetwork(scenario);
		Population pop = scenario.getPopulation();
		
		// test car trip with outdated access/egress walk modes
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("1", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg1 = pf.createLeg(TransportMode.access_walk);
			TripStructureUtils.setRoutingMode(leg1, null);
			plan.addLeg(leg1);
			Activity activity2 = pf.createActivityFromCoord("car interaction", new Coord((double) 0, -10));
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.car);
			TripStructureUtils.setRoutingMode(leg2, null);
			plan.addLeg(leg2);
			Activity activity3 = pf.createActivityFromCoord("car interaction", new Coord((double) -10, -10));
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.egress_walk);
			TripStructureUtils.setRoutingMode(leg3, null);
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity4);
			person.addPlan(plan);
			pop.addPerson(person);
			
			// Should give an exception with default: config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.reject);
			try {
				final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
						pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
						config.plans(), new MainModeIdentifierImpl());
				
				prepareForSimImpl.run();
				Assert.fail("expected Exception, got none.");
			} catch (RuntimeException expected) {}
			
			// Should work with config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
			config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
			final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
					pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
					config.plans(), new MainModeIdentifierImpl());
			
			prepareForSimImpl.run();
			
			// Check replacement of outdated helper modes.
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg1.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.car, leg2.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg3.getMode());
			
			// Check routing mode:
			Assert.assertEquals("wrong routing mode!", TransportMode.car, TripStructureUtils.getRoutingMode(leg1));
			Assert.assertEquals("wrong routing mode!", TransportMode.car, TripStructureUtils.getRoutingMode(leg2));
			Assert.assertEquals("wrong routing mode!", TransportMode.car, TripStructureUtils.getRoutingMode(leg3));
		}
		
		// test car trip with outdated access/egress walk modes
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("2", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg1 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg1, null);
			plan.addLeg(leg1);
			Activity activity2 = pf.createActivityFromCoord("car interaction", new Coord((double) 0, -10));
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.car);
			TripStructureUtils.setRoutingMode(leg2, null);
			plan.addLeg(leg2);
			Activity activity3 = pf.createActivityFromCoord("car interaction", new Coord((double) -10, -10));
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg3, null);
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity4);
			person.addPlan(plan);
			pop.addPerson(person);
			
			config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.reject);
			// Should give an exception with default: config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.reject);
			try {
				final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
						pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
						config.plans(), new MainModeIdentifierImpl());
				
				prepareForSimImpl.run();
				Assert.fail("expected Exception, got none.");
			} catch (RuntimeException expected) {}
			
			// Should work with config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
			config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
			final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
					pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
					config.plans(), new MainModeIdentifierImpl());
			
			prepareForSimImpl.run();
			
			// Check replacement of outdated helper modes.
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg1.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.car, leg2.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg3.getMode());
			
			// Check routing mode:
			Assert.assertEquals("wrong routing mode!", TransportMode.car, TripStructureUtils.getRoutingMode(leg1));
			Assert.assertEquals("wrong routing mode!", TransportMode.car, TripStructureUtils.getRoutingMode(leg2));
			Assert.assertEquals("wrong routing mode!", TransportMode.car, TripStructureUtils.getRoutingMode(leg3));
		}
		
		// test complicated intermodal drt+pt trip with outdated access/egress walk modes
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("3", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg1 = pf.createLeg(TransportMode.access_walk);
			TripStructureUtils.setRoutingMode(leg1, null);
			plan.addLeg(leg1);
			Activity activity2 = pf.createActivityFromCoord("drt interaction", new Coord((double) 0, -10));
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.drt);
			TripStructureUtils.setRoutingMode(leg2, null);
			plan.addLeg(leg2);
			Activity activity3 = pf.createActivityFromCoord("drt interaction", new Coord((double) -10, -10));
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg3, null);
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("pt interaction", new Coord((double) -20, -10));
			plan.addActivity(activity4);
			Leg leg4 = pf.createLeg(TransportMode.pt);
			TripStructureUtils.setRoutingMode(leg4, null);
			plan.addLeg(leg4);
			Activity activity5 = pf.createActivityFromCoord("pt interaction", new Coord((double) 1800, -10));
			plan.addActivity(activity5);
			Leg leg5 = pf.createLeg(TransportMode.transit_walk);
			TripStructureUtils.setRoutingMode(leg5, null);
			plan.addLeg(leg5);
			Activity activity6 = pf.createActivityFromCoord("walk interaction", new Coord((double) 1800, -10));
			plan.addActivity(activity6);
			Leg leg6 = pf.createLeg(TransportMode.pt);
			TripStructureUtils.setRoutingMode(leg6, null);
			plan.addLeg(leg6);
			Activity activity7 = pf.createActivityFromCoord("walk interaction", new Coord((double) 1900, -10));
			plan.addActivity(activity7);
			Leg leg7 = pf.createLeg(TransportMode.egress_walk);
			TripStructureUtils.setRoutingMode(leg7, null);
			plan.addLeg(leg7);
			Activity activity8 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity8);
			person.addPlan(plan);
			pop.addPerson(person);
			
			config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.reject);
			// Should give an exception with default: config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.reject);
			try {
				final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
						pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
						config.plans(), new MainModeIdentifierImpl());
				
				prepareForSimImpl.run();
				Assert.fail("expected Exception, got none.");
			} catch (RuntimeException expected) {}
			
			// Should work with config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
			config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
			final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
					pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
					config.plans(), new MainModeIdentifierImpl());
			
			prepareForSimImpl.run();
			
			// Check replacement of outdated helper modes.
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg1.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, leg2.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg3.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.pt, leg4.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg5.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.pt, leg6.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg7.getMode());
			
			/*
			 * Check routing mode:
			 * TransportMode.drt is what the default MainModeIdentifierImpl returns. To handle intermodal trips "right"
			 * in the old setup, we would probably have to overwrite the MainModeIdentifier with a custom
			 * MainModeIdentifier able to understand intermodal trips.
			 * 
			 * For the scope of this test it is sufficient, that the MainModeIdentifier is run, returns a main mode
			 * and that this main mode is assigned as routingMode to all legs. 
			 */
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg1));
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg2));
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg3));
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg4));
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg5));
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg6));
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg7));
		}
	}
	
	@Test
	public void testOutdatedFallbackAndHelperModesReplacement() {
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.reject);
		Scenario scenario = ScenarioUtils.createScenario(config);
		createAndAddNetwork(scenario);
		Population pop = scenario.getPopulation();
		
		// test complicated intermodal trip with consistent routing modes passes unchanged
		{
			PopulationFactory pf = pop.getFactory();
			Person person = pf.createPerson(Id.create("2", Person.class));
			Plan plan = pf.createPlan();
			Activity activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, -10));
			plan.addActivity(activity1);
			Leg leg1 = pf.createLeg(TransportMode.access_walk);
			TripStructureUtils.setRoutingMode(leg1, null);
			plan.addLeg(leg1);
			Activity activity2 = pf.createActivityFromCoord("drt interaction", new Coord((double) 0, -10));
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.drt);
			TripStructureUtils.setRoutingMode(leg2, null);
			plan.addLeg(leg2);
			Activity activity3 = pf.createActivityFromCoord("pt interaction", new Coord((double) -10, -10));
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.transit_walk);
			TripStructureUtils.setRoutingMode(leg3, null);
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("drt67 interaction", new Coord((double) -20, -10));
			plan.addActivity(activity4);
			Leg leg4 = pf.createLeg("drt67_walk");
			TripStructureUtils.setRoutingMode(leg4, null);
			plan.addLeg(leg4);
			Activity activity5 = pf.createActivityFromCoord("drt_fallback interaction", new Coord((double) 1800, -10));
			plan.addActivity(activity5);
			Leg leg5 = pf.createLeg("drt_fallback");
			TripStructureUtils.setRoutingMode(leg5, null);
			plan.addLeg(leg5);
			Activity activity6 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity6);
			person.addPlan(plan);
			pop.addPerson(person);
			
			// Should give an exception with default: config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.reject);
			try {
				final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
						pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
						config.plans(), new MainModeIdentifierImpl());
				
				prepareForSimImpl.run();
				Assert.fail("expected Exception, got none.");
			} catch (RuntimeException expected) {}
			
			// Should work with config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
			config.plans().setHandlingOfPlansWithoutRoutingMode(HandlingOfPlansWithoutRoutingMode.useMainModeIdentifier);
			final PrepareForSimImpl prepareForSimImpl = new PrepareForSimImpl(config.global(), scenario, scenario.getNetwork(), 
					pop, scenario.getActivityFacilities(), new DummyTripRouterProvider(), config.qsim(), config.facilities(), 
					config.plans(), new MainModeIdentifierImpl());
			
			prepareForSimImpl.run();
			
			// Check replacement of outdated helper modes.
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg1.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, leg2.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg3.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg4.getMode());
			Assert.assertEquals("wrong routing mode!", TransportMode.walk, leg5.getMode());
			
			/*
			 * Check routing mode:
			 * TransportMode.drt is what the default MainModeIdentifierImpl returns. To handle intermodal trips "right"
			 * in the old setup, we would probably have to overwrite the MainModeIdentifier with a custom
			 * MainModeIdentifier able to understand intermodal trips.
			 * 
			 * For the scope of thios test it is sufficient, that the MainModeIdentifier is run, returns a main mode
			 * and that this main mode is assigned as routingMode to all legs. 
			 */
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg1));
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg2));
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg3));
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg4));
			Assert.assertEquals("wrong routing mode!", TransportMode.drt, TripStructureUtils.getRoutingMode(leg5));
		}
	}
	
	private class DummyTripRouterProvider implements Provider<TripRouter> {
		@Override
		public TripRouter get() {
			return new TripRouter.Builder(ConfigUtils.createConfig())
					.setRoutingModule(TransportMode.walk, new DummyRoutingModule())
					.setRoutingModule(TransportMode.pt, new DummyRoutingModule())
					.setRoutingModule("intermodal pt", new DummyRoutingModule())
					.setRoutingModule(TransportMode.drt, new DummyRoutingModule())
					.setRoutingModule("drt67", new DummyRoutingModule())
					.setRoutingModule(TransportMode.car, new DummyRoutingModule())
					.build();
		}
	}
	
	private class DummyRoutingModule implements RoutingModule {
		@Override
		public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
				Person person) {
			return Collections.singletonList(PopulationUtils.createLeg("dummyMode"));
		}
	}

	private Link createAndAddNetwork(Scenario sc) {
		Network net = sc.getNetwork();
		Link link1;
		{
			NetworkFactory nf = net.getFactory();
			Set<String> modes = new HashSet<String>();
			Node n1 = nf.createNode(Id.create("1", Node.class), new Coord((double) 0, (double) 0));
			Node n2 = nf.createNode(Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
			Node n3 = nf.createNode(Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
			net.addNode(n1);
			net.addNode(n2);
			net.addNode(n3);
			link1 = nf.createLink(Id.create("1", Link.class), n1, n2);
			modes.add(TransportMode.car);
			link1.setAllowedModes(modes);
			Link l2 = nf.createLink(Id.create("2", Link.class), n2, n3);
			modes.clear();
			modes.add(TransportMode.pt);
			l2.setAllowedModes(modes);
			net.addLink(link1);
			net.addLink(l2);
		}
		return link1;
	}
}
