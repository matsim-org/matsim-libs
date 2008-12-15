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
import org.matsim.basic.v01.BasicLeg.Mode;
import org.matsim.events.Events;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.EventsToScore;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;
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
		Node node0 = network.createNode( "0",   "0",   "10", null);
		Node node1 = network.createNode( "1",   "0",  "100", null);
		Node node2 = network.createNode( "2", "100",  "100", null);
		Node node3 = network.createNode( "3", "150",  "150", null);
		Node node4 = network.createNode( "4", "200",  "100", null);
		Node node5 = network.createNode( "5", "300",  "100", null);
		Node node6 = network.createNode( "6", "300", "-100", null);
		Node node7 = network.createNode( "7", "200", "-100", null);
		Node node8 = network.createNode( "8", "150", "-150", null);
		Node node9 = network.createNode( "9", "100", "-100", null);
		Node node10 = network.createNode("10",   "0", "-100", null);
		Node node11 = network.createNode("11",   "0",  "-10", null);
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
	public static Population createPopulation1(final NetworkLayer network) {
		Population population = new Population(Population.NO_STREAMING);

		Link link0 = network.getLink(new IdImpl(0));
		Link link1 = network.getLink(new IdImpl(1));
		Link link2 = network.getLink(new IdImpl(2));
		Link link3 = network.getLink(new IdImpl(3));
		Link link4 = network.getLink(new IdImpl(4));
		population.addPerson(Fixture.createPerson1( 1, "07:00"   , link0, "2 3 4 5", link4)); // toll in 1st time slot
		population.addPerson(Fixture.createPerson1( 2, "11:00"   , link0, "2 3 4 5", link4)); // toll in 2nd time slot
		population.addPerson(Fixture.createPerson1( 3, "16:00"   , link0, "2 3 4 5", link4)); // toll in 3rd time slot
		population.addPerson(Fixture.createPerson1( 4, "09:59:50", link0, "2 3 4 5", link4)); // toll in 1st and 2nd time slot
		population.addPerson(Fixture.createPerson1( 5, "08:00:00", link1, "3 4 5", link4)); // starts on the 2nd link
		population.addPerson(Fixture.createPerson1( 6, "09:00:00", link0, "2 3 4", link3)); // ends not on the last link
		population.addPerson(Fixture.createPerson1( 7, "08:30:00", link1, "3 4", link3)); // starts and ends not on the first/last link
		population.addPerson(Fixture.createPerson1( 8, "08:35:00", link1, "3", link2)); // starts and ends not on the first/last link
		population.addPerson(Fixture.createPerson1( 9, "08:40:00", link1, "", link1)); // two acts on the same link
		population.addPerson(Fixture.createPerson1(10, "08:45:00", link2, "4", link3));

		return population;
	}

	/**
	 * @param network the network returned by {@link #createNetwork2()} 
	 * @return a population for network2
	 **/
	public static Population createPopulation2(final NetworkLayer network) {
		Population population = new Population(Population.NO_STREAMING);

		population.addPerson(Fixture.createPerson2(1, "07:00", network.getLink("1"), network.getLink("7"), network.getLink("13")));

		return population;
	}

	private static Person createPerson1(final int personId, final String startTime, final Link homeLink, final String routeNodes, final Link workLink) {
		Person person = new PersonImpl(new IdImpl(personId));
		Plan plan = new Plan(person);
		person.addPlan(plan);
		plan.createAct("h", homeLink).setEndTime(Time.parseTime(startTime));
		Leg leg = plan.createLeg(Mode.car);//"car", startTime, "00:01", null);
		CarRoute route = new NodeCarRoute();
		route.setNodes(routeNodes);
		leg.setRoute(route);
		plan.createAct("w", workLink);//, null, "24:00", null, "yes");
		return person;
	}

	private static Person createPerson2(final int personId, final String startTime, final Link homeLink, final Link workLink, final Link finishLink) {
		Person person = new PersonImpl(new IdImpl(personId));
		Plan plan = new Plan(person);
		person.addPlan(plan);
		plan.createAct("h", homeLink).setEndTime(Time.parseTime(startTime));//, "00:00", startTime, startTime, "no");
		plan.createLeg(Mode.car);//"car", startTime, "00:01", null);
		plan.createAct("w", workLink).setDuration(8.0 * 3600);//, null, "16:00", "08:00", "no");
		plan.createLeg(Mode.car);//"car", "16:00", null, null);
		plan.createAct("h", finishLink);//, null, "24:00", "00:00", "no");
		return person;
	}

	public static Population createReferencePopulation1(final World world) {
		// run mobsim once without toll and get score for network1/population1
		NetworkLayer network = createNetwork1();
		world.setNetworkLayer(network);
		world.complete();
		Population referencePopulation = Fixture.createPopulation1(network);
		Events events = new Events();
		EventsToScore scoring = new EventsToScore(referencePopulation, new CharyparNagelScoringFunctionFactory());
		events.addHandler(scoring);
		QueueSimulation sim = new QueueSimulation(network, referencePopulation, events);
		sim.run();
		scoring.finish();

		return referencePopulation;
	}

	public static void compareRoutes(final String expectedRoute, final CarRoute realRoute) {
		StringBuilder strBuilder = new StringBuilder();
		for (Node node : realRoute.getNodes()) {
			strBuilder.append(node.getId().toString());
			strBuilder.append(' ');
		}
		String route = strBuilder.toString();
		TestCase.assertEquals(expectedRoute + " ", route);
	}
}
