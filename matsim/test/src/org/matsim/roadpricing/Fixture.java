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

import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.network.Link;
import org.matsim.core.api.experimental.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.events.Events;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.NodeNetworkRoute;
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

	/** @return a simple network consisting of 5 equal links in a row. */
	protected static NetworkLayer createNetwork1() {
		/* This creates the following network:
		 *
		 * (1)-------(2)-------(3)-------(4)-------(5)-------(6)
		 *       0         1         2         3         4
		 */
		/* The vehicles can travel with 18km/h = 5m/s, so it should take them 20 seconds
		 * to travel along one link.		 */
		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod(Time.parseTime("01:00:00"));
		NodeImpl node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		NodeImpl node2 = network.createNode(new IdImpl(2), new CoordImpl(100, 0));
		NodeImpl node3 = network.createNode(new IdImpl(3), new CoordImpl(200, 0));
		NodeImpl node4 = network.createNode(new IdImpl(4), new CoordImpl(300, 0));
		NodeImpl node5 = network.createNode(new IdImpl(5), new CoordImpl(400, 0));
		NodeImpl node6 = network.createNode(new IdImpl(6), new CoordImpl(500, 0));
		// freespeed 18km/h = 5m/s --> 20s for 100m
		network.createLink(new IdImpl(0), node1, node2, 100, 5, 100, 1);
		network.createLink(new IdImpl(1), node2, node3, 100, 5, 100, 1);
		network.createLink(new IdImpl(2), node3, node4, 100, 5, 100, 1);
		network.createLink(new IdImpl(3), node4, node5, 100, 5, 100, 1);
		network.createLink(new IdImpl(4), node5, node6, 100, 5, 100, 1);
		return network;
	}

	/** @return a simple network with route alternatives in 2 places. */
	protected static NetworkLayer createNetwork2() {
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
		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod(Time.parseTime("01:00:00"));
		NodeImpl node0 = network.createNode(new IdImpl( "0"), new CoordImpl(  0,   10));
		NodeImpl node1 = network.createNode(new IdImpl( "1"), new CoordImpl(  0,  100));
		NodeImpl node2 = network.createNode(new IdImpl( "2"), new CoordImpl(100,  100));
		NodeImpl node3 = network.createNode(new IdImpl( "3"), new CoordImpl(150,  150));
		NodeImpl node4 = network.createNode(new IdImpl( "4"), new CoordImpl(200,  100));
		NodeImpl node5 = network.createNode(new IdImpl( "5"), new CoordImpl(300,  100));
		NodeImpl node6 = network.createNode(new IdImpl( "6"), new CoordImpl(300, -100));
		NodeImpl node7 = network.createNode(new IdImpl( "7"), new CoordImpl(200, -100));
		NodeImpl node8 = network.createNode(new IdImpl( "8"), new CoordImpl(150, -150));
		NodeImpl node9 = network.createNode(new IdImpl( "9"), new CoordImpl(100, -100));
		NodeImpl node10 =network.createNode(new IdImpl("10"), new CoordImpl(  0, -100));
		NodeImpl node11 =network.createNode(new IdImpl("11"), new CoordImpl(  0,  -10));
		network.createLink(new IdImpl( "1"),  node0,  node1, 100, 5, 100, 1);
		network.createLink(new IdImpl( "2"),  node1,  node2, 100, 5, 100, 1);
		network.createLink(new IdImpl( "3"),  node2,  node3, 100, 5, 100, 1);
		network.createLink(new IdImpl( "4"),  node3,  node4, 100, 5, 100, 1);
		network.createLink(new IdImpl( "5"),  node2,  node4, 100, 5, 100, 1);
		network.createLink(new IdImpl( "6"),  node4,  node5, 100, 5, 100, 1);
		network.createLink(new IdImpl( "7"),  node5,  node6, 100, 5, 100, 1);
		network.createLink(new IdImpl( "8"),  node6,  node7, 100, 5, 100, 1);
		network.createLink(new IdImpl( "9"),  node7,  node8, 100, 5, 100, 1);
		network.createLink(new IdImpl("10"),  node8,  node9, 100, 5, 100, 1);
		network.createLink(new IdImpl("11"),  node7,  node9, 100, 5, 100, 1);
		network.createLink(new IdImpl("12"),  node9, node10, 100, 5, 100, 1);
		network.createLink(new IdImpl("13"), node10, node11, 100, 5, 100, 1);
		return network;
	}

	/**
	 * @param network the network returned by {@link #createNetwork1()}
	 * @return a population for network1
	 **/
	protected static PopulationImpl createPopulation1(final NetworkLayer network) {
		PopulationImpl population = new PopulationImpl();

		LinkImpl link0 = network.getLink(new IdImpl(0));
		LinkImpl link1 = network.getLink(new IdImpl(1));
		LinkImpl link2 = network.getLink(new IdImpl(2));
		LinkImpl link3 = network.getLink(new IdImpl(3));
		LinkImpl link4 = network.getLink(new IdImpl(4));
		Fixture.addPersonToPopulation(Fixture.createPerson1( 1, "07:00"   , link0, NetworkUtils.getNodes(network, "2 3 4 5"), link4), population); // toll in 1st time slot
		Fixture.addPersonToPopulation(Fixture.createPerson1( 2, "11:00"   , link0, NetworkUtils.getNodes(network, "2 3 4 5"), link4), population); // toll in 2nd time slot
		Fixture.addPersonToPopulation(Fixture.createPerson1( 3, "16:00"   , link0, NetworkUtils.getNodes(network, "2 3 4 5"), link4), population); // toll in 3rd time slot
		Fixture.addPersonToPopulation(Fixture.createPerson1( 4, "09:59:50", link0, NetworkUtils.getNodes(network, "2 3 4 5"), link4), population); // toll in 1st and 2nd time slot
		Fixture.addPersonToPopulation(Fixture.createPerson1( 5, "08:00:00", link1, NetworkUtils.getNodes(network, "3 4 5"), link4), population); // starts on the 2nd link
		Fixture.addPersonToPopulation(Fixture.createPerson1( 6, "09:00:00", link0, NetworkUtils.getNodes(network, "2 3 4"), link3), population); // ends not on the last link
		Fixture.addPersonToPopulation(Fixture.createPerson1( 7, "08:30:00", link1, NetworkUtils.getNodes(network, "3 4"), link3), population); // starts and ends not on the first/last link
		Fixture.addPersonToPopulation(Fixture.createPerson1( 8, "08:35:00", link1, NetworkUtils.getNodes(network, "3"), link2), population); // starts and ends not on the first/last link
		Fixture.addPersonToPopulation(Fixture.createPerson1( 9, "08:40:00", link1, NetworkUtils.getNodes(network, ""), link1), population); // two acts on the same link
		Fixture.addPersonToPopulation(Fixture.createPerson1(10, "08:45:00", link2, NetworkUtils.getNodes(network, "4"), link3), population);

		return population;
	}
	
	private static void addPersonToPopulation(final PersonImpl person, final PopulationImpl population) {
		population.getPersons().put(person.getId(), person);
	}

	/**
	 * @param network the network returned by {@link #createNetwork2()}
	 * @return a population for network2
	 **/
	protected static PopulationImpl createPopulation2(final NetworkLayer network) {
		PopulationImpl population = new PopulationImpl();

		Fixture.addPersonToPopulation(Fixture.createPerson2(1, "07:00", network.getLink("1"), network.getLink("7"), network.getLink("13")), population);

		return population;
	}

	private static PersonImpl createPerson1(final int personId, final String startTime, final Link homeLink, final List<Node> routeNodes, final Link workLink) {
		PersonImpl person = new PersonImpl(new IdImpl(personId));
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		person.addPlan(plan);
		plan.createActivity("h", (LinkImpl)homeLink).setEndTime(Time.parseTime(startTime));
		LegImpl leg = plan.createLeg(TransportMode.car);//"car", startTime, "00:01", null);
		NetworkRoute route = new NodeNetworkRoute(homeLink, workLink);
		route.setNodes(homeLink, routeNodes, workLink);
		leg.setRoute(route);
		plan.createActivity("w", (LinkImpl)workLink);//, null, "24:00", null, "yes");
		return person;
	}

	private static PersonImpl createPerson2(final int personId, final String startTime, final LinkImpl homeLink, final LinkImpl workLink, final LinkImpl finishLink) {
		PersonImpl person = new PersonImpl(new IdImpl(personId));
		PlanImpl plan = new org.matsim.core.population.PlanImpl(person);
		person.addPlan(plan);
		ActivityImpl act = plan.createActivity("h", homeLink);
		act.setCoord(homeLink.getCoord());
		act.setEndTime(Time.parseTime(startTime));
		plan.createLeg(TransportMode.car);
		act = plan.createActivity("w", workLink);
		act.setCoord(workLink.getCoord());
		act.setEndTime(16.0 * 3600);
		plan.createLeg(TransportMode.car);
		act = plan.createActivity("h", finishLink);
		act.setCoord(finishLink.getCoord());
		return person;
	}

	protected static PopulationImpl createReferencePopulation1(final CharyparNagelScoringConfigGroup config) {
		// run mobsim once without toll and get score for network1/population1
		NetworkLayer network = createNetwork1();
		PopulationImpl referencePopulation = Fixture.createPopulation1(network);
		Events events = new Events();
		EventsToScore scoring = new EventsToScore(referencePopulation, new CharyparNagelScoringFunctionFactory(config));
		events.addHandler(scoring);
		QueueSimulation sim = new QueueSimulation(network, referencePopulation, events);
		sim.run();
		scoring.finish();

		return referencePopulation;
	}

	protected static void compareRoutes(final String expectedRoute, final NetworkRoute realRoute) {
		StringBuilder strBuilder = new StringBuilder();
		for (Node node : realRoute.getNodes()) {
			strBuilder.append(node.getId().toString());
			strBuilder.append(' ');
		}
		String route = strBuilder.toString();
		TestCase.assertEquals(expectedRoute + " ", route);
	}
}
