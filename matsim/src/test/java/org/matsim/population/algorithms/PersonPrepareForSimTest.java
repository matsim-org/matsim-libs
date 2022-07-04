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

package org.matsim.population.algorithms;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
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
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForSimImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PersonPrepareForSim;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser / senozon
 */
public class PersonPrepareForSimTest {

	@Test
	public void testRun_MultimodalNetwork() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		createAndAddNetwork(sc);
		Id<Link> link1id = Id.createLinkId("1");

		Population pop = sc.getPopulation();
		Person person;
		Activity activity1;
		Activity activity2;
		{
			PopulationFactory pf = pop.getFactory();
			person = pf.createPerson(Id.create("1", Person.class));
			Plan p = pf.createPlan();
			double y1 = -10;
			activity1 = pf.createActivityFromCoord("h", new Coord((double) 10, y1));
			Leg l = pf.createLeg(TransportMode.walk);
			double y = -10;
			activity2 = pf.createActivityFromCoord("w", new Coord((double) 1900, y));
			p.addActivity(activity1);
			p.addLeg(l);
			p.addActivity(activity2);
			person.addPlan(p);
			pop.addPerson(person);
		}

		new PersonPrepareForSim(new DummyRouter(), sc).run(person);

		Assert.assertEquals(link1id, activity1.getLinkId());
		Assert.assertEquals(link1id, activity2.getLinkId()); // must also be linked to l1, as l2 has no car mode
	}

	@Test
	public void testRun_MultimodalScenario() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		createAndAddNetwork(sc);
		Id<Link> link1id = Id.createLinkId("1");
		
		Population pop = sc.getPopulation();
		Person person;
		Activity a1;
		Activity a2;
		{
			PopulationFactory pf = pop.getFactory();
			person = pf.createPerson(Id.create("1", Person.class));
			Plan p = pf.createPlan();
			double y1 = -10;
			a1 = pf.createActivityFromCoord("h", new Coord((double) 10, y1));
			Leg l = pf.createLeg(TransportMode.walk);
			double y = -10;
			a2 = pf.createActivityFromCoord("w", new Coord((double) 1900, y));
			p.addActivity(a1);
			p.addLeg(l);
			p.addActivity(a2);
			person.addPlan(p);
			pop.addPerson(person);
		}
		
		new PersonPrepareForSim(new DummyRouter(), sc).run(person);
		
		Assert.assertEquals(link1id, a1.getLinkId());
		Assert.assertEquals(link1id, a2.getLinkId()); // must also be linked to l1, as l2 has no car mode
	}
	
	@Test
	public void testSingleLegTripRoutingMode() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		createAndAddNetwork(sc);
		Population pop = sc.getPopulation();

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

			new PersonPrepareForSim(new DummyRouter(), sc).run(person);

			Assert.assertEquals("wrong routing mode!", TransportMode.pt,
					TripStructureUtils.getRoutingMode(leg));
		}
		
		// test routing mode set
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

			new PersonPrepareForSim(new DummyRouter(), sc).run(person);

			Assert.assertEquals("wrong routing mode!", TransportMode.pt,
					TripStructureUtils.getRoutingMode(leg));
		}
	}
	
	/**
	 * Fallback modes are outdated with the introduction of routingMode. So, we want the simulation to crash if we encounter
	 * them <b>after</b> {@link PrepareForSimImpl} was run (and adapted outdated plans). However, for the time being we do not
	 * explicitly check for outdated modes and hope that an exception will be thrown during routing of that single leg trip, 
	 * because no router should be registered for those modes (and wasn't registered before introducing routingMode, besides 
	 * "transit_walk" which was also used for access/egress to pt and transfer between pt and therefore is
	 * checked explicitly).
	 */
	@Test
	public void testSingleFallbackModeLegTrip() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		createAndAddNetwork(sc);
		Population pop = sc.getPopulation();

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

			try {
				new PersonPrepareForSim(new DummyRouter(), sc).run(person);
				Assert.fail("expected Exception, got none.");
			} catch (RuntimeException expected) {}
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

			new PersonPrepareForSim(new DummyRouter(), sc).run(person);

			Assert.assertEquals("wrong leg mode replacement", "drt67_fallback", leg.getMode());
			Assert.assertEquals("wrong routing mode set", "drt67_fallback",
					TripStructureUtils.getRoutingMode(leg));
		}
	}
	
	@Test
	public void testCorrectTripsRemainUnchanged() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		createAndAddNetwork(sc);
		Population pop = sc.getPopulation();
		
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
			Activity activity2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) 0, -10),
					null, TransportMode.car);
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.car);
			TripStructureUtils.setRoutingMode(leg2, TransportMode.car);
			plan.addLeg(leg2);
			Activity activity3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) -10, -10),
					null, TransportMode.car);
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg3, TransportMode.car);
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity4);
			person.addPlan(plan);
			pop.addPerson(person);
			
			new PersonPrepareForSim(new DummyRouter(), sc).run(person);
			
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
			TripStructureUtils.setRoutingMode(leg1, TransportMode.pt);
			plan.addLeg(leg1);
			Activity activity2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) 0, -10), null, TransportMode.walk);
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg2, TransportMode.pt);
			plan.addLeg(leg2);
			Activity activity3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) 0, -10), null, TransportMode.walk);
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg3, TransportMode.pt);
			plan.addLeg(leg3);
			Activity activity4 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) 0, -10), null, TransportMode.drt);
			plan.addActivity(activity4);
			Leg leg4 = pf.createLeg(TransportMode.drt);
			TripStructureUtils.setRoutingMode(leg4, TransportMode.pt);
			plan.addLeg(leg4);
			Activity activity5 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) -10, -10), null, TransportMode.drt);
			plan.addActivity(activity5);
			Leg leg5 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg5, TransportMode.pt);
			plan.addLeg(leg5);
			Activity activity6 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) -10, -10), null, TransportMode.walk);
			plan.addActivity(activity6);
			Leg leg6 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg6, TransportMode.pt);
			plan.addLeg(leg6);
			Activity activity7 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) -20, -10), null, TransportMode.walk);
			plan.addActivity(activity7);
			Leg leg7 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg7, TransportMode.pt);
			plan.addLeg(leg7);
			Activity activity8 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) -20, -10), null, TransportMode.pt);
			plan.addActivity(activity8);
			Leg leg8 = pf.createLeg(TransportMode.pt);
			TripStructureUtils.setRoutingMode(leg8, TransportMode.pt);
			plan.addLeg(leg8);
			Activity activity9 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) 1800, -10), null, TransportMode.pt);
			plan.addActivity(activity9);
			Leg leg9 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg9, TransportMode.pt);
			plan.addLeg(leg9);
			Activity activity10 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) 1800, -10), null, TransportMode.walk);
			plan.addActivity(activity10);
			Leg leg10 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg10, TransportMode.pt);
			plan.addLeg(leg10);
			Activity activity11 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) 1900, -10), null, TransportMode.walk);
			plan.addActivity(activity11);
			Leg leg11 = pf.createLeg(TransportMode.non_network_walk);
			TripStructureUtils.setRoutingMode(leg11, TransportMode.pt);
			plan.addLeg(leg11);
			Activity activity12 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity12);
			person.addPlan(plan);
			pop.addPerson(person);
			
			new PersonPrepareForSim(new DummyRouter(), sc).run(person);
			
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg1));
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg2));
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg3));
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg4));
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg5));
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg6));
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg7));
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg8));
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg9));
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg10));
			Assert.assertEquals("wrong routing mode set", TransportMode.pt, TripStructureUtils.getRoutingMode(leg11));
		}
	}
		
	@Test
	public void testRoutingModeConsistency() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		createAndAddNetwork(sc);
		Population pop = sc.getPopulation();
		
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
			Activity activity2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) 0, -10), null, TransportMode.drt);
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.drt);
			TripStructureUtils.setRoutingMode(leg2, TransportMode.drt);
			plan.addLeg(leg2);
			Activity activity3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) -20, -10), null, TransportMode.drt);
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg3, TransportMode.drt);
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity4);
			person.addPlan(plan);
			pop.addPerson(person);
			
			try {
				new PersonPrepareForSim(new DummyRouter(), sc).run(person);
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
			Activity activity2 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) 0, -10), null, TransportMode.drt);
			plan.addActivity(activity2);
			Leg leg2 = pf.createLeg(TransportMode.drt);
			TripStructureUtils.setRoutingMode(leg2, TransportMode.drt);
			plan.addLeg(leg2);
			Activity activity3 = PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord((double) -20, -10), null, TransportMode.drt);
			plan.addActivity(activity3);
			Leg leg3 = pf.createLeg(TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg3, TransportMode.drt);
			plan.addLeg(leg3);
			Activity activity4 = pf.createActivityFromCoord("w", new Coord((double) 1900, -10));
			plan.addActivity(activity4);
			person.addPlan(plan);
			pop.addPerson(person);
			
			try {
				new PersonPrepareForSim(new DummyRouter(), sc).run(person);
				Assert.fail("expected Exception, got none.");
			} catch (RuntimeException expected) {}
		}
	}

	@Test
	public void testReplaceExperimentalTransitRoute() {
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		createAndAddNetwork(sc);
		Id<Link> startLink = Id.createLinkId("1");
		Id<Link> endLink = Id.createLinkId("2");
		TransitStopFacility stopFacility1 = sc.getTransitSchedule().getFactory().createTransitStopFacility(
				Id.create("stop1", TransitStopFacility.class),
				sc.getNetwork().getLinks().get(startLink).getToNode().getCoord(),
				false);
		stopFacility1.setLinkId(startLink);
		sc.getTransitSchedule().addStopFacility(stopFacility1);
		TransitStopFacility stopFacility2 = sc.getTransitSchedule().getFactory().createTransitStopFacility(
				Id.create("stop2", TransitStopFacility.class),
				sc.getNetwork().getLinks().get(endLink).getToNode().getCoord(),
				false);
		stopFacility2.setLinkId(endLink);
		sc.getTransitSchedule().addStopFacility(stopFacility2);
		Population pop = sc.getPopulation();
		PopulationFactory pf = pop.getFactory();
		Person person = pf.createPerson(Id.create("2", Person.class));
		Plan plan = pf.createPlan();
		Activity activity1 = pf.createActivityFromLinkId("h", startLink);
		plan.addActivity(activity1);
		Leg leg = pf.createLeg(TransportMode.pt);
		TripStructureUtils.setRoutingMode(leg, TransportMode.pt);
		Id<TransitLine> line = Id.create("line", TransitLine.class);
		Id<TransitRoute> route = Id.create("route", TransitRoute.class);
		ExperimentalTransitRoute experimentalTransitRoute = new ExperimentalTransitRoute(
				stopFacility1, stopFacility2, line, route);
		leg.setRoute(experimentalTransitRoute);
		plan.addLeg(leg);
		Activity activity2 = pf.createActivityFromLinkId("w", endLink);
		plan.addActivity(activity2);
		person.addPlan(plan);
		pop.addPerson(person);

		new PersonPrepareForSim(new DummyRouter(), sc).run(person);

		Assert.assertEquals(DefaultTransitPassengerRoute.ROUTE_TYPE, leg.getRoute().getRouteType());
		Assert.assertEquals(startLink, leg.getRoute().getStartLinkId());
		Assert.assertEquals(endLink, leg.getRoute().getEndLinkId());
		Assert.assertEquals(stopFacility1.getId(), ((DefaultTransitPassengerRoute) leg.getRoute()).getAccessStopId());
		Assert.assertEquals(stopFacility2.getId(), ((DefaultTransitPassengerRoute) leg.getRoute()).getEgressStopId());
		Assert.assertEquals(line, ((DefaultTransitPassengerRoute) leg.getRoute()).getLineId());
		Assert.assertEquals(route, ((DefaultTransitPassengerRoute) leg.getRoute()).getRouteId());
	}

	private static class DummyRouter implements PlanAlgorithm {
		@Override
		public void run(final Plan plan) {
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
