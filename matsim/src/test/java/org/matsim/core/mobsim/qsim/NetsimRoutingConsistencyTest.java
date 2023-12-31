
/* *********************************************************************** *
 * project: org.matsim.*
 * NetsimRoutingConsistencyTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.mobsim.qsim;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

	public class NetsimRoutingConsistencyTest {
		/*
* This test shows that the travel time that is predicted with the
* NetworkRoutingModule is NOT equivalent to the travel time that is produced by
* the Netsim.
 *
* The scenario is as follows:
 *
* N1 ----- N2 ----- N3 ----- N4 ----- N5
*     L12      L23      L34      L45
 *
* There are nodes Ni and connecting links Lij. There is one agent P who wants
* to depart at L12 and arrive at L45. He has a plan with an activity at L12 and
* another one at L45 and a connecting leg by car.
 *
* This car leg is produced (as would be in the full stack simulation) using the
* NetworkRoutingModule. For the disutility OnlyTimeDependentDisutility is used,
* for the TravelTime, freespeed is used. The freespeed of all links is 10,
* while the length is 1000, hence the traversal time for each link is 100.
 *
* Accordingly, the NetworkRoutingModule produces a Leg for the agent with a
* travel time of
 *
*    routingTravelTime = 200.0
 *
* because link L23 and L45 are taken into account. As expected the routing goes
* from the end node of the departure link to the start node of the arrival
* link. We already know that this will not be the true Netsim simulated time,
* because traversal times are rounded up. So we would expect a
 *
*    adjustedRoutingTravelTime = 202.0
 *
* because we need to add 1s per traversed link.
 *
* Now, the scenario is simulated using the QSim/Netsim. An event handler is set
* up that captures the only "departure" event and the only "arrival" event in
* the simulation (which is produced by the leg of the agent). The travel time
* can be computed an we get:
 *
*    netsimTravelTime = 303.0
 *
* Apparently, looking at QueueWithBuffer::moveQueueToBuffer , the agent needs
* to traverse the arrival link before he can arrive there. This leads to a
* travel time that is higher than expected by the router and so predictions
* done by the NetworkRoutingModule are inconsistent.
 *
* Not sure, what to do about that. Possible options:
* - Adjust Netsim such that agents arrive before they traverse the arrival link
* - Adjust the car routing such that the travel time of the final link is added
* - Adjust the car routing such that the routing goes to the end node of the arrival link
* - Explicitly document this behaviour somewhere
 *
* I guess usually this should not make such a big difference for MATSim,
* because the shortest path is found anyway. However, if one wants to predict
* travel times one should state that the NetworkRoutingModule has a bias by the
* arrival link.
 */
		@Test
	 void testRoutingVsSimulation() {
			Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			Network network = scenario.getNetwork();

			Node node1 = network.getFactory().createNode(Id.createNodeId("N1"), new Coord(0.0, 0.0));
			Node node2 = network.getFactory().createNode(Id.createNodeId("N2"), new Coord(0.0, 0.0));
			Node node3 = network.getFactory().createNode(Id.createNodeId("N3"), new Coord(0.0, 0.0));
			Node node4 = network.getFactory().createNode(Id.createNodeId("N4"), new Coord(0.0, 0.0));
			Node node5 = network.getFactory().createNode(Id.createNodeId("N5"), new Coord(0.0, 0.0));

			Link link12 = network.getFactory().createLink(Id.createLinkId("L12"), node1, node2);
			Link link23 = network.getFactory().createLink(Id.createLinkId("L23"), node2, node3);
			Link link34 = network.getFactory().createLink(Id.createLinkId("L34"), node3, node4);
			Link link45 = network.getFactory().createLink(Id.createLinkId("L45"), node4, node5);

			network.addNode(node1);
			network.addNode(node2);
			network.addNode(node3);
			network.addNode(node4);
			network.addNode(node5);

			network.addLink(link12);
			network.addLink(link23);
			network.addLink(link34);
			network.addLink(link45);

			Arrays.asList(link12, link23, link34, link45).forEach(l -> l.setAllowedModes(Collections.singleton(TransportMode.car)));
			Arrays.asList(link12, link23, link34, link45).forEach(l -> l.setLength(1000.0));
			Arrays.asList(link12, link23, link34, link45).forEach(l -> l.setFreespeed(10.0));

			Population population = scenario.getPopulation();

			Activity startActivity = population.getFactory().createActivityFromLinkId("A", Id.createLinkId("L12"));
			startActivity.setEndTime(0.0);
			Activity endActivity = population.getFactory().createActivityFromLinkId("A", Id.createLinkId("L45"));

			Person person = population.getFactory().createPerson(Id.createPersonId("P"));
			population.addPerson(person);

			Plan plan = population.getFactory().createPlan();
			person.addPlan(plan);

			Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(VehicleUtils.createVehicleId(person, TransportMode.car),
					VehicleUtils.getDefaultVehicleType());
			VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of(TransportMode.car, vehicle.getId()));
			scenario.getVehicles().addVehicleType(VehicleUtils.getDefaultVehicleType());
			scenario.getVehicles().addVehicle(vehicle);

			TravelTime travelTime = new FreeSpeedTravelTime();
			TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

			LeastCostPathCalculator router = new DijkstraFactory().createPathCalculator(network, travelDisutility,
					travelTime);
			NetworkRoutingModule routingModule = new NetworkRoutingModule(TransportMode.car, population.getFactory(), network,
					router);

			Leg leg = (Leg) routingModule
					.calcRoute(DefaultRoutingRequest.withoutAttributes(new LinkWrapperFacility(link12), new LinkWrapperFacility(link45), 0.0, person)).get(0);

			plan.addActivity(startActivity);
			plan.addLeg(leg);
			plan.addActivity(endActivity);

			EventsManager eventsManager = EventsUtils.createEventsManager();

			DepartureArrivalListener listener = new DepartureArrivalListener();
			eventsManager.addHandler(listener);

			new QSimBuilder(scenario.getConfig()) //
					.useDefaults() //
					.build(scenario, eventsManager) //
					.run();

			double netsimTravelTime = listener.arrivalTime - listener.departureTime;
			double routingTravelTime = leg.getTravelTime().seconds();

			// Travel times are rounded up in the Netsim, so we knowingly add an additional
			// +1s per link
			double adjustedRoutingTravelTime = routingTravelTime + 2.0;

			Assertions.assertEquals(netsimTravelTime, 303.0, 1e-3);
			Assertions.assertEquals(adjustedRoutingTravelTime, 202.0, 1e-3);
		}

		/*
* The same test as above, but here the full stack MATSim setup is used (i.e. the
* NetworkRoutingModule, etc. are created implicitly by the Controler).
 */
		@Test
	 void testRoutingVsSimulationFullStack() {
			Config config = ConfigUtils.createConfig();

			config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			config.controller().setLastIteration(0);

			ActivityParams activityParams = new ActivityParams("A");
			activityParams.setTypicalDuration(100.0);
			config.scoring().addActivityParams(activityParams);

			Scenario scenario = ScenarioUtils.createScenario(config);
			Network network = scenario.getNetwork();

			Node node1 = network.getFactory().createNode(Id.createNodeId("N1"), new Coord(0.0, 0.0));
			Node node2 = network.getFactory().createNode(Id.createNodeId("N2"), new Coord(0.0, 0.0));
			Node node3 = network.getFactory().createNode(Id.createNodeId("N3"), new Coord(0.0, 0.0));
			Node node4 = network.getFactory().createNode(Id.createNodeId("N4"), new Coord(0.0, 0.0));
			Node node5 = network.getFactory().createNode(Id.createNodeId("N5"), new Coord(0.0, 0.0));

			Link link12 = network.getFactory().createLink(Id.createLinkId("L12"), node1, node2);
			Link link23 = network.getFactory().createLink(Id.createLinkId("L23"), node2, node3);
			Link link34 = network.getFactory().createLink(Id.createLinkId("L34"), node3, node4);
			Link link45 = network.getFactory().createLink(Id.createLinkId("L45"), node4, node5);

			network.addNode(node1);
			network.addNode(node2);
			network.addNode(node3);
			network.addNode(node4);
			network.addNode(node5);

			network.addLink(link12);
			network.addLink(link23);
			network.addLink(link34);
			network.addLink(link45);

			Arrays.asList(link12, link23, link34, link45).forEach(l -> l.setAllowedModes(Collections.singleton(TransportMode.car)));
			Arrays.asList(link12, link23, link34, link45).forEach(l -> l.setLength(1000.0));
			Arrays.asList(link12, link23, link34, link45).forEach(l -> l.setFreespeed(10.0));

			Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(Id.createVehicleId("P"),
					VehicleUtils.getDefaultVehicleType());
			scenario.getVehicles().addVehicleType(VehicleUtils.getDefaultVehicleType());
			scenario.getVehicles().addVehicle(vehicle);

			Population population = scenario.getPopulation();

			Activity startActivity = population.getFactory().createActivityFromLinkId("A", Id.createLinkId("L12"));
			startActivity.setEndTime(0.0);
			Activity endActivity = population.getFactory().createActivityFromLinkId("A", Id.createLinkId("L45"));

			Person person = population.getFactory().createPerson(Id.createPersonId("P"));
			population.addPerson(person);

			Plan plan = population.getFactory().createPlan();
			person.addPlan(plan);

			plan.addActivity(startActivity);
			plan.addLeg(population.getFactory().createLeg(TransportMode.car));
			plan.addActivity(endActivity);

			DepartureArrivalListener listener = new DepartureArrivalListener();

			Controler controler = new Controler(scenario);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(listener);
				}
			});

			controler.run();

			double netsimTravelTime = listener.arrivalTime - listener.departureTime;
			double routingTravelTime = ((Leg)plan.getPlanElements().get(1)).getTravelTime().seconds();

			// Travel times are rounded up in the Netsim, so we knowingly add an additional
			// +1s per link
			double adjustedRoutingTravelTime = routingTravelTime + 2.0;

			Assertions.assertEquals(netsimTravelTime, 303.0, 1e-3);
			Assertions.assertEquals(adjustedRoutingTravelTime, 202.0, 1e-3);
		}

		static class DepartureArrivalListener implements PersonDepartureEventHandler, PersonArrivalEventHandler {
			public double departureTime = Double.NaN;
			public double arrivalTime = Double.NaN;

			@Override
			public void handleEvent(PersonArrivalEvent event) {
				this.arrivalTime = event.getTime();
			}

			@Override
			public void handleEvent(PersonDepartureEvent event) {
				this.departureTime = event.getTime();
			}
		}
	}
