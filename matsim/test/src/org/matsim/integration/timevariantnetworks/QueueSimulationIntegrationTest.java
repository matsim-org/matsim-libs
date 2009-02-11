/* *********************************************************************** *
 * project: org.matsim.*
 * TimeVariantNetwork_QueueSimulation_IntegrationTest.java
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

package org.matsim.integration.timevariantnetworks;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.BasicLeg;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.LinkLeaveEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.events.handler.LinkLeaveEventHandler;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.network.Link;
import org.matsim.network.NetworkChangeEvent;
import org.matsim.network.NetworkFactory;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.network.TimeVariantLinkImpl;
import org.matsim.network.NetworkChangeEvent.ChangeType;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
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
import org.matsim.utils.misc.Time;

/**
 * Tests that the QueueSimulation takes a TimeVariant Network into account.
 *
 * @author mrieser
 */
public class QueueSimulationIntegrationTest extends MatsimTestCase {

	public void testFreespeed() {
		loadConfig(null);

		NetworkLayer network = createNetwork();
		Link link1 = network.getLink("1");
		Link link2 = network.getLink("2");
		Link link3 = network.getLink("3");

		// add a freespeed change to 20 at 8am.
		NetworkChangeEvent change = new NetworkChangeEvent(8*3600.0);
		change.addLink(link2);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		network.addNetworkChangeEvent(change);

		// create a population
		Population plans = new Population(Population.NO_STREAMING);
		Person person1 = createPersons(7*3600, link1, link3, network, 1).get(0);
		Person person2 = createPersons(9*3600, link1, link3, network, 1).get(0);
		plans.addPerson(person1);
		plans.addPerson(person2);

		// run the simulation with the timevariant network and the two persons
		Events events = new Events();
		TestTravelTimeCalculator ttcalc = new TestTravelTimeCalculator(person1, person2, link2);
		events.addHandler(ttcalc);
		QueueSimulation qsim = new QueueSimulation(network, plans, events);
		qsim.run();

		// check that we get the expected result
		assertEquals("Person 1 should travel for 11 seconds.", 10.0 + 1.0, ttcalc.person1leaveTime - ttcalc.person1enterTime, EPSILON);
		assertEquals("Person 2 should travel for 6 seconds.", 5.0 + 1.0, ttcalc.person2leaveTime - ttcalc.person2enterTime, EPSILON);
	}

	/**
	 * Test the queue simulation for correct behavior if capacity of links is
	 * reduced during the run.
	 *
	 * @author illenberger
	 */
	public void testCapacity() {
		final int personsPerWave = 10;
		final double capacityFactor = 0.5;

		loadConfig(null);

		NetworkLayer network = createNetwork();
		Link link1 = network.getLink("1");
		Link link2 = network.getLink("2");
		Link link3 = network.getLink("3");
		/*
		 * Create a network change event that reduces the capacity.
		 */
		NetworkChangeEvent change1 = new NetworkChangeEvent(0);
		change1.addLink(link2);
		change1.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, capacityFactor));
		network.addNetworkChangeEvent(change1);
		/*
		 * Create a network event the restores the capacity to its original value.
		 */
		NetworkChangeEvent change2 = new NetworkChangeEvent(3600);
		change2.addLink(link2);
		change2.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, 1/capacityFactor));
		network.addNetworkChangeEvent(change2);
		/*
		 * Create two waves of persons, each counting 10.
		 */
		Population plans = new Population(Population.NO_STREAMING);
		List<Person> persons1 = createPersons(0, link1, link3, network, personsPerWave);
		for(Person p : persons1)
			plans.addPerson(p);
		Person person1 = persons1.get(personsPerWave - 1);

		List<Person> persons2 = createPersons(3600, link1, link3, network, personsPerWave);
		for(Person p : persons2)
			plans.addPerson(p);
		Person person2 = persons2.get(personsPerWave - 1);
		/*
		 * Run the simulation with the time-variant network and the two waves of
		 * persons. Observe the last person of each wave.
		 */
		Events events = new Events();
		TestTravelTimeCalculator ttcalc = new TestTravelTimeCalculator(person1, person2, link2);
		events.addHandler(ttcalc);
		QueueSimulation qsim = new QueueSimulation(network, plans, events);
		qsim.run();
		/*
		 * The last person of the first wave should have taken 20 s to travel
		 * link 3 (because of the spill-back). The last person of the second
		 * wave should have free-flow travel time.
		 */
		assertEquals("Person 1 should travel for 20 seconds.", 20.0, ttcalc.person1leaveTime - ttcalc.person1enterTime, EPSILON);
		assertEquals("Person 2 should travel for 11 seconds.", 10.0 + 1.0, ttcalc.person2leaveTime - ttcalc.person2enterTime, EPSILON);

	}

	/**
	 * Creates a network with three links of length 100 m, capacity 3600 veh/h
	 * and freespeed 10 m/s.
	 *
	 * @param world the world the network should belong to
	 * @return a network.
	 * @author illenberger
	 */
	private NetworkLayer createNetwork() {
		// create a network
		NetworkFactory nf = new NetworkFactory();
		nf.setLinkPrototype(TimeVariantLinkImpl.class);
		final NetworkLayer network = new NetworkLayer(nf);
		network.setCapacityPeriod(3600.0);

		// the network has 4 nodes and 3 links, each link by default 100 long and freespeed = 10 --> freespeed travel time = 10.0
		Node node1 = network.createNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createNode(new IdImpl("2"), new CoordImpl(100, 0));
		Node node3 = network.createNode(new IdImpl("3"), new CoordImpl(200, 0));
		Node node4 = network.createNode(new IdImpl("4"), new CoordImpl(300, 0));
		network.createLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);
		network.createLink(new IdImpl("2"), node2, node3, 100, 10, 3600, 1);
		network.createLink(new IdImpl("3"), node3, node4, 100, 10, 3600, 1);

		return network;
	}

	/**
	 * Creates <tt>count</tt> persons with departure time
	 * <tt>depTime<tt> + index(person).
	 * @param depTime the departure time for the first person.
	 * @param depLink the departure link.
	 * @param destLink the destination link.
	 * @param network
	 * @param count the number of persons to create
	 * @return a list of persons where the ordering corresponds to the departure times.
	 * @author illenberger
	 */
	private List<Person> createPersons(final double depTime, final Link depLink, final Link destLink, final NetworkLayer network,
			final int count) {
		double departureTime = depTime;
		List<Person> persons = new ArrayList<Person>(count);
		for(int i = 0; i < count; i++) {
			Person person = new PersonImpl(new IdImpl(i + (int)departureTime));
			Plan plan1 = person.createPlan(true);
			Act a1 = plan1.createAct("h", depLink);
			a1.setEndTime(departureTime);
			Leg leg1 = plan1.createLeg(BasicLeg.Mode.car);
			leg1.setDepartureTime(departureTime);
			leg1.setTravelTime(10);
			CarRoute route = (CarRoute) network.getFactory().createRoute(BasicLeg.Mode.car, depLink, destLink);
			route.setNodes(depLink, NetworkUtils.getNodes(network, "2 3"), destLink);
			leg1.setRoute(route);
			plan1.createAct("w", destLink);

			persons.add(person);
			departureTime++;
		}
		return persons;
	}

	/**
	 * A special EventHandler to get the link enter and link leave time for two persons on one specific link.
	 *
	 * @author mrieser
	 */
	private static class TestTravelTimeCalculator implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Person person1;
		private final Person person2;
		private final Link link;
		public double person1enterTime = Time.UNDEFINED_TIME;
		public double person1leaveTime = Time.UNDEFINED_TIME;
		public double person2enterTime = Time.UNDEFINED_TIME;
		public double person2leaveTime = Time.UNDEFINED_TIME;

		public TestTravelTimeCalculator(final Person person1, final Person person2, final Link link) {
			this.person1 = person1;
			this.person2 = person2;
			this.link = link;
		}

		public void handleEvent(final LinkEnterEvent event) {
			if (event.link != this.link) {
				return;
			}
			if (event.agent == this.person1) {
				this.person1enterTime = event.time;
			} else if (event.agent == this.person2) {
				this.person2enterTime = event.time;
			}
		}

		public void handleEvent(final LinkLeaveEvent event) {
			if (event.link != this.link) {
				return;
			}
			if (event.agent == this.person1) {
				this.person1leaveTime = event.time;
			} else if (event.agent == this.person2) {
				this.person2leaveTime = event.time;
			}
		}

		public void reset(final int iteration) {
			// nothing to do
		}

	}
}
