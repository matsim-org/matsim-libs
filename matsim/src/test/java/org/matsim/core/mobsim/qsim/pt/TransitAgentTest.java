/* *********************************************************************** *
 * project: org.matsim.*
 * TransitAgentTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.pt;

import junit.framework.TestCase;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.*;

import java.util.Arrays;
import java.util.Collections;


/**
 * @author mrieser
 */
public class TransitAgentTest extends TestCase {

	public void testAcceptLineRoute() {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new CoordImpl(   0, 0));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new CoordImpl(1000, 0));
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new CoordImpl(2000, 0));
		network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1000.0, 10.0, 3600.0, 1);

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PopulationFactory pb = scenario.getPopulation().getFactory();
		Person person = pb.createPerson(Id.create("1", Person.class));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", Id.create("1", Link.class));
		Leg leg = pb.createLeg(TransportMode.pt);
		TransitStopFacility stopFacility1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), scenario.createCoord(100, 100), false);
		TransitStopFacility stopFacility2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), scenario.createCoord(900, 100), false);
		TransitRouteStop stop1 = builder.createTransitRouteStop(stopFacility1, 50, 60);
		TransitRouteStop stop2 = builder.createTransitRouteStop(stopFacility2, 100, 110);
		TransitLine line1 = builder.createTransitLine(Id.create("L1", TransitLine.class));
		TransitLine line2 = builder.createTransitLine(Id.create("L2", TransitLine.class));
		TransitRoute route1a = builder.createTransitRoute(Id.create("1a", TransitRoute.class), null, Arrays.asList(stop1, stop2), TransportMode.pt);
		TransitRoute route1b = builder.createTransitRoute(Id.create("1b", TransitRoute.class), null, Collections.<TransitRouteStop>emptyList(), TransportMode.pt);
		TransitRoute route2a = builder.createTransitRoute(Id.create("2a", TransitRoute.class), null, Collections.<TransitRouteStop>emptyList(), TransportMode.pt);
		leg.setRoute(new ExperimentalTransitRoute(stopFacility1, line1, route1a, stopFacility2));
		Activity workAct = pb.createActivityFromLinkId("work", Id.create("2", Link.class));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);

		QSim sim = (QSim) QSimUtils.createDefaultQSim(scenario, EventsUtils.createEventsManager());
		TransitAgent agent = TransitAgent.createTransitAgent(person, sim);
		sim.insertAgentIntoMobsim(agent);
		agent.endActivityAndComputeNextState(10);

		assertTrue(agent.getEnterTransitRoute(line1, route1a, route1a.getStops(), null));
		assertFalse(agent.getEnterTransitRoute(line1, route1b, route1b.getStops(), null));
		assertFalse(agent.getEnterTransitRoute(line2, route2a, route2a.getStops(), null));
		assertTrue(agent.getEnterTransitRoute(line1, route1a, route1a.getStops(), null)); // offering the same line again should yield "true"
	}

	public void testArriveAtStop() {
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		Node node1 = network.createAndAddNode(Id.create("1", Node.class), new CoordImpl(   0, 0));
		Node node2 = network.createAndAddNode(Id.create("2", Node.class), new CoordImpl(1000, 0));
		Node node3 = network.createAndAddNode(Id.create("3", Node.class), new CoordImpl(2000, 0));
		network.createAndAddLink(Id.create("1", Link.class), node1, node2, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(Id.create("2", Link.class), node2, node3, 1000.0, 10.0, 3600.0, 1);

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PopulationFactory pb = scenario.getPopulation().getFactory();
		Person person = pb.createPerson(Id.create("1", Person.class));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", Id.create("1", Link.class));
		Leg leg = pb.createLeg(TransportMode.pt);
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), scenario.createCoord(100, 100), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), scenario.createCoord(900, 100), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("3", TransitStopFacility.class), scenario.createCoord(1900, 100), false);
		TransitLine line1 = builder.createTransitLine(Id.create("L1", TransitLine.class));
		leg.setRoute(new ExperimentalTransitRoute(stop1, line1, null, stop2));
		Activity workAct = pb.createActivityFromLinkId("work", Id.create("2", Link.class));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);

		QSim sim = (QSim) QSimUtils.createDefaultQSim(scenario, EventsUtils.createEventsManager());
		TransitAgent agent = TransitAgent.createTransitAgent(person, sim);
		sim.insertAgentIntoMobsim(agent);
		agent.endActivityAndComputeNextState(10);

		assertFalse(agent.getExitAtStop(stop1));
		assertTrue(agent.getExitAtStop(stop2));
		assertFalse(agent.getExitAtStop(stop3));
		assertTrue(agent.getExitAtStop(stop2)); // offering the same stop again should yield "true"
	}

}
