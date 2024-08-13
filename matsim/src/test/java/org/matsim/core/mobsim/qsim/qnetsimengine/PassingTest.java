/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) ${year} by the members listed in the COPYING,        *
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

import java.util.*;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;


/**
 * Tests that a faster vehicle can pass slower vehicle on the same link
 */
public class PassingTest {

	@RegisterExtension private MatsimTestUtils helper = new MatsimTestUtils();

	/**
	 * A bike enters at t=0; and a car at t=5sec link length = 1000m
	 * Assume car speed = 20 m/s, bike speed = 5 m/s
	 * tt_car = 50 sec; tt_bike = 200 sec
	 */
	@Test
	void test4PassingInFreeFlowState(){

		SimpleNetwork net = new SimpleNetwork();

		//=== build plans; two persons; one with car and another with bike; car leave 5 secs after bike
		String transportModes [] = new String [] {"bike","car"};

		for(int i=0;i<2;i++){
			Id<Person> id = Id.create(i, Person.class);
			Person p = net.population.getFactory().createPerson(id);
			Plan plan = net.population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = net.population.getFactory().createActivityFromLinkId("h", net.link1.getId());
			a1.setEndTime(8*3600+i*5);
			Leg leg = net.population.getFactory().createLeg(transportModes[i]);
			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link3.getId());
			route.setLinkIds(net.link1.getId(), Arrays.asList(net.link2.getId()), net.link3.getId());
			leg.setRoute(route);

			Activity a2 = net.population.getFactory().createActivityFromLinkId("w", net.link3.getId());
			plan.addActivity(a2);
			net.population.addPerson(p);
		}

		VehicleLinkTravelTimeEventHandler handler = new VehicleLinkTravelTimeEventHandler();
		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(handler);

		storeVehicleTypeInfo(net);

		PrepareForSimUtils.createDefaultPrepareForSim(net.scenario).run();

		new QSimBuilder(net.scenario.getConfig()) //
			.useDefaults() //
			.build(net.scenario, manager) //
			.run();

		Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkTravelTimes =  handler.getVehicleId2LinkTravelTime();

		Map<Id<Link>, Double> travelTime1 = vehicleLinkTravelTimes.get(Id.create("0_bike", Vehicle.class));
		Map<Id<Link>, Double> travelTime2 = vehicleLinkTravelTimes.get(Id.create("1", Vehicle.class));

		int bikeTravelTime = travelTime1.get(Id.create("2", Link.class)).intValue();
		int carTravelTime = travelTime2.get(Id.create("2", Link.class)).intValue();

		Assertions.assertEquals(51, carTravelTime, "Wrong car travel time");
		Assertions.assertEquals(201, bikeTravelTime, "Wrong bike travel time");
		Assertions.assertEquals(150, bikeTravelTime-carTravelTime, "Passing is not implemented");

	}

	private static class TravelTimeControlerListener implements StartupListener, IterationEndsListener {

		Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkTravelTimes = new HashMap<>();
		VehicleLinkTravelTimeEventHandler hand;
		@Inject EventsManager eventsManager;

		@Override
		public void notifyStartup(StartupEvent event) {

			hand = new VehicleLinkTravelTimeEventHandler();
			eventsManager.addHandler(hand);
		}

		public Map<Id<Vehicle>, Map<Id<Link>, Double>> getVehicleId2Time(){
			return this.vehicleLinkTravelTimes;
		}

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			this.vehicleLinkTravelTimes = this.hand.getVehicleId2LinkTravelTime();
		}
	}

	private void storeVehicleTypeInfo(SimpleNetwork net) {
		net.scenario.getConfig().qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.modeVehicleTypesFromVehiclesData);

		Vehicles vehicles = net.scenario.getVehicles();

		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car", VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);
		vehicles.addVehicleType(car);

		VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike", VehicleType.class));
		bike.setMaximumVelocity(5);
		bike.setPcuEquivalents(0.25);
		vehicles.addVehicleType(bike);
	}


	private static final class SimpleNetwork{

		final Config config;
		final Scenario scenario ;
		final Network network;
		final Population population;
		final Link link1;
		final Link link2;
		final Link link3;

		public SimpleNetwork(){

			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setFlowCapFactor(1.0);
			config.qsim().setStorageCapFactor(1.0);
			config.qsim().setMainModes(Arrays.asList("car","bike"));
			config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
			config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

			network = (Network) scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			double x = -100.0;
			Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(x, 0.0));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(0.0, 0.0));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(0.0, 1000.0));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(0.0, 1100.0));

			Set<String> allowedModes = new HashSet<String>();
			allowedModes.addAll(Arrays.asList("pt","motorbike"));
			final Node fromNode = node1;
			final Node toNode = node2;

			link1 = NetworkUtils.createAndAddLink(network,Id.create("1", Link.class), fromNode, toNode, (double) 100, (double) 25, (double) 60, (double) 1, null, "22");
			final Node fromNode1 = node2;
			final Node toNode1 = node3; //capacity is 1 PCU per min.
			link2 = NetworkUtils.createAndAddLink(network,Id.create("2", Link.class), fromNode1, toNode1, (double) 1000, (double) 25, (double) 60, (double) 1, null, "22");
			final Node fromNode2 = node3;
			final Node toNode2 = node4;
			link3 = NetworkUtils.createAndAddLink(network,Id.create("3", Link.class), fromNode2, toNode2, (double) 100, (double) 25, (double) 60, (double) 1, null, "22");

			link1.setAllowedModes(allowedModes);
			link2.setAllowedModes(allowedModes);
			link3.setAllowedModes(allowedModes);


			population = scenario.getPopulation();
		}
	}
	private static class VehicleLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkTravelTimes;

		public VehicleLinkTravelTimeEventHandler() {
			this.vehicleLinkTravelTimes = new HashMap<>();
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleLinkTravelTimes.get(event.getVehicleId());
			if (travelTimes == null) {
				travelTimes = new HashMap<Id<Link>, Double>();
				this.vehicleLinkTravelTimes.put(event.getVehicleId(), travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleLinkTravelTimes.get(event.getVehicleId());
			if (travelTimes != null) {
				Double d = travelTimes.get(event.getLinkId());
				if (d != null) {
					double time = event.getTime() - d.doubleValue();
					travelTimes.put(event.getLinkId(), Double.valueOf(time));
				}
			}
		}

		@Override
		public void reset(int iteration) {
		}

		public Map<Id<Vehicle>, Map<Id<Link>, Double>> getVehicleId2LinkTravelTime(){
			return this.vehicleLinkTravelTimes;
		}
	}
}
