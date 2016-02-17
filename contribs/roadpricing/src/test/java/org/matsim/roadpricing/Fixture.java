/* *********************************************************************** *
 * project: org.matsim.*
 * Fixture.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.roadpricing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.misc.Time;

import junit.framework.TestCase;

/**
 * Some static methods to set up the road pricing scenarios in the test cases.
 *
 * @author mrieser
 */
/*package*/ class Fixture {

	private Fixture() {
		// static class
	}

	/** Creates a simple network consisting of 5 equal links in a row. */
	static void createNetwork1(final MutableScenario scenario) {
		/* This creates the following network:
		 *
		 * (1)-------(2)-------(3)-------(4)-------(5)-------(6)
		 *       0         1         2         3         4
		 */
		/* The vehicles can travel with 18km/h = 5m/s, so it should take them 20 seconds
		 * to travel along one link.		 */
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(Time.parseTime("01:00:00"));
		Node node1 = network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 0, (double) 0));
		Node node2 = network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 100, (double) 0));
		Node node3 = network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 200, (double) 0));
		Node node4 = network.createAndAddNode(Id.create(4, Node.class), new Coord((double) 300, (double) 0));
		Node node5 = network.createAndAddNode(Id.create(5, Node.class), new Coord((double) 400, (double) 0));
		Node node6 = network.createAndAddNode(Id.create(6, Node.class), new Coord((double) 500, (double) 0));
		// freespeed 18km/h = 5m/s --> 20s for 100m
		network.createAndAddLink(Id.create(0, Link.class), node1, node2, 100, 5, 100, 1);
		network.createAndAddLink(Id.create(1, Link.class), node2, node3, 100, 5, 100, 1);
		network.createAndAddLink(Id.create(2, Link.class), node3, node4, 100, 5, 100, 1);
		network.createAndAddLink(Id.create(3, Link.class), node4, node5, 100, 5, 100, 1);
		network.createAndAddLink(Id.create(4, Link.class), node5, node6, 100, 5, 100, 1);
	}

	/** Creates a simple network with route alternatives in 2 places. */
	static void createNetwork2(final MutableScenario scenario) {
		/* This creates the following network:
		 *
		 *            3 /----(3)----\ 4
		 *             /             \
		 * (1)-------(2)--------------(4)-------(5)
		 *  |    2            5             6    |
		 *  |1                                   |
		 *  |                                    |
		 * (0)                                   |
		 *                                     7 |
		 * (11)                                  |
		 *  |                                    |
		 *  |13                                  |
		 *  |    12          11             8    |
		 * (10)------(9)--------------(7)-------(6)
		 *             \             /
		 *           10 \----(8)----/ 9
		 *
		 * each link is 100m long and can be traveled along with 18km/h = 5m/s = 20s for 100m
		 */
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		network.setCapacityPeriod(Time.parseTime("01:00:00"));
		Node node0 = network.createAndAddNode(Id.create( "0", Node.class), new Coord((double) 0, (double) 10));
		Node node1 = network.createAndAddNode(Id.create( "1", Node.class), new Coord((double) 0, (double) 100));
		Node node2 = network.createAndAddNode(Id.create( "2", Node.class), new Coord((double) 100, (double) 100));
		Node node3 = network.createAndAddNode(Id.create( "3", Node.class), new Coord((double) 150, (double) 150));
		Node node4 = network.createAndAddNode(Id.create( "4", Node.class), new Coord((double) 200, (double) 100));
		Node node5 = network.createAndAddNode(Id.create( "5", Node.class), new Coord((double) 300, (double) 100));
		final double y5 = -100;
		Node node6 = network.createAndAddNode(Id.create( "6", Node.class), new Coord((double) 300, y5));
		final double y4 = -100;
		Node node7 = network.createAndAddNode(Id.create( "7", Node.class), new Coord((double) 200, y4));
		final double y3 = -150;
		Node node8 = network.createAndAddNode(Id.create( "8", Node.class), new Coord((double) 150, y3));
		final double y2 = -100;
		Node node9 = network.createAndAddNode(Id.create( "9", Node.class), new Coord((double) 100, y2));
		final double y1 = -100;
		Node node10 =network.createAndAddNode(Id.create("10", Node.class), new Coord((double) 0, y1));
		final double y = -10;
		Node node11 =network.createAndAddNode(Id.create("11", Node.class), new Coord((double) 0, y));
		network.createAndAddLink(Id.create( "1", Link.class),  node0,  node1, 100, 5, 100, 1);
		network.createAndAddLink(Id.create( "2", Link.class),  node1,  node2, 100, 5, 100, 1);
		network.createAndAddLink(Id.create( "3", Link.class),  node2,  node3, 100, 5, 100, 1);
		network.createAndAddLink(Id.create( "4", Link.class),  node3,  node4, 100, 5, 100, 1);
		network.createAndAddLink(Id.create( "5", Link.class),  node2,  node4, 100, 5, 100, 1);
		network.createAndAddLink(Id.create( "6", Link.class),  node4,  node5, 100, 5, 100, 1);
		network.createAndAddLink(Id.create( "7", Link.class),  node5,  node6, 100, 5, 100, 1);
		network.createAndAddLink(Id.create( "8", Link.class),  node6,  node7, 100, 5, 100, 1);
		network.createAndAddLink(Id.create( "9", Link.class),  node7,  node8, 100, 5, 100, 1);
		network.createAndAddLink(Id.create("10", Link.class),  node8,  node9, 100, 5, 100, 1);
		network.createAndAddLink(Id.create("11", Link.class),  node7,  node9, 100, 5, 100, 1);
		network.createAndAddLink(Id.create("12", Link.class),  node9, node10, 100, 5, 100, 1);
		network.createAndAddLink(Id.create("13", Link.class), node10, node11, 100, 5, 100, 1);
	}

	/**
	 * Creates a population for network1
	 **/
	static void createPopulation1(final MutableScenario scenario) {
		Population population = scenario.getPopulation();
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();

		Link link0 = network.getLinks().get(Id.create(0, Link.class));
		Link link1 = network.getLinks().get(Id.create(1, Link.class));
		Link link2 = network.getLinks().get(Id.create(2, Link.class));
		Link link3 = network.getLinks().get(Id.create(3, Link.class));
		Link link4 = network.getLinks().get(Id.create(4, Link.class));
		Fixture.addPersonToPopulation(Fixture.createPerson1( 1, "07:00"   , link0.getId(), NetworkUtils.getLinkIds("1 2 3"), link4.getId()), population); // toll in 1st time slot
		Fixture.addPersonToPopulation(Fixture.createPerson1( 2, "11:00"   , link0.getId(), NetworkUtils.getLinkIds("1 2 3"), link4.getId()), population); // toll in 2nd time slot
		Fixture.addPersonToPopulation(Fixture.createPerson1( 3, "16:00"   , link0.getId(), NetworkUtils.getLinkIds("1 2 3"), link4.getId()), population); // toll in 3rd time slot
		Fixture.addPersonToPopulation(Fixture.createPerson1( 4, "09:59:50", link0.getId(), NetworkUtils.getLinkIds("1 2 3"), link4.getId()), population); // toll in 1st and 2nd time slot
		Fixture.addPersonToPopulation(Fixture.createPerson1( 5, "08:00:00", link1.getId(), NetworkUtils.getLinkIds("2 3"), link4.getId()), population); // starts on the 2nd link
		Fixture.addPersonToPopulation(Fixture.createPerson1( 6, "09:00:00", link0.getId(), NetworkUtils.getLinkIds("1 2"), link3.getId()), population); // ends not on the last link
		Fixture.addPersonToPopulation(Fixture.createPerson1( 7, "08:30:00", link1.getId(), NetworkUtils.getLinkIds("2"), link3.getId()), population); // starts and ends not on the first/last link
		Fixture.addPersonToPopulation(Fixture.createPerson1( 8, "08:35:00", link1.getId(), NetworkUtils.getLinkIds(""), link2.getId()), population); // starts and ends not on the first/last link
		Fixture.addPersonToPopulation(Fixture.createPerson1( 9, "08:40:00", link1.getId(), NetworkUtils.getLinkIds(""), link1.getId()), population); // two acts on the same link
		Fixture.addPersonToPopulation(Fixture.createPerson1(10, "08:45:00", link2.getId(), NetworkUtils.getLinkIds(""), link3.getId()), population);
	}

	private static void addPersonToPopulation(final Person person, final Population population) {
		population.addPerson(person);
	}

	/**
	 * Creates a population for network2
	 **/
	static void createPopulation2(final MutableScenario scenario) {
		Population population = scenario.getPopulation();
		Network network = scenario.getNetwork();

		Fixture.addPersonToPopulation(Fixture.createPerson2(1, "07:00", network.getLinks().get(Id.create("1", Link.class)), network.getLinks().get(Id.create("7", Link.class)), network.getLinks().get(Id.create("13", Link.class))), population);
	}

	private static Person createPerson1(final int personId, final String startTime, final Id homeLinkId, final List<Id<Link>> routeLinkIds, final Id workLinkId) {
		Person person = PopulationUtils.createPerson(Id.create(personId, Person.class));
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		person.addPlan(plan);
		plan.createAndAddActivity("h", homeLinkId).setEndTime(Time.parseTime(startTime));
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		NetworkRoute route = new LinkNetworkRouteImpl(homeLinkId, workLinkId);
		route.setLinkIds(homeLinkId, routeLinkIds, workLinkId);
		leg.setRoute(route);
		plan.createAndAddActivity("w", workLinkId);
		return person;
	}

	private static Person createPerson2(final int personId, final String startTime, final Link homeLink, final Link workLink, final Link finishLink) {
		Person person = PopulationUtils.createPerson(Id.create(personId, Person.class));
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		person.addPlan(plan);
		ActivityImpl act = plan.createAndAddActivity("h", homeLink.getId());
		act.setCoord(homeLink.getCoord());
		act.setEndTime(Time.parseTime(startTime));
		plan.createAndAddLeg(TransportMode.car);
		act = plan.createAndAddActivity("w", workLink.getId());
		act.setCoord(workLink.getCoord());
		act.setEndTime(16.0 * 3600);
		plan.createAndAddLeg(TransportMode.car);
		act = plan.createAndAddActivity("h", finishLink.getId());
		act.setCoord(finishLink.getCoord());
		return person;
	}

	protected static Population createReferencePopulation1(final Config config) {
		// run mobsim once without toll and get score for network1/population1
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);
		Fixture.createNetwork1(scenario);
		Fixture.createPopulation1(scenario);
		Population referencePopulation = scenario.getPopulation();
		EventsManager events = EventsUtils.createEventsManager();
		EventsToScore scoring = EventsToScore.createWithScoreUpdating(scenario, new CharyparNagelScoringFunctionFactory(scenario), events);
		Mobsim sim = QSimUtils.createDefaultQSim(scenario, events);
		sim.run();
		scoring.finish();

		return referencePopulation;
	}

	protected static void compareRoutes(final String expectedRoute, final NetworkRoute realRoute) {
		TestCase.assertNotNull(expectedRoute) ;
		TestCase.assertNotNull(realRoute);
		TestCase.assertNotNull(realRoute.getLinkIds()) ;

		StringBuilder strBuilder = new StringBuilder();
		
		for (Id linkId : realRoute.getLinkIds()) {
			strBuilder.append(linkId.toString());
			strBuilder.append(' ');
		}
		String route = strBuilder.toString();
		TestCase.assertEquals(expectedRoute + " ", route);
	}
}
