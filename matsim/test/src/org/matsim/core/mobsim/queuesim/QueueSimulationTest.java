/* *********************************************************************** *
 * project: org.matsim.*
 * QueueSimulationTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2009 by the members listed in the COPYING,  *
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

package org.matsim.core.mobsim.queuesim;

import java.util.ArrayList;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.Leg;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.LinkEnterEvent;
import org.matsim.core.events.handler.LinkEnterEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

public class QueueSimulationTest extends MatsimTestCase {

	/**
	 * This test is mostly useful for manual debugging, because only a single agent is simulated
	 * on a very simple network.
	 *
	 * @author mrieser
	 */
	public void testSingleAgent() {
		Fixture f = new Fixture();

		// add a single person with leg from link1 to link3
		Person person = new PersonImpl(new IdImpl(0));
		Plan plan = person.createPlan(true);
		Activity a1 = plan.createAct("h", f.link1);
		a1.setEndTime(6*3600);
		Leg leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
		route.setNodes(f.link1, f.nodes23, f.link3);
		leg.setRoute(route);
		plan.createAct("w", f.link3);
		f.plans.addPerson(person);

		/* build events */
		Events events = new Events();
		EventCollector collector = new EventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		assertEquals("wrong number of link enter events.", 2, collector.events.size());
		assertEquals("wrong time in first event.", 6.0*3600 + 1, collector.events.get(0).getTime(), EPSILON);
		assertEquals("wrong time in second event.", 6.0*3600 + 102, collector.events.get(1).getTime(), EPSILON);
	}

	/**
	 * This test is mostly useful for manual debugging, because only two single agents are simulated
	 * on a very simple network.
	 *
	 * @author mrieser
	 */
	public void testTwoAgent() {
		Fixture f = new Fixture();

		// add two persons with leg from link1 to link3, the first starting at 6am, the second at 7am
		for (int i = 0; i < 2; i++) {
			Person person = new PersonImpl(new IdImpl(i));
			Plan plan = person.createPlan(true);
			Activity a1 = plan.createAct("h", f.link1);
			a1.setEndTime((6+i)*3600);
			Leg leg = plan.createLeg(TransportMode.car);
			NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
			route.setNodes(f.link1, f.nodes23, f.link3);
			leg.setRoute(route);
			plan.createAct("w", f.link3);
			f.plans.addPerson(person);
		}

		/* build events */
		Events events = new Events();
		EventCollector collector = new EventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		assertEquals("wrong number of link enter events.", 4, collector.events.size());
		assertEquals("wrong time in first event.", 6.0*3600 + 1, collector.events.get(0).getTime(), EPSILON);
		assertEquals("wrong time in second event.", 6.0*3600 + 102, collector.events.get(1).getTime(), EPSILON);
		assertEquals("wrong time in first event.", 7.0*3600 + 1, collector.events.get(2).getTime(), EPSILON);
		assertEquals("wrong time in second event.", 7.0*3600 + 102, collector.events.get(3).getTime(), EPSILON);
	}

	/*package*/ static class EventCollector implements LinkEnterEventHandler {

		public final ArrayList<LinkEnterEvent> events = new ArrayList<LinkEnterEvent>();

		public void handleEvent(final LinkEnterEvent event) {
			this.events.add(event);
		}

		public void reset(final int iteration) {
			this.events.clear();
		}

	}

	/**
	 * Tests that no Exception occurs if an agent has no leg at all.
	 */
	public void testAgentWithoutLeg() {
		Fixture f = new Fixture();

		
		Person person = new PersonImpl(new IdImpl(1));
		Plan plan = person.createPlan(true);
		Activity act = plan.createAct("home", f.link1);
		f.plans.addPerson(person);
		
		/* build events */
		Events events = new Events();
		EventCollector collector = new EventCollector();
		events.addHandler(collector);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
		sim.run();

		/* finish */
		assertEquals("wrong number of link enter events.", 0, collector.events.size());
	}
	
	/**
	 * Tests that the flow capacity can be reached (but not exceeded) by
	 * agents driving over a link.
	 *
	 * @author mrieser
	 */
	public void testFlowCapacityDriving() {
		Fixture f = new Fixture();

		// add a first person with leg from link1 to link3, let it start early, so the simulation can accumulate buffer capacity
		Person person = new PersonImpl(new IdImpl(0));
		Plan plan = person.createPlan(true);
		Activity a1 = plan.createAct("h", f.link1);
		a1.setEndTime(6*3600 - 500);
		Leg leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
		route.setNodes(f.link1, f.nodes23, f.link3);
		leg.setRoute(route);
		plan.createAct("w", f.link3);
		f.plans.addPerson(person);

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
			Activity a = plan.createAct("h", f.link1);
			a.setEndTime(7*3600 - 1902);
			leg = plan.createLeg(TransportMode.car);
			route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
			route.setNodes(f.link1, f.nodes23, f.link3);
			leg.setRoute(route);
			plan.createAct("w", f.link3);
			f.plans.addPerson(person);
		}

		/* build events */
		Events events = new Events();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
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
		Fixture f = new Fixture();

		// add a first person with leg from link1 to link3, let it start early, so the simulation can accumulate buffer capacity
		Person person = new PersonImpl(new IdImpl(0));
		Plan plan = person.createPlan(true);
		Activity a1 = plan.createAct("h", f.link1);
		a1.setEndTime(6*3600 - 500);
		Leg leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
		route.setNodes(f.link1, f.nodes23, f.link3);
		leg.setRoute(route);
		plan.createAct("w", f.link3);
		f.plans.addPerson(person);

		// add a lot of persons with legs from link2 to link3
		for (int i = 1; i <= 10000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
			Activity a2 = plan.createAct("h", f.link2);
			a2.setEndTime(7*3600 - 1801);
			leg = plan.createLeg(TransportMode.car);
			route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link2, f.link3);
			route.setNodes(f.link2, f.nodes3, f.link3);
			leg.setRoute(route);
			plan.createAct("w", f.link3);
			f.plans.addPerson(person);
		}

		/* build events */
		Events events = new Events();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
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
		Fixture f = new Fixture();

		// add a first person with leg from link1 to link3, let it start early, so the simulation can accumulate buffer capacity
		Person person = new PersonImpl(new IdImpl(0));
		Plan plan = person.createPlan(true);
		Activity a1 = plan.createAct("h", f.link1);
		a1.setEndTime(6*3600 - 500);
		Leg leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, f.link3);
		route.setNodes(f.link1, f.nodes23, f.link3);
		leg.setRoute(route);
		plan.createAct("w", f.link3);
		f.plans.addPerson(person);

		// add a lot of persons with legs from link2 to link3
		for (int i = 1; i <= 5000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
			Activity a2 = plan.createAct("h", f.link2);
			a2.setEndTime(7*3600 - 1801);
			leg = plan.createLeg(TransportMode.car);
			route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link2, f.link3);
			route.setNodes(f.link2, f.nodes3, f.link3);
			leg.setRoute(route);
			plan.createAct("w", f.link3);
			f.plans.addPerson(person);
		}
		// add a lot of persons with legs from link1 to link3
		for (int i = 5001; i <= 10000; i++) {
			person = new PersonImpl(new IdImpl(i));
			plan = person.createPlan(true);
			Activity a2 = plan.createAct("h", f.link1);
			a2.setEndTime(7*3600 - 1902);
			leg = plan.createLeg(TransportMode.car);
			route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link2, f.link3);
			route.setNodes(f.link1, f.nodes23, f.link3);
			leg.setRoute(route);
			plan.createAct("w", f.link3);
			f.plans.addPerson(person);
		}

		/* build events */
		Events events = new Events();
		VolumesAnalyzer vAnalyzer = new VolumesAnalyzer(3600, 9*3600, f.network);
		events.addHandler(vAnalyzer);

		/* run sim */
		QueueSimulation sim = new QueueSimulation(f.network, f.plans, events);
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
	 *
	 * @author mrieser
	 */
	public void testConsistentRoutes_WrongRoute() {
		new LogCounter();
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "2 3", "5", events); // route should continue on link 4
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}
	
	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * starts at another link than the previous activity is located at.
	 *
	 * @author mrieser
	 */
	public void testConsistentRoutes_WrongStartLink() {
		new LogCounter();
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("2", "3 4", "5", events); // first act is on link 1, not 2
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}
	
	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * starts at another link than the previous activity is located at.
	 *
	 * @author mrieser
	 */
	public void testConsistentRoutes_WrongEndLink() {
		new LogCounter();
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "2 3", "4", events); // second act is on link 5, not 4
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/**
	 * Tests that the QueueSimulation reports a problem if the route of a vehicle
	 * does not specify all nodes, so it is unclear at one node or another how to
	 * continue.
	 *
	 * @author mrieser
	 */
	public void testConsistentRoutes_ImpossibleRoute() {
		Events events = new Events();
		EnterLinkEventCounter counter = new EnterLinkEventCounter("6");
		events.addHandler(counter);
		LogCounter logger = runConsistentRoutesTestSim("1", "2 4", "5", events); // link 3 is missing
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
		LogCounter logger = runConsistentRoutesTestSim("1", "", "5", events); // no links at all
		assertEquals(0, counter.getCounter()); // the agent should have been removed from the sim, so nobody travels there
		assertTrue((logger.getWarnCount() + logger.getErrorCount()) > 0); // there should have been at least a warning
	}

	/** Prepares miscellaneous data for the testConsistentRoutes() tests:
	 * Creates a network of 6 links, and a population of one person driving from
	 * link 1 to link 5, and then from link 5 to link 6.
	 *
	 * @param startLinkId the start link of the route for the first leg
	 * @param linkIds the links the agent should travel along on the first leg
	 * @param endLinkId the end link of the route for the first leg
	 * @param events the Events object to be used by the simulation.
	 * @return A QueueSimulation which can be started immediately.
	 *
	 * @author mrieser
	 **/
	private LogCounter runConsistentRoutesTestSim(final String startLinkId, final String linkIds, final String endLinkId, final Events events) {
		Fixture f = new Fixture();

		/* enhance network */
		Node node5 = f.network.createNode(new IdImpl("5"), new CoordImpl(3100, 0));
		Node node6 = f.network.createNode(new IdImpl("6"), new CoordImpl(3200, 0));
		Node node7 = f.network.createNode(new IdImpl("7"), new CoordImpl(3300, 0));
		f.network.createLink(new IdImpl("4"), f.node4, node5, 1000, 10, 6000, 2);
		Link link5 = f.network.createLink(new IdImpl("5"), node5, node6, 100, 10, 60000, 9);
		Link link6 = f.network.createLink(new IdImpl("6"), node6, node7, 100, 10, 60000, 9);

		// create a person with a car-leg from link1 to link5, but an incomplete route
		Person person = new PersonImpl(new IdImpl(0));
		Plan plan = person.createPlan(true);
		Activity a1 = plan.createAct("h", f.link1);
		a1.setEndTime(8*3600);
		Leg leg = plan.createLeg(TransportMode.car);
		NetworkRoute route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, f.link1, link5);
		route.setLinks(f.network.getLink(new IdImpl(startLinkId)), NetworkUtils.getLinks(f.network, linkIds), f.network.getLink(new IdImpl(endLinkId)));
		leg.setRoute(route);
		Activity a2 = plan.createAct("w", link5);
		a2.setEndTime(9*3600);
		leg = plan.createLeg(TransportMode.car);
		route = (NetworkRoute) f.network.getFactory().createRoute(TransportMode.car, link5, link6);
		route.setLinks(link5, null, link6);
		leg.setRoute(route);
		plan.createAct("h", link6);
		f.plans.addPerson(person);

		/* run sim with special logger */		
		LogCounter logger = new LogCounter();
		Logger.getRootLogger().addAppender(logger);
		new QueueSimulation(f.network, f.plans, events).run();
		Logger.getRootLogger().removeAppender(logger);
		
		return logger;
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
			if (event.getLinkId().toString().equals(this.linkId)) this.counter++;
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

	/**
	 * Initializes some commonly used data in the tests.
	 *
	 * @author mrieser
	 */
	private static final class Fixture {
		final Config config;
		final NetworkLayer network;
		final Node node1;
		final Node node2;
		final Node node3;
		final Node node4;
		final Link link1;
		final Link link2;
		final Link link3;
		final Population plans;
		final ArrayList<Node> nodes3;
		final ArrayList<Node> nodes23;

		public Fixture() {
			this.config = Gbl.createConfig(null);
			this.config.simulation().setFlowCapFactor(1.0);
			this.config.simulation().setStorageCapFactor(1.0);

			/* build network */
			this.network = new NetworkLayer();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			this.node1 = this.network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
			this.node2 = this.network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
			this.node3 = this.network.createNode(new IdImpl("3"), new CoordImpl(1100, 0));
			this.node4 = this.network.createNode(new IdImpl("4"), new CoordImpl(1200, 0));
			this.link1 = this.network.createLink(new IdImpl("1"), this.node1, this.node2, 100, 10, 60000, 9);
			this.link2 = this.network.createLink(new IdImpl("2"), this.node2, this.node3, 1000, 10, 6000, 2);
			this.link3 = this.network.createLink(new IdImpl("3"), this.node3, this.node4, 100, 10, 60000, 9);

			/* build plans */
			this.plans = new PopulationImpl();

			this.nodes3 = new ArrayList<Node>();
			this.nodes3.add(this.node3);

			this.nodes23 = new ArrayList<Node>();
			this.nodes23.add(this.node2);
			this.nodes23.add(this.node3);
		}
	}
}
