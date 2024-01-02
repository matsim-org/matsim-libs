/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
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
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;


/**
 * This test is to show that qsim returns actualTravelTime+1 because of java rounding errors.
 * This is more predominant when flow capacity of link is not multiple of 3600 Veh/h.
 * In such scenarios, flowCapacityFraction is accumulated in every second and that's where
 * problem starts. For e.g. 0.2+0.1 = 0.30000000004 also 0.6+0.1=0.79999999999999999
 * See, nice article http://floating-point-gui.de/basic/
 *
 * See small numerical test also
 * @author amit
 */

public class JavaRoundingErrorInQsimTest {

	@Test
	void printDecimalSum(){
		double a = 0.1;
		double sum =0;
		double counter = 0;

		for(int i=0; i<10;i++){
			sum += a;
			counter++;
			System.out.println("Sum at counter "+counter+" is "+sum);
		}
	}

	@Test
	void testToCheckTravelTime() {
		// 2 cars depart on same time, central (bottleneck) link allow only 1 agent / 10 sec.
		PseudoInputs net = new PseudoInputs();
		net.createNetwork(360);
		net.createPopulation();

		Map<Id<Vehicle>, Double> vehicleLinkTravelTime = new HashMap<>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new VehicleLinkTravelTimeEventHandler(vehicleLinkTravelTime));

		PrepareForSimUtils.createDefaultPrepareForSim(net.scenario).run();
		new QSimBuilder(net.scenario.getConfig()) //
			.useDefaults() //
			.build(net.scenario, manager) //
			.run();

		//agent 2 is departed first so will have free speed time = 1000/25 +1 = 41 sec
		Assertions.assertEquals( 41.0 , vehicleLinkTravelTime.get(Id.createVehicleId(2))  , MatsimTestUtils.EPSILON, "Wrong travel time for on link 2 for vehicle 2");

		// agent 1 should have 1000/25 +1 + 10 = 51 but, it may be 52 sec sometimes due to rounding errors in java. Rounding errors is eliminated at the moment if accumulating flow to zero instead of one.
		Assertions.assertEquals( 51.0 , vehicleLinkTravelTime.get(Id.createVehicleId(1))  , MatsimTestUtils.EPSILON, "Wrong travel time for on link 2 for vehicle 1");
		LogManager.getLogger(JavaRoundingErrorInQsimTest.class).warn("Although the test is passing instead of failing for vehicle 1. This is done intentionally in order to keep this in mind for future.");
	}

	private static class VehicleLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Double> vehicleTravelTime;

		public VehicleLinkTravelTimeEventHandler(Map<Id<Vehicle>, Double> vehicleLinkTravelTime) {
			this.vehicleTravelTime = vehicleLinkTravelTime;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			if( event.getLinkId().equals(Id.createLinkId(2))){
				vehicleTravelTime.put(event.getVehicleId(), - event.getTime());
			}

		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			if( event.getLinkId().equals(Id.createLinkId(2)) ){
				vehicleTravelTime.put(event.getVehicleId(), vehicleTravelTime.get(event.getVehicleId()) + event.getTime());
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}

	private static final class PseudoInputs{

		final Scenario scenario ;
		Network network;
		final Population population;
		Link link1;
		Link link2;
		Link link3;

		public PseudoInputs(){
			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			scenario.getConfig().qsim().setUsingFastCapacityUpdate(true);
			population = scenario.getPopulation();
		}

		private void createNetwork(double linkCapacity){

			network = (Network) scenario.getNetwork();

			double x = -100.0;
			Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(x, 0.0));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(0.0, 0.0));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(0.0, 1000.0));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(0.0, 1100.0));
			final Node fromNode = node1;
			final Node toNode = node2;

			link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 1000, (double) 25, (double) 7200, (double) 1, null, "22");
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			final double capacity = linkCapacity;
			link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 1000, (double) 25, capacity, (double) 1, null, "22");
			final Node fromNode2 = node3;
			final Node toNode2 = node4;
			link3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, (double) 1000, (double) 25, (double) 7200, (double) 1, null, "22");

		}

		private void createPopulation(){

			for(int i=1;i<3;i++){
				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);
				Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());

				a1.setEndTime(0*3600);
				Leg leg = population.getFactory().createLeg(TransportMode.car);
				plan.addActivity(a1);
				plan.addLeg(leg);
				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
				NetworkRoute route;
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				route= (NetworkRoute) factory.createRoute(link1.getId(), link3.getId());
				linkIds.add(link2.getId());
				route.setLinkIds(link1.getId(), linkIds, link3.getId());
				leg.setRoute(route);

				Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
				plan.addActivity(a2);
				population.addPerson(p);
			}
		}
	}
}
