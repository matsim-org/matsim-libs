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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.TimeVariantLinkFactory;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestCase;

/**
 * Tests that the QueueSimulation takes a TimeVariant Network into account.
 *
 * @author mrieser
 */
public class QueueSimulationIntegrationTest extends MatsimTestCase {

	public void testFreespeed() {
		ScenarioImpl scenario = new ScenarioImpl(loadConfig(null));

		NetworkLayer network = createNetwork(scenario);
		LinkImpl link1 = network.getLinks().get(new IdImpl("1"));
		LinkImpl link2 = network.getLinks().get(new IdImpl("2"));
		LinkImpl link3 = network.getLinks().get(new IdImpl("3"));

		// add a freespeed change to 20 at 8am.
		NetworkChangeEvent change = new NetworkChangeEvent(8*3600.0);
		change.addLink(link2);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE, 20));
		network.addNetworkChangeEvent(change);

		// create a population
		PopulationImpl plans = scenario.getPopulation();
		PersonImpl person1 = createPersons(7*3600, link1, link3, network, 1).get(0);
		PersonImpl person2 = createPersons(9*3600, link1, link3, network, 1).get(0);
		plans.addPerson(person1);
		plans.addPerson(person2);

		// run the simulation with the timevariant network and the two persons
		EventsManagerImpl events = new EventsManagerImpl();
		TestTravelTimeCalculator ttcalc = new TestTravelTimeCalculator(person1, person2, link2.getId());
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

		ScenarioImpl scenario = new ScenarioImpl(loadConfig(null));

		NetworkLayer network = createNetwork(scenario);
		LinkImpl link1 = network.getLinks().get(new IdImpl("1"));
		LinkImpl link2 = network.getLinks().get(new IdImpl("2"));
		LinkImpl link3 = network.getLinks().get(new IdImpl("3"));
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
		PopulationImpl plans = scenario.getPopulation();
		List<PersonImpl> persons1 = createPersons(0, link1, link3, network, personsPerWave);
		for(PersonImpl p : persons1) {
			plans.addPerson(p);
		}
		PersonImpl person1 = persons1.get(personsPerWave - 1);

		List<PersonImpl> persons2 = createPersons(3600, link1, link3, network, personsPerWave);
		for(PersonImpl p : persons2) {
			plans.addPerson(p);
		}
		PersonImpl person2 = persons2.get(personsPerWave - 1);
		/*
		 * Run the simulation with the time-variant network and the two waves of
		 * persons. Observe the last person of each wave.
		 */
		EventsManagerImpl events = new EventsManagerImpl();
		TestTravelTimeCalculator ttcalc = new TestTravelTimeCalculator(person1, person2, link2.getId());
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
	private NetworkLayer createNetwork(ScenarioImpl scenario) {
		// create a network
		NetworkFactoryImpl nf = scenario.getNetwork().getFactory();
		nf.setLinkFactory(new TimeVariantLinkFactory());
		final NetworkLayer network = scenario.getNetwork();
		network.setCapacityPeriod(3600.0);

		// the network has 4 nodes and 3 links, each link by default 100 long and freespeed = 10 --> freespeed travel time = 10.0
		Node node1 = network.createAndAddNode(new IdImpl("1"), new CoordImpl(0, 0));
		Node node2 = network.createAndAddNode(new IdImpl("2"), new CoordImpl(100, 0));
		Node node3 = network.createAndAddNode(new IdImpl("3"), new CoordImpl(200, 0));
		Node node4 = network.createAndAddNode(new IdImpl("4"), new CoordImpl(300, 0));
		network.createAndAddLink(new IdImpl("1"), node1, node2, 100, 10, 3600, 1);
		network.createAndAddLink(new IdImpl("2"), node2, node3, 100, 10, 3600, 1);
		network.createAndAddLink(new IdImpl("3"), node3, node4, 100, 10, 3600, 1);

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
	private List<PersonImpl> createPersons(final double depTime, final LinkImpl depLink, final LinkImpl destLink, final NetworkLayer network,
			final int count) {
		double departureTime = depTime;
		List<PersonImpl> persons = new ArrayList<PersonImpl>(count);
		for(int i = 0; i < count; i++) {
			PersonImpl person = new PersonImpl(new IdImpl(i + (int)departureTime));
			PlanImpl plan1 = person.createAndAddPlan(true);
			ActivityImpl a1 = plan1.createAndAddActivity("h", depLink.getId());
			a1.setEndTime(departureTime);
			LegImpl leg1 = plan1.createAndAddLeg(TransportMode.car);
			leg1.setDepartureTime(departureTime);
			leg1.setTravelTime(10);
			NetworkRouteWRefs route = (NetworkRouteWRefs) network.getFactory().createRoute(TransportMode.car, depLink, destLink);
			route.setLinks(depLink, NetworkUtils.getLinks(network, "2"), destLink);
			leg1.setRoute(route);
			plan1.createAndAddActivity("w", destLink.getId());

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

		private final PersonImpl person1;
		private final PersonImpl person2;
		private final Id linkId;
		protected double person1enterTime = Time.UNDEFINED_TIME;
		protected double person1leaveTime = Time.UNDEFINED_TIME;
		protected double person2enterTime = Time.UNDEFINED_TIME;
		protected double person2leaveTime = Time.UNDEFINED_TIME;

		protected TestTravelTimeCalculator(final PersonImpl person1, final PersonImpl person2, final Id linkId) {
			this.person1 = person1;
			this.person2 = person2;
			this.linkId = linkId;
		}

		public void handleEvent(final LinkEnterEvent event) {
			if (!event.getLinkId().equals(this.linkId)) {
				return;
			}
			if (event.getPersonId().equals(this.person1.getId())) {
				this.person1enterTime = event.getTime();
			} else if (event.getPersonId().equals(this.person2.getId())) {
				this.person2enterTime = event.getTime();
			}
		}

		public void handleEvent(final LinkLeaveEvent event) {
			if (!event.getLinkId().equals(this.linkId)) {
				return;
			}
			if (event.getPersonId().equals(this.person1.getId())) {
				this.person1leaveTime = event.getTime();
			} else if (event.getPersonId().equals(this.person2.getId())) {
				this.person2leaveTime = event.getTime();
			}
		}

		public void reset(final int iteration) {
			// nothing to do
		}

	}
}
