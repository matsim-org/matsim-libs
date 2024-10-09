/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.hermes;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class StorageCapacityTest {

	/**
	 * Tests that the storage capacity can be reached (but not exceeded) by agents driving over a link.
	 *
	 * @author jfbischoff
	 */
	@Test
	void testStorageCapacity() {
		ScenarioImporter.flush();
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		config.hermes().setStuckTime(Integer.MAX_VALUE);
		Scenario scenario = ScenarioUtils.createScenario(config);
		var links = generateNetwork(scenario.getNetwork());

		for (int i = 1; i <= 600; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", links.get(0).getId());
			a.setEndTime(7 * 3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.car);
			TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			NetworkRoute route = scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, links.get(0).getId(), links.get(4).getId());
			route.setLinkIds(links.get(0).getId(), List.of(links.get(1).getId(), links.get(2).getId(), links.get(3).getId()), links.get(4).getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", links.get(4).getId());
			scenario.getPopulation().addPerson(person);
		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();

		VehiclesOnLinkCounter counter3 = new VehiclesOnLinkCounter(links.get(3).getId());
		events.addHandler(counter3);
		VehiclesOnLinkCounter counter2 = new VehiclesOnLinkCounter(links.get(2).getId());
		events.addHandler(counter2);
		VehiclesOnLinkCounter counter1 = new VehiclesOnLinkCounter(links.get(1).getId());
		events.addHandler(counter1);
		/* run sim */
		Hermes sim = HermesTest.createHermes(scenario, events);
		sim.run();

		System.out.println(counter3.currentMax);
		System.out.println(counter2.currentMax);
		System.out.println(counter1.currentMax);
		Assertions.assertEquals(14, counter3.currentMax);  // the bottleneck link can store 14 vehicles
		Assertions.assertEquals(100, counter2.currentMax); //spillback 100 vehicles
		Assertions.assertEquals(100, counter1.currentMax); // spillback

	}

	/**
	 * Tests that the storage capacity can be reached (but not exceeded) by agents driving over a link.
	 *
	 * @author jfbischoff
	 */
	@Test
	void testStorageCapacityDownscaling() {
		ScenarioImporter.flush();
		Config config = ConfigUtils.createConfig();
		config.hermes().setStuckTime(Integer.MAX_VALUE);
		config.hermes().setFlowCapacityFactor(0.1);
		config.hermes().setStorageCapacityFactor(0.1);
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		Scenario scenario = ScenarioUtils.createScenario(config);
		var links = generateNetwork(scenario.getNetwork());

		for (int i = 1; i <= 120; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", links.get(0).getId());
			a.setEndTime(7 * 3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.car);
			TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			NetworkRoute route = scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, links.get(0).getId(), links.get(4).getId());
			route.setLinkIds(links.get(0).getId(), List.of(links.get(1).getId(), links.get(2).getId(), links.get(3).getId()), links.get(4).getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", links.get(4).getId());
			scenario.getPopulation().addPerson(person);

		}

		/* build events */
		EventsManager events = EventsUtils.createEventsManager();

		VehiclesOnLinkCounter counter3 = new VehiclesOnLinkCounter(links.get(3).getId());
		events.addHandler(counter3);
		VehiclesOnLinkCounter counter2 = new VehiclesOnLinkCounter(links.get(2).getId());
		events.addHandler(counter2);
		VehiclesOnLinkCounter counter1 = new VehiclesOnLinkCounter(links.get(1).getId());
		events.addHandler(counter1);
		/* run sim */
		Hermes sim = HermesTest.createHermes(scenario, events);
		sim.run();

		System.out.println(counter3.currentMax);
		System.out.println(counter2.currentMax);
		System.out.println(counter1.currentMax);
		Assertions.assertEquals(1, counter3.currentMax);  // the bottleneck link can store 14 vehicles, but one vehicle counts for 10, so only 1 works
		Assertions.assertEquals(10, counter2.currentMax); //spillback 100 vehicles
		Assertions.assertEquals(10, counter1.currentMax); // spillback

	}

	/**
	 * Tests that the storage capacity can be reached (but not exceeded) by agents driving over a link.
	 *
	 * @author jfbischoff
	 */
	@Test
	void testStorageCapacityWithDifferentPCUs() {
		ScenarioImporter.flush();
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
		config.hermes().setStuckTime(Integer.MAX_VALUE);
		Scenario scenario = ScenarioUtils.createScenario(config);
		var links = generateNetwork(scenario.getNetwork());
		VehicleType tractor = VehicleUtils.createVehicleType(Id.create("tractor", VehicleType.class));
		tractor.setPcuEquivalents(2.0);
		scenario.getVehicles().addVehicleType(tractor);
		VehicleType car = VehicleUtils.createVehicleType(Id.create("car", VehicleType.class));
		car.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(car);

		for (int i = 1; i <= 500; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", links.get(0).getId());
			a.setEndTime(7 * 3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.car);
			TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			NetworkRoute route = scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, links.get(0).getId(), links.get(4).getId());
			route.setLinkIds(links.get(0).getId(), List.of(links.get(1).getId(), links.get(2).getId(), links.get(3).getId()), links.get(4).getId());
			leg.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", links.get(4).getId());
			scenario.getPopulation().addPerson(person);
			//every second person gets an unflowy, but speedy tractor
			if (i % 2 == 1) {
				Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId(person.getId()), tractor);
				scenario.getVehicles().addVehicle(vehicle);
				VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(TransportMode.car, vehicle.getId()));
			}
			if (i % 2 == 0) {
				Vehicle vehicle = VehicleUtils.createVehicle(Id.createVehicleId(person.getId()), car);
				scenario.getVehicles().addVehicle(vehicle);
				VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(TransportMode.car, vehicle.getId()));
			}
		}



		/* build events */
		EventsManager events = EventsUtils.createEventsManager();

		VehiclesOnLinkCounter counter3 = new VehiclesOnLinkCounter(links.get(3).getId());
		events.addHandler(counter3);
		VehiclesOnLinkCounter counter2 = new VehiclesOnLinkCounter(links.get(2).getId());
		events.addHandler(counter2);
		/* run sim */
		Hermes sim = HermesTest.createHermes(scenario, events);
		sim.run();

		System.out.println(counter3.currentMax);
		System.out.println(counter2.currentMax);
		Assertions.assertEquals(10, counter3.currentMax);  // the bottleneck link can store 14 vehicles
		Assertions.assertEquals(67, counter2.currentMax); //spillback 100 vehicles

	}

	/**
	 * Tests that the storage capacity can be reached (but not exceeded) by agents driving over a link, changing vehicles in between. trip is: car -> walk back -> truck for all agents
	 *
	 * @author jfbischoff
	 */
	@Test
	void testStorageCapacityWithVaryingPCUs() {
		ScenarioImporter.flush();
		Config config = ConfigUtils.createConfig();
		config.routing().setNetworkModes(Set.of(TransportMode.car, TransportMode.truck));
		config.hermes().setStuckTime(Integer.MAX_VALUE);
		config.hermes().setMainModes(Set.of(TransportMode.car, TransportMode.truck));
		Scenario scenario = ScenarioUtils.createScenario(config);
		var links = generateNetwork(scenario.getNetwork());
		VehicleType tractor = VehicleUtils.createVehicleType(Id.create("tractor", VehicleType.class));
		tractor.setPcuEquivalents(2.0);
		scenario.getVehicles().addVehicleType(tractor);
		VehicleType car = VehicleUtils.createVehicleType(Id.create("car", VehicleType.class));
		car.setPcuEquivalents(1.0);
		scenario.getVehicles().addVehicleType(car);

		for (int i = 1; i <= 500; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			Plan plan = PersonUtils.createAndAddPlan(person, true);
			Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", links.get(0).getId());
			a.setEndTime(7 * 3600 - 1812);
			Leg leg = PopulationUtils.createAndAddLeg(plan, TransportMode.car);
			TripStructureUtils.setRoutingMode(leg, TransportMode.car);
			NetworkRoute route = scenario.getPopulation().getFactory().getRouteFactories().createRoute(NetworkRoute.class, links.get(0).getId(), links.get(4).getId());
			route.setLinkIds(links.get(0).getId(), List.of(links.get(1).getId(), links.get(2).getId(), links.get(3).getId()), links.get(4).getId());
			leg.setRoute(route);

			Activity b = PopulationUtils.createAndAddActivityFromLinkId(plan, "w", links.get(4).getId());
			b.setEndTime(15 * 3600 - 1812);
			Leg leg2 = PopulationUtils.createAndAddLeg(plan, TransportMode.walk);
			TripStructureUtils.setRoutingMode(leg2, TransportMode.walk);
			Route route2 = new GenericRouteFactory().createRoute(links.get(4).getId(), links.get(0).getId());
			route2.setTravelTime(3600);
			route2.setDistance(1000);
			Activity c = PopulationUtils.createAndAddActivityFromLinkId(plan, "h", links.get(0).getId());
			c.setEndTime(18 * 3600 - 1812);

			Leg leg3 = PopulationUtils.createAndAddLeg(plan, TransportMode.truck);
			TripStructureUtils.setRoutingMode(leg3, TransportMode.truck);
			leg3.setRoute(route);
			PopulationUtils.createAndAddActivityFromLinkId(plan, "w", links.get(4).getId());

			scenario.getPopulation().addPerson(person);
			Vehicle tractorv = VehicleUtils.createVehicle(Id.createVehicleId("truck_" + person.getId().toString()), tractor);
			scenario.getVehicles().addVehicle(tractorv);
			VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(TransportMode.truck, tractorv.getId()));

			Vehicle carv = VehicleUtils.createVehicle(Id.createVehicleId(person.getId()), car);
			scenario.getVehicles().addVehicle(carv);
			VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(TransportMode.car, carv.getId()));

		}



		/* build events */
		EventsManager events = EventsUtils.createEventsManager();

		VehiclesOnLinkCounter counter3 = new VehiclesOnLinkCounter(links.get(3).getId());
		events.addHandler(counter3);
		VehiclesOnLinkCounter counter2 = new VehiclesOnLinkCounter(links.get(2).getId());
		events.addHandler(counter2);

		VehiclesOnLinkCounter counter3pm = new VehiclesOnLinkCounter(links.get(3).getId(), 17 * 3600);
		events.addHandler(counter3pm);
		VehiclesOnLinkCounter counter2pm = new VehiclesOnLinkCounter(links.get(2).getId(), 17 * 3600);
		events.addHandler(counter2pm);
		/* run sim */
		Hermes sim = HermesTest.createHermes(scenario, events, false);
		sim.run();

		System.out.println(counter3.currentMax);
		System.out.println(counter2.currentMax);
		System.out.println(counter3pm.currentMax);
		System.out.println(counter2pm.currentMax);
		Assertions.assertEquals(14, counter3.currentMax);  // the bottleneck link can store 14 cars
		Assertions.assertEquals(100, counter2.currentMax); //spillback 100 cars

		//the following asserts fail on jenkins, but work on appveyor and travis
		//Assert.assertEquals(7, counter3pm.currentMax);  // the bottleneck link can store 7 tractors
		//Assert.assertEquals(50, counter2pm.currentMax); //spillback 50 vehicles

	}

	private List<Link> generateNetwork(Network network) {
		network.setCapacityPeriod(Time.parseTime("1:00:00"));
		var node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(0, 0));
		var node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(100, 0));
		var node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(850, 0));
		var node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(1600, 0));
		var node5 = NetworkUtils.createAndAddNode(network, Id.create("5", Node.class), new Coord(1700, 0));
		var node6 = NetworkUtils.createAndAddNode(network, Id.create("6", Node.class), new Coord(1800, 0));

		var link1 = NetworkUtils.createAndAddLink(network, Id.create("1", Link.class), node1, node2, 105, 100, 3600, 1);
		var link2 = NetworkUtils.createAndAddLink(network, Id.create("2", Link.class), node2, node3, 750, 100, 3600, 1);
		var link3 = NetworkUtils.createAndAddLink(network, Id.create("3", Link.class), node3, node4, 750, 100, 3600, 1);
		var link4 = NetworkUtils.createAndAddLink(network, Id.create("4", Link.class), node4, node5, 105, 100, 360, 1);
		var link5 = NetworkUtils.createAndAddLink(network, Id.create("5", Link.class), node5, node6, 105, 100, 3600, 1);

		return List.of(link1, link2, link3, link4, link5);
	}

	static class VehiclesOnLinkCounter implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Id<Link> link;
		private final double starttime;
		int currentMax = 0;
		int vehiclesOnLink = 0;

		VehiclesOnLinkCounter(Id<Link> relevantLink) {
			this.link = relevantLink;
			starttime = 0;
		}
		VehiclesOnLinkCounter(Id<Link> relevantLink, double starttime) {
			this.link = relevantLink;
			this.starttime = starttime;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if (event.getLinkId().equals(link) && event.getTime() > starttime) {
				vehiclesOnLink++;
				if (vehiclesOnLink > currentMax) {
					currentMax = vehiclesOnLink;
				}
			}
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if (event.getLinkId().equals(link) && event.getTime() > starttime) {
				vehiclesOnLink--;
			}
		}
	}

}
