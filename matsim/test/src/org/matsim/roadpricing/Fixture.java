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

import junit.framework.TestCase;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.Route;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.world.World;

/**
 * Some static methods to set up the road pricing scenarios in the test cases.
 *
 * @author mrieser
 */
public class Fixture {

	private Fixture() {
		// static class
	}

	/** @return a simple network consisting of 5 equal links in a row. */
	public static NetworkLayer createNetwork1() {
		/* This creates the following network:
		 *
		 * (1)-------(2)-------(3)-------(4)-------(5)-------(6)
		 *       0         1         2         3         4
		 */
		/* The vehicles can travel with 18km/h = 5m/s, so it should take them 20 seconds
		 * to travel along one link.		 */
		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod("01:00:00");
		Node node1 = network.createNode(new IdImpl(1), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl(2), new CoordImpl(100, 0));
		Node node3 = network.createNode(new IdImpl(3), new CoordImpl(200, 0));
		Node node4 = network.createNode(new IdImpl(4), new CoordImpl(300, 0));
		Node node5 = network.createNode(new IdImpl(5), new CoordImpl(400, 0));
		Node node6 = network.createNode(new IdImpl(6), new CoordImpl(500, 0));
		// freespeed 18km/h = 5m/s --> 20s for 100m
		network.createLink(new IdImpl(0), node1, node2, 100, 5, 100, 1);
		network.createLink(new IdImpl(1), node2, node3, 100, 5, 100, 1);
		network.createLink(new IdImpl(2), node3, node4, 100, 5, 100, 1);
		network.createLink(new IdImpl(3), node4, node5, 100, 5, 100, 1);
		network.createLink(new IdImpl(4), node5, node6, 100, 5, 100, 1);
		return network;
	}

	/** @return a simple network with route alternatives in 2 places. */
	public static NetworkLayer createNetwork2() {
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
		network.setCapacityPeriod("01:00:00");
		network.createNode( "0",   "0",   "10", null);
		network.createNode( "1",   "0",  "100", null);
		network.createNode( "2", "100",  "100", null);
		network.createNode( "3", "150",  "150", null);
		network.createNode( "4", "200",  "100", null);
		network.createNode( "5", "300",  "100", null);
		network.createNode( "6", "300", "-100", null);
		network.createNode( "7", "200", "-100", null);
		network.createNode( "8", "150", "-150", null);
		network.createNode( "9", "100", "-100", null);
		network.createNode("10",   "0", "-100", null);
		network.createNode("11",   "0",  "-10", null);
		network.createLink( "1",  "0",  "1", "100", "5", "100", "1", null, null);
		network.createLink( "2",  "1",  "2", "100", "5", "100", "1", null, null);
		network.createLink( "3",  "2",  "3", "100", "5", "100", "1", null, null);
		network.createLink( "4",  "3",  "4", "100", "5", "100", "1", null, null);
		network.createLink( "5",  "2",  "4", "100", "5", "100", "1", null, null);
		network.createLink( "6",  "4",  "5", "100", "5", "100", "1", null, null);
		network.createLink( "7",  "5",  "6", "100", "5", "100", "1", null, null);
		network.createLink( "8",  "6",  "7", "100", "5", "100", "1", null, null);
		network.createLink( "9",  "7",  "8", "100", "5", "100", "1", null, null);
		network.createLink("10",  "8",  "9", "100", "5", "100", "1", null, null);
		network.createLink("11",  "7",  "9", "100", "5", "100", "1", null, null);
		network.createLink("12",  "9", "10", "100", "5", "100", "1", null, null);
		network.createLink("13", "10", "11", "100", "5", "100", "1", null, null);
		return network;
	}

	/** @return a population for network1 */
	public static Population createPopulation1() throws Exception {
		Population population = new Population(Population.NO_STREAMING);

		population.addPerson(Fixture.createPerson1( 1, "07:00"   , "0", "2 3 4 5", "4")); // toll in 1st time slot
		population.addPerson(Fixture.createPerson1( 2, "11:00"   , "0", "2 3 4 5", "4")); // toll in 2nd time slot
		population.addPerson(Fixture.createPerson1( 3, "16:00"   , "0", "2 3 4 5", "4")); // toll in 3rd time slot
		population.addPerson(Fixture.createPerson1( 4, "09:59:50", "0", "2 3 4 5", "4")); // toll in 1st and 2nd time slot
		population.addPerson(Fixture.createPerson1( 5, "08:00:00", "1", "3 4 5", "4")); // starts on the 2nd link
		population.addPerson(Fixture.createPerson1( 6, "09:00:00", "0", "2 3 4", "3")); // ends not on the last link
		population.addPerson(Fixture.createPerson1( 7, "08:30:00", "1", "3 4", "3")); // starts and ends not on the first/last link
		population.addPerson(Fixture.createPerson1( 8, "08:35:00", "1", "3", "2")); // starts and ends not on the first/last link
		population.addPerson(Fixture.createPerson1( 9, "08:40:00", "1", "", "1")); // two acts on the same link
		population.addPerson(Fixture.createPerson1(10, "08:45:00", "2", "4", "3"));

		return population;
	}

	/** @return a population for network2 */
	public static Population createPopulation2() throws Exception {
		Population population = new Population(Population.NO_STREAMING);

		population.addPerson(Fixture.createPerson2( 1, "07:00", "1", "7", "13"));

		return population;
	}

	private static Person createPerson1(final int personId, final String startTime, final String homeLink, final String routeNodes, final String workLeg) throws Exception {
		Person person = new Person(new IdImpl(personId));
		Plan plan = new Plan(person);
		person.addPlan(plan);
		plan.createAct("h", (String)null, null, homeLink, "00:00", startTime, startTime, "no");
		Leg leg = plan.createLeg("car", startTime, "00:01", null);
		Route route = new Route();
		route.setRoute(routeNodes);
		leg.setRoute(route);
		plan.createAct("w", (String)null, null, workLeg, null, "24:00", null, "yes");
		return person;
	}

	private static Person createPerson2(final int personId, final String startTime, final String homeLink, final String workLink, final String finishLink) throws Exception {
		Person person = new Person(new IdImpl(personId));
		Plan plan = new Plan(person);
		person.addPlan(plan);
		plan.createAct("h", (String)null, null, homeLink, "00:00", startTime, startTime, "no");
		plan.createLeg("car", startTime, "00:01", null);
		plan.createAct("w", (String)null, null, workLink, null, "16:00", "08:00", "no");
		plan.createLeg("car", "16:00", null, null);
		plan.createAct("h", (String)null, null, finishLink, null, "24:00", "00:00", "no");
		return person;
	}

	public static Population createReferencePopulation1(final World world) {
		// run mobsim once without toll and get score for network1/population1
		try {
			NetworkLayer network = createNetwork1();
			world.setNetworkLayer(network);
			Population referencePopulation = Fixture.createPopulation1();
			Events events = new Events();
			EventsToScore scoring = new EventsToScore(referencePopulation, new CharyparNagelScoringFunctionFactory());
			events.addHandler(scoring);
			QueueSimulation sim = new QueueSimulation(network, referencePopulation, events);
			sim.run();
			scoring.finish();

			return referencePopulation;
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void compareRoutes(final String expectedRoute, final Route realRoute) {
		StringBuilder strBuilder = new StringBuilder();
		for (Node node : realRoute.getRoute()) {
			strBuilder.append(node.getId().toString());
			strBuilder.append(' ');
		}
		String route = strBuilder.toString();
		TestCase.assertEquals(expectedRoute + " ", route);
	}
}
