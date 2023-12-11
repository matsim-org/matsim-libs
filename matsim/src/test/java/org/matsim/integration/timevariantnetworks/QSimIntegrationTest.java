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

import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.testcases.utils.EventsLogger;
import org.matsim.vehicles.Vehicle;


/**
 * Tests that the QSim takes a TimeVariant Network into account.
 *
 * @author mrieser
 */
public class QSimIntegrationTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testFreespeed() {
		Config config = utils.loadConfig((String)null);
		config.network().setTimeVariantNetwork(true);
		Scenario scenario = ScenarioUtils.createScenario(config);

		Network network = createNetwork(scenario);
		Link link1 = network.getLinks().get(Id.create("1", Link.class));
		Link link2 = network.getLinks().get(Id.create("2", Link.class));
		Link link3 = network.getLinks().get(Id.create("3", Link.class));

		// add a freespeed change to 20 at 8am.
		NetworkChangeEvent change = new NetworkChangeEvent(8*3600.0);
		change.addLink(link2);
		change.setFreespeedChange(new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 20));
		final NetworkChangeEvent event = change;
		NetworkUtils.addNetworkChangeEvent(((Network)network),event);

		// create a population
		Population plans = scenario.getPopulation();
		Person person1 = createPersons(7*3600, link1, link3, network, 1).get(0);
		Person person2 = createPersons(9*3600, link1, link3, network, 1).get(0);
		plans.addPerson(person1);
		plans.addPerson(person2);

		// run the simulation with the timevariant network and the two persons
		EventsManager events = EventsUtils.createEventsManager();
		TestTravelTimeCalculator ttcalc = new TestTravelTimeCalculator(person1.getId(), person2.getId(), link2.getId());
		events.addHandler(ttcalc);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, events) //
			.run();

		// check that we get the expected result
		assertEquals(10.0 + 1.0, ttcalc.person1leaveTime - ttcalc.person1enterTime, MatsimTestUtils.EPSILON, "Person 1 should travel for 11 seconds.");
		assertEquals(5.0 + 1.0, ttcalc.person2leaveTime - ttcalc.person2enterTime, MatsimTestUtils.EPSILON, "Person 2 should travel for 6 seconds.");
	}

	/**
	 * Test the queue simulation for correct behavior if capacity of links is
	 * reduced during the run.
	 *
	 * @author illenberger
	 */
	@Test
	void testCapacity() {
		final int personsPerWave = 10;
		final double capacityFactor = 0.5;

		Config config = utils.loadConfig((String)null);
		config.network().setTimeVariantNetwork(true);
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);

		Network network = createNetwork(scenario);
		Link link1 = network.getLinks().get(Id.create("1", Link.class));
		Link link2 = network.getLinks().get(Id.create("2", Link.class));
		Link link3 = network.getLinks().get(Id.create("3", Link.class));
		/*
		 * Create a network change event that reduces the capacity.
		 */
		NetworkChangeEvent change1 = new NetworkChangeEvent(0);
		change1.addLink(link2);
		change1.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, capacityFactor));
		final NetworkChangeEvent event = change1;
		NetworkUtils.addNetworkChangeEvent(network,event);
		/*
		 * Create a network event the restores the capacity to its original value.
		 */
		NetworkChangeEvent change2 = new NetworkChangeEvent(3600);
		change2.addLink(link2);
		change2.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, 1/capacityFactor));
		final NetworkChangeEvent event1 = change2;
		NetworkUtils.addNetworkChangeEvent(network,event1);
		/*
		 * Create two waves of persons, each counting 10.
		 */
		Population pop = scenario.getPopulation();
		List<Person> persons1 = createPersons(0, link1, link3, network, personsPerWave);
		for(Person p : persons1) {
			pop.addPerson(p);
		}
		Person person1 = persons1.get(personsPerWave - 1);

		List<Person> persons2 = createPersons(3600, link1, link3, network, personsPerWave);
		for(Person p : persons2) {
			pop.addPerson(p);
		}
		Person person2 = persons2.get(personsPerWave - 1);
		/*
		 * Run the simulation with the time-variant network and the two waves of
		 * persons. Observe the last person of each wave.
		 */
		EventsManager events = EventsUtils.createEventsManager();
		TestTravelTimeCalculator ttcalc = new TestTravelTimeCalculator(person1.getId(), person2.getId(), link2.getId());
		events.addHandler(ttcalc);
		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, events) //
			.run();
		/*
		 * The last person of the first wave should have taken 20 s to travel
		 * link 3 (because of the spill-back). The last person of the second
		 * wave should have free-flow travel time.
		 */
		assertEquals(20.0, ttcalc.person1leaveTime - ttcalc.person1enterTime, MatsimTestUtils.EPSILON, "Person 1 should travel for 20 seconds.");
		assertEquals(10.0 + 1.0, ttcalc.person2leaveTime - ttcalc.person2enterTime, MatsimTestUtils.EPSILON, "Person 2 should travel for 11 seconds.");

	}

	/**
	 * Test the queue simulation for correct behavior if capacity of links is
	 * reduced to 0.
	 *
	 * @author dgrether
	 */
	@Test
	void testZeroCapacity() {
		final double capacityFactor = 0.0;

		Config config = utils.loadConfig((String)null);
		config.network().setTimeVariantNetwork(true);
		config.qsim().setStartTime(0.0);
		final double simEndTime = 7200.0;
		config.qsim().setEndTime(simEndTime);
		Scenario scenario = (MutableScenario) ScenarioUtils.createScenario(config);


		Network network = createNetwork(scenario);
		final Id<Link> id1 = Id.create("1", Link.class);
		final Id<Link> id2 = Id.create("2", Link.class);
		final Id<Link> id3 = Id.create("3", Link.class);

		Link link1 = network.getLinks().get(id1);
		Link link2 = network.getLinks().get(id2);
		Link link3 = network.getLinks().get(id3);
		/*
		 * Create a network change event that reduces the capacity.
		 */
		NetworkChangeEvent change1 = new NetworkChangeEvent(0);
		change1.addLink(link2);
		change1.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, capacityFactor));
		final NetworkChangeEvent event1 = change1;
		NetworkUtils.addNetworkChangeEvent(network,event1);
		/*
		 * Create two waves of persons, each counting 10.
		 */
		Population plans = scenario.getPopulation();
		List<Person> persons1 = createPersons(0, link1, link3, network, 1);
		final Id<Person> personId = persons1.get(0).getId();
		for(Person p : persons1) {
			plans.addPerson(p);
		}

		/*
		 * Run the simulation with the time-variant network and the two waves of
		 * persons. Observe the last person of each wave.
		 */
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new EventsLogger());
		events.addHandler(new LinkEnterEventHandler(){
			@Override
			public void reset(int iteration) {}

			@Override
			public void handleEvent(LinkEnterEvent event) {
				if (id2.equals(event.getLinkId()))
					Assertions.assertEquals(1.0, event.getTime(), MatsimTestUtils.EPSILON);
				if (id3.equals(event.getLinkId()))
					Assertions.fail("Link 3 should never be reached as capacity of link 2 is set to 0");
			}
		});

		events.addHandler(new PersonStuckEventHandler() {
			@Override
			public void reset(int iteration) {}

			@Override
			public void handleEvent(PersonStuckEvent event) {
				Assertions.assertEquals(id2, event.getLinkId());
				Assertions.assertEquals(simEndTime, event.getTime(), MatsimTestUtils.EPSILON);
				Assertions.assertEquals(personId, event.getPersonId());
			}
		});

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, events) //
			.run();
	}



	/**
	 * Creates a network with three links of length 100 m, capacity 3600 veh/h
	 * and freespeed 10 m/s.
	 *
	 * @author illenberger
	 */
	private static Network createNetwork(Scenario scenario) {
		// create a network
		final Network network = (Network) scenario.getNetwork();
		network.setCapacityPeriod(3600.0);

		// the network has 4 nodes and 3 links, each link by default 100 long and freespeed = 10 --> freespeed travel time = 10.0
		Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord((double) 0, (double) 0));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord((double) 100, (double) 0));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord((double) 200, (double) 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord((double) 300, (double) 0));
		final Node fromNode = node1;
		final Node toNode = node2;
		NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 10, (double) 3600, (double) 1 );
		final Node fromNode1 = node2;
		final Node toNode1 = node3;
		NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 100, (double) 10, (double) 3600, (double) 1 );
		final Node fromNode2 = node3;
		final Node toNode2 = node4;
		NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, (double) 100, (double) 10, (double) 3600, (double) 1 );

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
	private static List<Person> createPersons(final double depTime, final Link depLink, final Link destLink, final Network network,
			final int count) {
		double departureTime = depTime;
		List<Person> persons = new ArrayList<Person>(count);
		for(int i = 0; i < count; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i + (int) departureTime, Person.class));
			Plan plan1 = PersonUtils.createAndAddPlan(person, true);
			Activity a1 = PopulationUtils.createAndAddActivityFromLinkId(plan1, "h", depLink.getId());
			a1.setEndTime(departureTime);
			Leg leg1 = PopulationUtils.createAndAddLeg( plan1, TransportMode.car );
			leg1.setDepartureTime(departureTime);
			leg1.setTravelTime(10);
			NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(depLink.getId(), destLink.getId());
			route.setLinkIds(depLink.getId(), NetworkUtils.getLinkIds("2"), destLink.getId());
			leg1.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan1, "w", destLink.getId());

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
	private static class TestTravelTimeCalculator implements PersonEntersVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Id<Person> personId1;
		private final Id<Person> personId2;
		private final Id<Link> linkId;
		private Id<Vehicle> vehicleId1;
		private Id<Vehicle> vehicleId2;
		protected Double person1enterTime = null;
		protected Double person1leaveTime = null;
		protected Double person2enterTime = null;
		protected Double person2leaveTime = null;

		protected TestTravelTimeCalculator(final Id<Person> personId1, final Id<Person> personId2, final Id<Link> linkId) {
			this.personId1 = personId1;
			this.personId2 = personId2;
			this.linkId = linkId;
		}

		@Override
		public void handleEvent(final LinkEnterEvent event) {
			if (!event.getLinkId().equals(this.linkId)) {
				return;
			}
			if (event.getVehicleId().equals(this.vehicleId1)) {
				this.person1enterTime = event.getTime();
			} else if (event.getVehicleId().equals(this.vehicleId2)) {
				this.person2enterTime = event.getTime();
			}
		}

		@Override
		public void handleEvent(final LinkLeaveEvent event) {
			if (!event.getLinkId().equals(this.linkId)) {
				return;
			}
			if (event.getVehicleId().equals(this.vehicleId1)) {
				this.person1leaveTime = event.getTime();
			} else if (event.getVehicleId().equals(this.vehicleId2)) {
				this.person2leaveTime = event.getTime();
			}
		}

		@Override
		public void reset(final int iteration) {
			// nothing to do
		}

		@Override
		public void handleEvent(PersonEntersVehicleEvent event) {
			if (event.getPersonId().equals(personId1)){
				vehicleId1 = event.getVehicleId();
			} else if (event.getPersonId().equals(personId2)){
				vehicleId2 = event.getVehicleId();
			}
		}

	}
}
