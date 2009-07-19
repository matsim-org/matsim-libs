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

import junit.framework.TestCase;

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.api.experimental.population.Activity;
import org.matsim.core.api.experimental.population.Leg;
import org.matsim.core.api.experimental.population.Person;
import org.matsim.core.api.experimental.population.Plan;
import org.matsim.core.api.experimental.population.PopulationBuilder;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.Events;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.transitSchedule.TransitScheduleBuilderImpl;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitScheduleBuilder;
import org.matsim.transitSchedule.api.TransitStopFacility;

import playground.marcel.pt.routes.ExperimentalTransitRoute;

/**
 * @author mrieser
 */
public class TransitAgentTest extends TestCase {

	public void testAcceptLineRoute() {
		ScenarioImpl scenario = new ScenarioImpl();

		NetworkLayer network = scenario.getNetwork();
		NodeImpl node1 = network.createNode(new IdImpl("1"), new CoordImpl(   0, 0));
		NodeImpl node2 = network.createNode(new IdImpl("2"), new CoordImpl(1000, 0));
		NodeImpl node3 = network.createNode(new IdImpl("3"), new CoordImpl(2000, 0));
		network.createLink(new IdImpl("1"), node1, node2, 1000.0, 10.0, 3600.0, 1);
		network.createLink(new IdImpl("2"), node2, node3, 1000.0, 10.0, 3600.0, 1);

		TransitScheduleBuilder builder = new TransitScheduleBuilderImpl();
		PopulationBuilder pb = scenario.getPopulation().getBuilder();
		Person person = pb.createPerson(scenario.createId("1"));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", scenario.createId("1"));
		Leg leg = pb.createLeg(TransportMode.pt);
		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("1"), scenario.createCoord(100, 100));
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("2"), scenario.createCoord(900, 100));
		TransitLine line1 = builder.createTransitLine(scenario.createId("L1"));
		TransitLine line2 = builder.createTransitLine(scenario.createId("L2"));
		leg.setRoute(new ExperimentalTransitRoute(stop1, line1, stop2));
		Activity workAct = pb.createActivityFromLinkId("work", scenario.createId("2"));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);

		QueueSimulation sim = new QueueSimulation(scenario, new Events());
		TransitAgent agent = new TransitAgent((PersonImpl) person, sim);
		agent.initialize();
		agent.activityEnds(10);
		assertTrue(agent.ptLineAvailable(line1));
		assertFalse(agent.ptLineAvailable(line2));
		assertTrue(agent.ptLineAvailable(line1)); // offering the same line again should yield "true"
	}

	public void testArriveAtStop() {
		ScenarioImpl scenario = new ScenarioImpl();

		NetworkLayer network = scenario.getNetwork();
		NodeImpl node1 = network.createNode(new IdImpl("1"), new CoordImpl(   0, 0));
		NodeImpl node2 = network.createNode(new IdImpl("2"), new CoordImpl(1000, 0));
		NodeImpl node3 = network.createNode(new IdImpl("3"), new CoordImpl(2000, 0));
		network.createLink(new IdImpl("1"), node1, node2, 1000.0, 10.0, 3600.0, 1);
		network.createLink(new IdImpl("2"), node2, node3, 1000.0, 10.0, 3600.0, 1);

		TransitScheduleBuilder builder = new TransitScheduleBuilderImpl();
		PopulationBuilder pb = scenario.getPopulation().getBuilder();
		Person person = pb.createPerson(scenario.createId("1"));
		Plan plan = pb.createPlan();
		person.addPlan(plan);
		Activity homeAct = pb.createActivityFromLinkId("home", scenario.createId("1"));
		Leg leg = pb.createLeg(TransportMode.pt);
		TransitStopFacility stop1 = builder.createTransitStopFacility(scenario.createId("1"), scenario.createCoord(100, 100));
		TransitStopFacility stop2 = builder.createTransitStopFacility(scenario.createId("2"), scenario.createCoord(900, 100));
		TransitStopFacility stop3 = builder.createTransitStopFacility(scenario.createId("3"), scenario.createCoord(1900, 100));
		TransitLine line1 = builder.createTransitLine(scenario.createId("L1"));
		leg.setRoute(new ExperimentalTransitRoute(stop1, line1, stop2));
		Activity workAct = pb.createActivityFromLinkId("work", scenario.createId("2"));
		plan.addActivity(homeAct);
		plan.addLeg(leg);
		plan.addActivity(workAct);

		QueueSimulation sim = new QueueSimulation(scenario, new Events());
		TransitAgent agent = new TransitAgent((PersonImpl) person, sim);
		agent.initialize();
		agent.activityEnds(10);
		assertFalse(agent.arriveAtStop(stop1));
		assertTrue(agent.arriveAtStop(stop2));
		assertFalse(agent.arriveAtStop(stop3));
		assertTrue(agent.arriveAtStop(stop2)); // offering the same stop again should yield "true"
	}

}
