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

package playground.marcel.pt.queuesim;

import java.util.Collections;

import junit.framework.TestCase;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitScheduleFactory;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.pt.routes.ExperimentalTransitRoute;

/**
 * @author mrieser
 */
public class TransitAgentTest extends TestCase {

	public void testAcceptLineRoute() {
		ScenarioImpl scenario = new ScenarioImpl();

		NetworkLayer network = scenario.getNetwork();
		NodeImpl node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(   0, 0));
		NodeImpl node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1000, 0));
		NodeImpl node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(2000, 0));
		network.createAndAddLink(new IdImpl("1"), node1, node2, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("2"), node2, node3, 1000.0, 10.0, 3600.0, 1);

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PopulationFactory pb = scenario.getPopulation().getFactory();
		Person person = pb.createPerson(scenario.createId("1"));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", scenario.createId("1"));
		Leg leg = pb.createLeg(TransportMode.pt);
		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("1"), scenario.createCoord(100, 100), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("2"), scenario.createCoord(900, 100), false);
		TransitLine line1 = builder.createTransitLine(scenario.createId("L1"));
		TransitLine line2 = builder.createTransitLine(scenario.createId("L2"));
		TransitRoute route1a = builder.createTransitRoute(scenario.createId("1a"), null, Collections.<TransitRouteStop>emptyList(), TransportMode.pt);
		TransitRoute route1b = builder.createTransitRoute(scenario.createId("1b"), null, Collections.<TransitRouteStop>emptyList(), TransportMode.pt);
		TransitRoute route2a = builder.createTransitRoute(scenario.createId("2a"), null, Collections.<TransitRouteStop>emptyList(), TransportMode.pt);
		leg.setRoute(new ExperimentalTransitRoute(stop1, line1, route1a, stop2));
		Activity workAct = pb.createActivityFromLinkId("work", scenario.createId("2"));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);

		QueueSimulation sim = new QueueSimulation(scenario, new EventsManagerImpl());
		TransitAgent agent = new TransitAgent((PersonImpl) person, sim);
		agent.initialize();
		agent.activityEnds(10);
		assertTrue(agent.getEnterTransitRoute(line1, route1a));
		assertFalse(agent.getEnterTransitRoute(line1, route1b));
		assertFalse(agent.getEnterTransitRoute(line2, route2a));
		assertTrue(agent.getEnterTransitRoute(line1, route1a)); // offering the same line again should yield "true"
	}

	public void testArriveAtStop() {
		ScenarioImpl scenario = new ScenarioImpl();

		NetworkLayer network = scenario.getNetwork();
		NodeImpl node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(   0, 0));
		NodeImpl node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(1000, 0));
		NodeImpl node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(2000, 0));
		network.createAndAddLink(new IdImpl("1"), node1, node2, 1000.0, 10.0, 3600.0, 1);
		network.createAndAddLink(new IdImpl("2"), node2, node3, 1000.0, 10.0, 3600.0, 1);

		TransitScheduleFactory builder = new TransitScheduleFactoryImpl();
		PopulationFactory pb = scenario.getPopulation().getFactory();
		Person person = pb.createPerson(scenario.createId("1"));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", scenario.createId("1"));
		Leg leg = pb.createLeg(TransportMode.pt);
		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("1"), scenario.createCoord(100, 100), false);
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("2"), scenario.createCoord(900, 100), false);
		TransitStopFacility stop3 = builder.createTransitStopFacility(scenario.createId("3"), scenario.createCoord(1900, 100), false);
		TransitLine line1 = builder.createTransitLine(scenario.createId("L1"));
		leg.setRoute(new ExperimentalTransitRoute(stop1, line1, null, stop2));
		Activity workAct = pb.createActivityFromLinkId("work", scenario.createId("2"));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);

		QueueSimulation sim = new QueueSimulation(scenario, new EventsManagerImpl());
		TransitAgent agent = new TransitAgent((PersonImpl) person, sim);
		agent.initialize();
		agent.activityEnds(10);
		assertFalse(agent.getExitAtStop(stop1));
		assertTrue(agent.getExitAtStop(stop2));
		assertFalse(agent.getExitAtStop(stop3));
		assertTrue(agent.getExitAtStop(stop2)); // offering the same stop again should yield "true"
	}

}
