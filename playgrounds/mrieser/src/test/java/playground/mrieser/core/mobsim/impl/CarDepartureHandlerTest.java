/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.mobsim.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.apache.log4j.Level;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.testcases.utils.LogCounter;

import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.PlanElementHandler;
import playground.mrieser.core.mobsim.api.SimVehicle;
import playground.mrieser.core.mobsim.api.TimestepSimEngine;
import playground.mrieser.core.mobsim.fakes.FakeSimVehicle;
import playground.mrieser.core.mobsim.features.NetworkFeature;
import playground.mrieser.core.mobsim.features.refQueueNetworkFeature.RefQueueNetworkFeature;
import playground.mrieser.core.mobsim.network.api.MobSimLink;

/**
 * @author mrieser
 */
public class CarDepartureHandlerTest {

	@Test
	public void testHandleDeparture() {
		Fixture f = new Fixture();

		CarDepartureHandler cdh = new CarDepartureHandler(f.simEngine, f.networkFeature, f.scenario);

		Plan plan = f.scenario.getPopulation().getFactory().createPlan();
		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("home", f.ids[0]));
		Leg leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(f.ids[0], f.ids[2]);
		List<Id> linkIds = new ArrayList<Id>(1);
		Collections.addAll(linkIds, f.ids[1], f.ids[2]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[3]);
		leg.setRoute(route);
		plan.addLeg(leg);
		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("work", f.ids[3]));
		plan.setPerson(f.scenario.getPopulation().getFactory().createPerson(f.ids[0]));
		PlanAgent agent = new DefaultPlanAgent(plan);
		MobSimLink link = f.networkFeature.getSimNetwork().getLinks().get(f.ids[0]);
		SimVehicle veh = new FakeSimVehicle(f.ids[0]);
		link.insertVehicle(veh, MobSimLink.POSITION_AT_TO_NODE, MobSimLink.PRIORITY_PARKING);
		Assert.assertEquals(veh, link.getParkedVehicle(veh.getId()));
		agent.useNextPlanElement(); // homeAct
		agent.useNextPlanElement(); // leg
		cdh.handleDeparture(agent);
		Assert.assertNull(link.getParkedVehicle(veh.getId()));
	}

	@Test
	public void testHandleDeparture_NextActOnSameLink() {
		Fixture f = new Fixture();
		CountingPlanElementHandler legCounter = new CountingPlanElementHandler();
		CountingPlanElementHandler actCounter = new CountingPlanElementHandler();
		f.sim.setPlanElementHandler(Leg.class, legCounter);
		f.sim.setPlanElementHandler(Activity.class, actCounter);

		CarDepartureHandler cdh = new CarDepartureHandler(f.simEngine, f.networkFeature, f.scenario);

		Plan plan = f.scenario.getPopulation().getFactory().createPlan();

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("home", f.ids[0]));

		Leg leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(f.ids[0], f.ids[3]);
		List<Id> linkIds = new ArrayList<Id>(1);
		Collections.addAll(linkIds, f.ids[1], f.ids[2]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[3]);
		leg.setRoute(route);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("work", f.ids[3]));

		leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(f.ids[3], f.ids[3]);
		linkIds = new ArrayList<Id>(1);
		route.setLinkIds(f.ids[3], linkIds, f.ids[3]);
		leg.setRoute(route);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("leisure", f.ids[3]));

		plan.setPerson(f.scenario.getPopulation().getFactory().createPerson(f.ids[0]));
		PlanAgent agent = new DefaultPlanAgent(plan);
		MobSimLink link = f.networkFeature.getSimNetwork().getLinks().get(f.ids[3]);
		SimVehicle veh = new FakeSimVehicle(f.ids[0]);
		link.insertVehicle(veh, MobSimLink.POSITION_AT_TO_NODE, MobSimLink.PRIORITY_PARKING);
		Assert.assertEquals(veh, link.getParkedVehicle(veh.getId()));
		agent.useNextPlanElement(); // homeAct
		agent.useNextPlanElement(); // leg
		agent.useNextPlanElement(); // workAct
		agent.useNextPlanElement(); // leg w/ empty route
		Assert.assertEquals(0, actCounter.countStart);
		Assert.assertEquals(0, legCounter.countStart);
		cdh.handleDeparture(agent);
		Assert.assertEquals(1, actCounter.countStart);
		Assert.assertEquals(1, legCounter.countEnd);
		Assert.assertEquals("vehicle should still be parked.", veh, link.getParkedVehicle(veh.getId()));
	}

	@Test
	public void testHandleDeparture_vehicleConsistency() {
		Fixture f = new Fixture();
		CountingPlanElementHandler legCounter = new CountingPlanElementHandler();
		CountingPlanElementHandler actCounter = new CountingPlanElementHandler();
		f.sim.setPlanElementHandler(Leg.class, legCounter);
		f.sim.setPlanElementHandler(Activity.class, actCounter);

		CarDepartureHandler cdh = new CarDepartureHandler(f.simEngine, f.networkFeature, f.scenario);

		Plan plan = f.scenario.getPopulation().getFactory().createPlan();

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("home", f.ids[0]));

		Leg leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(f.ids[0], f.ids[2]);
		List<Id> linkIds = new ArrayList<Id>(1);
		Collections.addAll(linkIds, f.ids[1]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[2]);
		leg.setRoute(route);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("work", f.ids[2]));

		leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(f.ids[2], f.ids[3]);
		route.setLinkIds(f.ids[2], new ArrayList<Id>(0), f.ids[3]);
		leg.setRoute(route);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("leisure", f.ids[3]));

		leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(f.ids[3], f.ids[4]);
		route.setLinkIds(f.ids[3], new ArrayList<Id>(0), f.ids[4]);
		leg.setRoute(route);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("shop", f.ids[4]));

		Id personId = f.ids[0];
		plan.setPerson(f.scenario.getPopulation().getFactory().createPerson(personId));
		PlanAgent agent = new DefaultPlanAgent(plan);

		f.networkFeature.doSimStep(0);

		agent.useNextPlanElement(); // home
		agent.useNextPlanElement(); // leg
		cdh.handleDeparture(agent);

		f.networkFeature.doSimStep(10); // agent moved to link0.buffer
		f.networkFeature.doSimStep(11); // agent moved to link1
		f.networkFeature.doSimStep(100); // agent moved to link1.buffer
		f.networkFeature.doSimStep(101); // agent moved to link2
		f.networkFeature.doSimStep(200); // agent arrives, work

		SimVehicle vehicle = f.networkFeature.getSimNetwork().getLinks().get(f.ids[2]).getParkedVehicle(personId);
		Assert.assertNotNull(vehicle);

		Assert.assertTrue(agent.useNextPlanElement() instanceof Leg); // leg
		cdh.handleDeparture(agent);

		f.networkFeature.doSimStep(210); // agent moved to link2.buffer
		f.networkFeature.doSimStep(211); // agent moved to link3
		f.networkFeature.doSimStep(300); // agent arrives, leisure

		Assert.assertEquals(vehicle, f.networkFeature.getSimNetwork().getLinks().get(f.ids[3]).getParkedVehicle(personId));
		Assert.assertTrue(agent.useNextPlanElement() instanceof Leg); // leg
		cdh.handleDeparture(agent);

		f.networkFeature.doSimStep(310); // agent moved to link3.buffer
		f.networkFeature.doSimStep(311); // agent moved to link4
		f.networkFeature.doSimStep(400); // agent arrives, shop

		Assert.assertEquals(vehicle, f.networkFeature.getSimNetwork().getLinks().get(f.ids[4]).getParkedVehicle(personId));
		Assert.assertNull(agent.useNextPlanElement());
	}

	@Test
	public void testHandleDeparture_vehicleConsistency_NoTeleportation() {
		Fixture f = new Fixture();
		CountingPlanElementHandler legCounter = new CountingPlanElementHandler();
		CountingPlanElementHandler actCounter = new CountingPlanElementHandler();
		f.sim.setPlanElementHandler(Leg.class, legCounter);
		f.sim.setPlanElementHandler(Activity.class, actCounter);

		CarDepartureHandler cdh = new CarDepartureHandler(f.simEngine, f.networkFeature, f.scenario);

		Plan plan = f.scenario.getPopulation().getFactory().createPlan();

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("home", f.ids[0]));

		Leg leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(f.ids[0], f.ids[2]);
		List<Id> linkIds = new ArrayList<Id>(1);
		Collections.addAll(linkIds, f.ids[1]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[2]);
		leg.setRoute(route);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("work", f.ids[2]));

		leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.walk); // WALK, vehicle will be missing!
		leg.setTravelTime(5.0);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("leisure", f.ids[3]));

		leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(f.ids[3], f.ids[4]);
		route.setLinkIds(f.ids[3], new ArrayList<Id>(0), f.ids[4]);
		leg.setRoute(route);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("shop", f.ids[4]));

		Id personId = f.ids[0];
		plan.setPerson(f.scenario.getPopulation().getFactory().createPerson(personId));
		PlanAgent agent = new DefaultPlanAgent(plan);

		f.networkFeature.doSimStep(0);

		agent.useNextPlanElement(); // home
		agent.useNextPlanElement(); // leg car
		cdh.handleDeparture(agent);

		f.networkFeature.doSimStep(10); // agent moved to link0.buffer
		f.networkFeature.doSimStep(11); // agent moved to link1
		f.networkFeature.doSimStep(100); // agent moved to link1.buffer
		f.networkFeature.doSimStep(101); // agent moved to link2
		f.networkFeature.doSimStep(200); // agent arrives, work

		SimVehicle vehicle = f.networkFeature.getSimNetwork().getLinks().get(f.ids[2]).getParkedVehicle(personId);
		Assert.assertNotNull(vehicle);

		Assert.assertTrue(agent.useNextPlanElement() instanceof Leg); // leg walk
		Assert.assertTrue(agent.useNextPlanElement() instanceof Activity); // leisure
		Assert.assertTrue(agent.useNextPlanElement() instanceof Leg); // leg walk

		LogCounter counter = new LogCounter(Level.ERROR);
		counter.activiate();
		cdh.handleDeparture(agent);
		counter.deactiviate();
		Assert.assertEquals(1, counter.getErrorCount());
	}

	@Test
	public void testHandleDeparture_vehicleConsistency_WithTeleportation() {
		Fixture f = new Fixture();
		CountingPlanElementHandler legCounter = new CountingPlanElementHandler();
		CountingPlanElementHandler actCounter = new CountingPlanElementHandler();
		f.sim.setPlanElementHandler(Leg.class, legCounter);
		f.sim.setPlanElementHandler(Activity.class, actCounter);

		CarDepartureHandler cdh = new CarDepartureHandler(f.simEngine, f.networkFeature, f.scenario);
		cdh.setTeleportVehicles(true);

		Plan plan = f.scenario.getPopulation().getFactory().createPlan();

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("home", f.ids[0]));

		Leg leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(f.ids[0], f.ids[2]);
		List<Id> linkIds = new ArrayList<Id>(1);
		Collections.addAll(linkIds, f.ids[1]);
		route.setLinkIds(f.ids[0], linkIds, f.ids[2]);
		leg.setRoute(route);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("work", f.ids[2]));

		leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.walk); // WALK, vehicle will be missing!
		leg.setTravelTime(5.0);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("leisure", f.ids[3]));

		leg = f.scenario.getPopulation().getFactory().createLeg(TransportMode.car);
		route = new LinkNetworkRouteImpl(f.ids[3], f.ids[4]);
		route.setLinkIds(f.ids[3], new ArrayList<Id>(0), f.ids[4]);
		leg.setRoute(route);
		plan.addLeg(leg);

		plan.addActivity(f.scenario.getPopulation().getFactory().createActivityFromLinkId("shop", f.ids[4]));

		Id personId = f.ids[0];
		plan.setPerson(f.scenario.getPopulation().getFactory().createPerson(personId));
		PlanAgent agent = new DefaultPlanAgent(plan);

		f.networkFeature.doSimStep(0);

		agent.useNextPlanElement(); // home
		agent.useNextPlanElement(); // leg car
		cdh.handleDeparture(agent);

		f.networkFeature.doSimStep(10); // agent moved to link0.buffer
		f.networkFeature.doSimStep(11); // agent moved to link1
		f.networkFeature.doSimStep(100); // agent moved to link1.buffer
		f.networkFeature.doSimStep(101); // agent moved to link2
		f.networkFeature.doSimStep(200); // agent arrives, work

		SimVehicle vehicle = f.networkFeature.getSimNetwork().getLinks().get(f.ids[2]).getParkedVehicle(personId);
		Assert.assertNotNull(vehicle);

		Assert.assertTrue(agent.useNextPlanElement() instanceof Leg); // leg walk
		Assert.assertTrue(agent.useNextPlanElement() instanceof Activity); // leisure
		Assert.assertTrue(agent.useNextPlanElement() instanceof Leg); // leg walk

		LogCounter counter = new LogCounter(Level.WARN);
		counter.activiate();
		cdh.handleDeparture(agent); // vehicle will be teleported from link2 to link3
		counter.deactiviate();
		Assert.assertEquals(1, counter.getWarnCount());

		f.networkFeature.doSimStep(310); // agent moved to link3.buffer
		f.networkFeature.doSimStep(311); // agent moved to link4
		f.networkFeature.doSimStep(400); // agent arrives, work

		Assert.assertEquals(vehicle, f.networkFeature.getSimNetwork().getLinks().get(f.ids[4]).getParkedVehicle(personId));
	}

	/**
	 * Creates a simple network with 6 nodes and 5 links in a row.
	 *
	 * @author mrieser
	 */
	private static class Fixture {

		public final Id[] ids = new Id[10];
		public final Scenario scenario;
		public final NetworkFeature networkFeature;
		public final TimestepSimEngine simEngine;
		public final PlanSimulationImpl sim;

		public Fixture() {
			this.scenario = new ScenarioImpl();

			for (int i = 0; i < this.ids.length; i++) {
				this.ids[i] = this.scenario.createId(Integer.toString(i));
			}

			Network network = this.scenario.getNetwork();
			NetworkFactory netFactory = network.getFactory();
			network.addNode(netFactory.createNode(this.ids[0], this.scenario.createCoord(0, 0)));
			network.addNode(netFactory.createNode(this.ids[1], this.scenario.createCoord(500, 0)));
			network.addNode(netFactory.createNode(this.ids[2], this.scenario.createCoord(1000, 0)));
			network.addNode(netFactory.createNode(this.ids[3], this.scenario.createCoord(1500, 0)));
			network.addNode(netFactory.createNode(this.ids[4], this.scenario.createCoord(2000, 0)));
			network.addNode(netFactory.createNode(this.ids[5], this.scenario.createCoord(2500, 0)));
			network.addLink(setLinkAttributes(netFactory.createLink(this.ids[0], this.ids[0], this.ids[1]), 500.0, 3600.0, 10.0, 1));
			network.addLink(setLinkAttributes(netFactory.createLink(this.ids[1], this.ids[1], this.ids[2]), 500.0, 3600.0, 10.0, 1));
			network.addLink(setLinkAttributes(netFactory.createLink(this.ids[2], this.ids[2], this.ids[3]), 500.0, 3600.0, 10.0, 1));
			network.addLink(setLinkAttributes(netFactory.createLink(this.ids[3], this.ids[3], this.ids[4]), 500.0, 3600.0, 10.0, 1));
			network.addLink(setLinkAttributes(netFactory.createLink(this.ids[4], this.ids[4], this.ids[5]), 500.0, 3600.0, 10.0, 1));

			EventsManager events = new EventsManagerImpl();
			this.sim = new PlanSimulationImpl(this.scenario);
			this.simEngine = new DefaultTimestepSimEngine(this.sim, events);
			this.networkFeature = new RefQueueNetworkFeature(network, this.simEngine);
		}

		private static Link setLinkAttributes(final Link link, final double length, final double capacity, final double freespeed, final int nOfLanes) {
			link.setLength(length);
			link.setCapacity(capacity);
			link.setFreespeed(freespeed);
			link.setNumberOfLanes(nOfLanes);
			return link;
		}
	}

	/*package*/ static class CountingPlanElementHandler implements PlanElementHandler {
		public int countStart = 0;
		public int countEnd = 0;
		@Override
		public void handleStart(final PlanAgent agent) {
			this.countStart++;
		}
		@Override
		public void handleEnd(final PlanAgent agent) {
			this.countEnd++;
		}
	}

}
