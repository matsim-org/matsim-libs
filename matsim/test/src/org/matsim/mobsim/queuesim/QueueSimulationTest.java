/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.mobsim.queuesim;

import java.util.ArrayList;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.BasicLeg;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.routes.CarRoute;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.NetworkUtils;
import org.matsim.utils.geometry.CoordImpl;

public class QueueSimulationTest extends MatsimTestCase {

	/**
	 * Tests that the flow capacity can be reached (but not exceeded) by
	 * agents driving over a link.
	 *
	 * @author mrieser
	 */
	public void testFlowCapacityDriving() {
		Config config = Gbl.createConfig(null);
		config.simulation().setFlowCapFactor(1.0);
		config.simulation().setStorageCapFactor(1.0);

		/* build network */
		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod("1:00:00");
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(1100, 0));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(1200, 0));
		Link link1 = network.createLink(new IdImpl("1"), node1, node2, 100, 10, 60000, 9);
		/* ------ */ network.createLink(new IdImpl("2"), node2, node3, 1000, 10, 6000, 2);
		Link link3 = network.createLink(new IdImpl("3"), node3, node4, 100, 10, 60000, 9);

		/* build plans */
		Population plans = new Population(Population.NO_STREAMING);

		ArrayList<Node> nodes23 = new ArrayList<Node>();
		nodes23.add(node2);
		nodes23.add(node3);

		// add a first person with leg from link1 to link3, let it start early, so the simulation can accumulate buffer capacity
		Person person = new PersonImpl(new IdImpl(0));
		Plan plan = person.createPlan(true);
		Act a1 = plan.createAct("h", link1);
		a1.setEndTime(6*3600 - 500);
		Leg leg = plan.createLeg(BasicLeg.Mode.car);
		CarRoute route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link1, link3);
		route.setNodes(link1, nodes23, link3);
		leg.setRoute(route);
		plan.createAct("w", link3);
		plans.addPerson(person);

		// add a lot of other persons with legs from link1 to link3, starting at 6:30
		for (int i = 1; i <= 10000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
			/* exact dep. time: 6:28:18. The agents needs:
			 * - at the specified time, the agent goes into the waiting list, and if space is available, into
			 * the buffer of link 1.
			 * - 1 sec later, it leaves the buffer on link 1 and enters link 2
			 * - the agent takes 100 sec. to travel along link 2, after which it gets placed in the buffer of link 2
			 * - 1 sec later, the agent leaves the buffer on link 2 (if flow-cap allows this) and enters link 3
			 * - as we measure the vehicles leaving link 2, and the first veh should leave at exactly 6:30, it has
			 * to start 1 + 100 + 1 = 102 secs earlier.
			 * So, the start time is 7*3600 - 1800 - 102 = 7*3600 - 1902
			 */
			Act a = plan.createAct("h", link1);
			a.setEndTime(7*3600 - 1902);
			leg = plan.createLeg(BasicLeg.Mode.car);
			route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link1, link3);
			route.setNodes(link1, nodes23, link3);
			leg.setRoute(route);
			plan.createAct("w", link3);
			plans.addPerson(person);
		}

		/* build events */
		Events events = new Events();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(network, plans, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink("2");
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));

		assertEquals(3000, volume[6]); // we should have half of the maximum flow in this hour
		assertEquals(6000, volume[7]); // we should have maximum flow in this hour
		assertEquals(1000, volume[8]); // all the rest
	}

	/**
	 * Tests that the flow capacity can be reached (but not exceeded) by
	 * agents starting on a link. Due to the different handling of these
	 * agents and their direct placing in the Buffer, it makes sense to
	 * test this specifically.
	 *
	 * @author mrieser
	 */
	public void testFlowCapacityStarting() {
		Config config = Gbl.createConfig(null);
		config.simulation().setFlowCapFactor(1.0);
		config.simulation().setStorageCapFactor(1.0);

		/* build network */
		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod("1:00:00");
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(1100, 0));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(1200, 0));
		Link link1 = network.createLink(new IdImpl("1"), node1, node2, 100, 10, 60000, 9);
		Link link2 = network.createLink(new IdImpl("2"), node2, node3, 1000, 10, 6000, 2);
		Link link3 = network.createLink(new IdImpl("3"), node3, node4, 100, 10, 60000, 9);

		/* build plans */
		Population plans = new Population(Population.NO_STREAMING);

		ArrayList<Node> nodes3 = new ArrayList<Node>();
		nodes3.add(node3);

		ArrayList<Node> nodes23 = new ArrayList<Node>();
		nodes23.add(node2);
		nodes23.add(node3);

		// add a first person with leg from link1 to link3, let it start early, so the simulation can accumulate buffer capacity
		Person person = new PersonImpl(new IdImpl(0));
		Plan plan = person.createPlan(true);
		Act a1 = plan.createAct("h", link1);
		a1.setEndTime(6*3600 - 500);
		Leg leg = plan.createLeg(BasicLeg.Mode.car);
		CarRoute route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link1, link3);
		route.setNodes(link1, nodes23, link3);
		leg.setRoute(route);
		plan.createAct("w", link3);
		plans.addPerson(person);

		// add a lot of persons with legs from link2 to link3
		for (int i = 1; i <= 10000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
			Act a2 = plan.createAct("h", link2);
			a2.setEndTime(7*3600 - 1801);
			leg = plan.createLeg(BasicLeg.Mode.car);
			route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link2, link3);
			route.setNodes(link2, nodes3, link3);
			leg.setRoute(route);
			plan.createAct("w", link3);
			plans.addPerson(person);
		}

		/* build events */
		Events events = new Events();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(network, plans, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink("2");
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));

		assertEquals(3000, volume[6]); // we should have half of the maximum flow in this hour
		assertEquals(6000, volume[7]); // we should have maximum flow in this hour
		assertEquals(1000, volume[8]); // all the rest
	}

	/**
	 * Tests that the flow capacity of a link can be reached (but not exceeded) by
	 * agents starting on that link or driving through that link. This especially
	 * insures that the flow capacity measures both kinds (starting, driving) together.
	 *
	 * @author mrieser
	 */
	public void testFlowCapacityMixed() {
		Config config = Gbl.createConfig(null);
		config.simulation().setFlowCapFactor(1.0);
		config.simulation().setStorageCapFactor(1.0);

		/* build network */
		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod("1:00:00");
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(1100, 0));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(1200, 0));
		Link link1 = network.createLink(new IdImpl("1"), node1, node2, 100, 10, 60000, 9);
		Link link2 = network.createLink(new IdImpl("2"), node2, node3, 1000, 10, 6000, 2);
		Link link3 = network.createLink(new IdImpl("3"), node3, node4, 100, 10, 60000, 9);

		/* build plans */
		Population plans = new Population(Population.NO_STREAMING);
		
		ArrayList<Node> nodes3 = new ArrayList<Node>();
		nodes3.add(node3);

		ArrayList<Node> nodes23 = new ArrayList<Node>();
		nodes23.add(node2);
		nodes23.add(node3);

		// add a first person with leg from link1 to link3, let it start early, so the simulation can accumulate buffer capacity
		Person person = new PersonImpl(new IdImpl(0));
		Plan plan = person.createPlan(true);
		Act a1 = plan.createAct("h", link1);
		a1.setEndTime(6*3600 - 500);
		Leg leg = plan.createLeg(BasicLeg.Mode.car);
		CarRoute route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link1, link3);
		route.setNodes(link1, nodes23, link3);
		leg.setRoute(route);
		plan.createAct("w", link3);
		plans.addPerson(person);

		// add a lot of persons with legs from link2 to link3
		for (int i = 1; i <= 5000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
			Act a2 = plan.createAct("h", link2);
			a2.setEndTime(7*3600 - 1801);
			leg = plan.createLeg(BasicLeg.Mode.car);
			route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link2, link3);
			route.setNodes(link2, nodes3, link3);
			leg.setRoute(route);
			plan.createAct("w", link3);
			plans.addPerson(person);
		}
		// add a lot of persons with legs from link1 to link3
		for (int i = 5001; i <= 10000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
			Act a2 = plan.createAct("h", link1);
			a2.setEndTime(7*3600 - 1902);
			leg = plan.createLeg(BasicLeg.Mode.car);
			route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link2, link3);
			route.setNodes(link1, nodes23, link3);
			leg.setRoute(route);
			plan.createAct("w", link3);
			plans.addPerson(person);
		}

		/* build events */
		Events events = new Events();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(network, plans, events);
		sim.run();

		/* finish */
		int[] volume = vAnalyzer.getVolumesForLink("2");
		System.out.println("#vehicles 6-7: " + Integer.toString(volume[6]));
		System.out.println("#vehicles 7-8: " + Integer.toString(volume[7]));
		System.out.println("#vehicles 8-9: " + Integer.toString(volume[8]));

		assertEquals(3000, volume[6]); // we should have half of the maximum flow in this hour
		assertEquals(6000, volume[7]); // we should have maximum flow in this hour
		assertEquals(1000, volume[8]); // all the rest
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * does not lead to the destination link.
	 */
	public void testConsistentRoutes_WrongRoute() {
		new LogCounter();
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		QueueSimulation sim = prepareConsistentRoutesTest("2 3", events); // route should continue on 4, 5
		LogCounter logger = new LogCounter();
		Logger.getRootLogger().addAppender(logger);
		sim.run();
		Logger.getRootLogger().removeAppender(logger);
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * does not specify all nodes, so it is unclear at one node or another how to
	 * continue.
	 */
	public void testConsistentRoutes_ImpossibleRoute() {
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		QueueSimulation sim = prepareConsistentRoutesTest("2 3 5", events); // node 4 is missing
		LogCounter logger = new LogCounter();
		Logger.getRootLogger().addAppender(logger);
		sim.run();
		Logger.getRootLogger().removeAppender(logger);
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * is not specified, even that the destination link is different from the departure link.
	 */
	public void testConsistentRoutes_MissingRoute() {
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		QueueSimulation sim = prepareConsistentRoutesTest("", events); // no nodes at all
		LogCounter logger = new LogCounter();
		Logger.getRootLogger().addAppender(logger);
		sim.run();
		Logger.getRootLogger().removeAppender(logger);
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/** Prepares miscellaneous data for the testConsistentRoutes() tests:
	 * Creates a network of 6 links, and a population of one person driving from
	 * link 1 to link 5, and then from link 5 to link 6.
	 *
	 * @param nodes a list of node ids the agent should travel along for the first leg.
	 * @param events the Events object to be used by the simulation.
	 * @return A QueueSimulation which can be started immediately.
	 *
	 * @author mrieser
	 **/
	private QueueSimulation prepareConsistentRoutesTest(final String nodes, final Events events) {
		Config config = Gbl.createConfig(null);
		config.simulation().setFlowCapFactor(1.0);
		config.simulation().setStorageCapFactor(1.0);

		/* build network */
		NetworkLayer network = new NetworkLayer();
		network.setCapacityPeriod("1:00:00");
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(1100, 0));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(2100, 0));
		Node node5 = network.createNode(new IdImpl("5"), new CoordImpl(3100, 0));
		Node node6 = network.createNode(new IdImpl("6"), new CoordImpl(3200, 0));
		Node node7 = network.createNode(new IdImpl("7"), new CoordImpl(3300, 0));
		Link link1 = network.createLink(new IdImpl("1"), node1, node2, 100, 10, 60000, 9);
		network.createLink(new IdImpl("2"), node2, node3, 1000, 10, 6000, 2);
		network.createLink(new IdImpl("3"), node3, node4, 1000, 10, 6000, 2);
		network.createLink(new IdImpl("4"), node4, node5, 1000, 10, 6000, 2);
		Link link5 = network.createLink(new IdImpl("5"), node5, node6, 100, 10, 60000, 9);
		Link link6 = network.createLink(new IdImpl("6"), node6, node7, 100, 10, 60000, 9);

		/* build plans */
		Population plans = new Population(Population.NO_STREAMING);

		// create a person with a car-leg from link1 to link5, but an incomplete route
		Person person = new PersonImpl(new IdImpl(0));
		Plan plan = person.createPlan(true);
		Act a1 = plan.createAct("h", link1);
		a1.setEndTime(8*3600);
		Leg leg = plan.createLeg(BasicLeg.Mode.car);
		CarRoute route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link1, link5);
		route.setNodes(link1, NetworkUtils.getNodes(network, nodes), link5);
		leg.setRoute(route);
		Act a2 = plan.createAct("w", link5);
		a2.setEndTime(9*3600);
		leg = plan.createLeg(BasicLeg.Mode.car);
		route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, link5, link6);
		route.setLinks(link5, null, link6);
		leg.setRoute(route);
		plan.createAct("h", link6);
		plans.addPerson(person);

		/* build sim */
		return new QueueSimulation(network, plans, events);
	}

	/**
	 * A simple events handler that counts the number of enter link events on one specific link.
	 * Used by some tests in the class.
	 *
	 * @author mrieser
	 */
	private final static class EnterLinkEventCounter implements LinkEnterEventHandler {
		private final String linkId;
		private int counter = 0;
		public EnterLinkEventCounter(final String linkId) {
			this.linkId = linkId;
		}

		public void handleEvent(final LinkEnterEvent event) {
			if (event.linkId.equals(this.linkId)) this.counter++;
		}

		public void reset(final int iteration) {
			this.counter = 0;
		}

		public int getCounter() {
			return this.counter;
		}
	}

	private final static class LogCounter extends AppenderSkeleton {
		private int cntWARN = 0;
		private int cntERROR = 0;

		public LogCounter() {
			this.setThreshold(Level.WARN);
		}

		@Override
		protected void append(final LoggingEvent event) {
			if (event.getLevel() == Level.WARN) this.cntWARN++;
			if (event.getLevel() == Level.ERROR) this.cntERROR++;
		}

		public void close() {
		}

		public boolean requiresLayout() {
			return false;
		}

		public int getWarnCount() {
			return this.cntWARN;
		}

		public int getErrorCount() {
			return this.cntERROR;
		}
	}
}
