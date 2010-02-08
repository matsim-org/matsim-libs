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

import java.util.List;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;

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
	protected static void createNetwork1(final ScenarioImpl scenario) {
		/* This creates the following network:
		 *
		 * (1)-------(2)-------(3)-------(4)-------(5)-------(6)
		 *       0         1         2         3         4
		 */
		/* The vehicles can travel with 18km/h = 5m/s, so it should take them 20 seconds
		 * to travel along one link.		 */
		NetworkLayer network = scenario.getNetwork();
		network.setCapacityPeriod(Time.parseTime("01:00:00"));
		Node node1 = network.createAndAddNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl(2), new CoordImpl(100, 0));
		Node node3 = network.createAndAddNode(new IdImpl(3), new CoordImpl(200, 0));
		Node node4 = network.createAndAddNode(new IdImpl(4), new CoordImpl(300, 0));
		Node node5 = network.createAndAddNode(new IdImpl(5), new CoordImpl(400, 0));
		Node node6 = network.createAndAddNode(new IdImpl(6), new CoordImpl(500, 0));
		// freespeed 18km/h = 5m/s --> 20s for 100m
		network.createAndAddLink(new IdImpl(0), node1, node2, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl(1), node2, node3, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl(2), node3, node4, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl(3), node4, node5, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl(4), node5, node6, 100, 5, 100, 1);
	}

	/** Creates a simple network with route alternatives in 2 places. */
	protected static void createNetwork2(final ScenarioImpl scenario) {
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
		NetworkLayer network = scenario.getNetwork();
		network.setCapacityPeriod(Time.parseTime("01:00:00"));
		Node node0 = network.createAndAddNode(new IdImpl( "0"), new CoordImpl(  0,   10));
		Node node1 = network.createAndAddNode(new IdImpl( "1"), new CoordImpl(  0,  100));
		Node node2 = network.createAndAddNode(new IdImpl( "2"), new CoordImpl(100,  100));
		Node node3 = network.createAndAddNode(new IdImpl( "3"), new CoordImpl(150,  150));
		Node node4 = network.createAndAddNode(new IdImpl( "4"), new CoordImpl(200,  100));
		Node node5 = network.createAndAddNode(new IdImpl( "5"), new CoordImpl(300,  100));
		Node node6 = network.createAndAddNode(new IdImpl( "6"), new CoordImpl(300, -100));
		Node node7 = network.createAndAddNode(new IdImpl( "7"), new CoordImpl(200, -100));
		Node node8 = network.createAndAddNode(new IdImpl( "8"), new CoordImpl(150, -150));
		Node node9 = network.createAndAddNode(new IdImpl( "9"), new CoordImpl(100, -100));
		Node node10 =network.createAndAddNode(new IdImpl("10"), new CoordImpl(  0, -100));
		Node node11 =network.createAndAddNode(new IdImpl("11"), new CoordImpl(  0,  -10));
		network.createAndAddLink(new IdImpl( "1"),  node0,  node1, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl( "2"),  node1,  node2, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl( "3"),  node2,  node3, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl( "4"),  node3,  node4, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl( "5"),  node2,  node4, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl( "6"),  node4,  node5, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl( "7"),  node5,  node6, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl( "8"),  node6,  node7, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl( "9"),  node7,  node8, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl("10"),  node8,  node9, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl("11"),  node7,  node9, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl("12"),  node9, node10, 100, 5, 100, 1);
		network.createAndAddLink(new IdImpl("13"), node10, node11, 100, 5, 100, 1);
	}

	/**
	 * Creates a population for network1
	 * @param network the network returned by {@link #createNetwork1()}
	 **/
	protected static void createPopulation1(final ScenarioImpl scenario) {
		PopulationImpl population = scenario.getPopulation();
		NetworkLayer network = scenario.getNetwork();

		LinkImpl link0 = network.getLinks().get(new IdImpl(0));
		LinkImpl link1 = network.getLinks().get(new IdImpl(1));
		LinkImpl link2 = network.getLinks().get(new IdImpl(2));
		LinkImpl link3 = network.getLinks().get(new IdImpl(3));
		LinkImpl link4 = network.getLinks().get(new IdImpl(4));
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

	private static void addPersonToPopulation(final PersonImpl person, final PopulationImpl population) {
		population.addPerson(person);
	}

	/**
	 * Creates a population for network2
	 * @param network the network returned by {@link #createNetwork2()}
	 **/
	protected static void createPopulation2(final ScenarioImpl scenario) {
		PopulationImpl population = scenario.getPopulation();
		NetworkLayer network = scenario.getNetwork();

		Fixture.addPersonToPopulation(Fixture.createPerson2(1, "07:00", network.getLinks().get(new IdImpl("1")), network.getLinks().get(new IdImpl("7")), network.getLinks().get(new IdImpl("13"))), population);
	}

	private static PersonImpl createPerson1(final int personId, final String startTime, final Id homeLinkId, final List<Id> routeLinkIds, final Id workLinkId) {
		PersonImpl person = new PersonImpl(new IdImpl(personId));
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		person.addPlan(plan);
		plan.createAndAddActivity("h", homeLinkId).setEndTime(Time.parseTime(startTime));
		LegImpl leg = plan.createAndAddLeg(TransportMode.car);
		NetworkRouteWRefs route = new LinkNetworkRouteImpl(homeLinkId, workLinkId, null);
		route.setLinkIds(homeLinkId, routeLinkIds, workLinkId);
		leg.setRoute(route);
		plan.createAndAddActivity("w", workLinkId);
		return person;
	}

	private static PersonImpl createPerson2(final int personId, final String startTime, final LinkImpl homeLink, final LinkImpl workLink, final LinkImpl finishLink) {
		PersonImpl person = new PersonImpl(new IdImpl(personId));
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

	protected static PopulationImpl createReferencePopulation1(final CharyparNagelScoringConfigGroup config) {
		// run mobsim once without toll and get score for network1/population1
		ScenarioImpl scenario = new ScenarioImpl();
		Fixture.createNetwork1(scenario);
		Fixture.createPopulation1(scenario);
		PopulationImpl referencePopulation = scenario.getPopulation();
		EventsManagerImpl events = new EventsManagerImpl();
		EventsToScore scoring = new EventsToScore(referencePopulation, new CharyparNagelScoringFunctionFactory(config));
		events.addHandler(scoring);
		QueueSimulation sim = new QueueSimulation(scenario, events);
		sim.run();
		scoring.finish();

		return referencePopulation;
	}

	protected static void compareRoutes(final String expectedRoute, final NetworkRouteWRefs realRoute) {
		StringBuilder strBuilder = new StringBuilder();
		for (Id linkId : realRoute.getLinkIds()) {
			strBuilder.append(linkId.toString());
			strBuilder.append(' ');
		}
		String route = strBuilder.toString();
		TestCase.assertEquals(expectedRoute + " ", route);
	}
}
