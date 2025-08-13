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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
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
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.mobsim.qsim.agents.TransitAgent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;


/**
 * @author mrieser
 */
public class TransitAgentTest {

	@Test
	void testAcceptLineRoute() {
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		Network network = (Network) scenario.getNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, 1000.0, 10.0, 3600.0, (double) 1 );

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PopulationFactory pb = scenario.getPopulation().getFactory();
		Person person = pb.createPerson(Id.create("1", Person.class));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", Id.create("1", Link.class));
		Leg leg = pb.createLeg(TransportMode.pt);
		TransitStopFacility stopFacility1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 100, (double) 100), false);
		TransitStopFacility stopFacility2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 900, (double) 100), false);
		TransitRouteStop stop1 = builder.createTransitRouteStop(stopFacility1, 50, 60);
		TransitRouteStop stop2 = builder.createTransitRouteStop(stopFacility2, 100, 110);
		TransitLine line1 = builder.createTransitLine(Id.create("L1", TransitLine.class));
		TransitLine line2 = builder.createTransitLine(Id.create("L2", TransitLine.class));
		TransitRoute route1a = builder.createTransitRoute(Id.create("1a", TransitRoute.class), null, Arrays.asList(stop1, stop2), TransportMode.pt);
		TransitRoute route1b = builder.createTransitRoute(Id.create("1b", TransitRoute.class), null, Collections.<TransitRouteStop>emptyList(), TransportMode.pt);
		TransitRoute route2a = builder.createTransitRoute(Id.create("2a", TransitRoute.class), null, Collections.<TransitRouteStop>emptyList(), TransportMode.pt);
		leg.setRoute(new DefaultTransitPassengerRoute(stopFacility1, line1, route1a, stopFacility2));
		Activity workAct = pb.createActivityFromLinkId("work", Id.create("2", Link.class));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim sim = new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, eventsManager);
		TransitAgent agent = TransitAgent.createTransitAgent(person, sim, sim.getChildInjector().getInstance(TimeInterpretation.class));
		sim.insertAgentIntoMobsim(agent);
		agent.endActivityAndComputeNextState(10);

		assertTrue(agent.getEnterTransitRoute(line1, route1a, route1a.getStops(), null));
		assertFalse(agent.getEnterTransitRoute(line1, route1b, route1b.getStops(), null));
		assertFalse(agent.getEnterTransitRoute(line2, route2a, route2a.getStops(), null));
		assertTrue(agent.getEnterTransitRoute(line1, route1a, route1a.getStops(), null)); // offering the same line again should yield "true"
	}

	@Test
	void testArriveAtStop() {
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		Network network = (Network) scenario.getNetwork();
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 1000, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 2000, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, 1000.0, 10.0, 3600.0, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, 1000.0, 10.0, 3600.0, (double) 1 );

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PopulationFactory pb = scenario.getPopulation().getFactory();
		Person person = pb.createPerson(Id.create("1", Person.class));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", Id.create("1", Link.class));
		Leg leg = pb.createLeg(TransportMode.pt);
		TransitStopFacility stop1 = builder.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 100, (double) 100), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(Id.create("2", TransitStopFacility.class), new Coord((double) 900, (double) 100), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(Id.create("3", TransitStopFacility.class), new Coord((double) 1900, (double) 100), false);
		TransitLine line1 = builder.createTransitLine(Id.create("L1", TransitLine.class));
		leg.setRoute(new DefaultTransitPassengerRoute(stop1, line1, null, stop2));
		Activity workAct = pb.createActivityFromLinkId("work", Id.create("2", Link.class));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim sim = new QSimBuilder(scenario.getConfig()) //
				.useDefaults() //
				.build(scenario, eventsManager);
		TransitAgent agent = TransitAgent.createTransitAgent(person, sim, sim.getChildInjector().getInstance(TimeInterpretation.class));
		sim.insertAgentIntoMobsim(agent);
		agent.endActivityAndComputeNextState(10);

		assertFalse(agent.getExitAtStop(stop1));
		assertTrue(agent.getExitAtStop(stop2));
		assertFalse(agent.getExitAtStop(stop3));
		assertTrue(agent.getExitAtStop(stop2)); // offering the same stop again should yield "true"
	}

}
