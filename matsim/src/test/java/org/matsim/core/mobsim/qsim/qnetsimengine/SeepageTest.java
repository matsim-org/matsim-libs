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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

/**
 * Tests that in congested part, walk (seep mode) can overtake (seep) car mode.
 *
 */
public class SeepageTest {
	static private final Logger log = LogManager.getLogger( SeepageTest.class);

	/**
	 *  Two carAgents end act at time 948 and 949 sec and walkAgent ends act at 49 sec.
	 *  Link length is 1 km and flow capacity 1 PCU/min. Speed of car and walk is 20 mps and 1 mps.
	 *  First car leave link at 1000 sec thus blocking it for next 59 sec. Second car is in queue at 1000 sec.
	 *  WalkAgent joins queue at 1050 sec but leave link before second car at 1060 sec and thus blocking link for another 6 sec(flowCap*PCU)
	 *  Thus, second car leaves link after walkAgent.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void seepageOfWalkInCongestedRegime(boolean isUsingFastCapacityUpdate){

		SimpleNetwork net = new SimpleNetwork();

		Scenario sc = net.scenario;
		sc.getConfig().qsim().setVehiclesSource(VehiclesSource.fromVehiclesData);

		sc.getConfig().qsim().setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);

		Map<String, VehicleType> modesType = new HashMap<String, VehicleType>();
		VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.car,VehicleType.class));
		car.setMaximumVelocity(20);
		car.setPcuEquivalents(1.0);
		modesType.put(TransportMode.car, car);
		sc.getVehicles().addVehicleType(car);

		VehicleType walk = VehicleUtils.getFactory().createVehicleType(Id.create(TransportMode.walk,VehicleType.class));
		walk.setMaximumVelocity(1);
		walk.setPcuEquivalents(0.1);
		modesType.put(TransportMode.walk, walk);
		sc.getVehicles().addVehicleType(walk);

		for (int i=0;i<3;i++){
			Id<Person> id = Id.createPersonId(i);
			Person p = net.population.getFactory().createPerson(id);
			Plan plan = net.population.getFactory().createPlan();
			p.addPlan(plan);
			Activity a1 = net.population.getFactory().createActivityFromLinkId("h", net.link1.getId());
			Leg leg;
			if(i!=2){
				a1.setEndTime(948+i);
				leg = net.population.getFactory().createLeg(TransportMode.car);
			} else {
				a1.setEndTime(49);
				leg = net.population.getFactory().createLeg(TransportMode.walk);
			}

			plan.addActivity(a1);
			plan.addLeg(leg);
			LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
			NetworkRoute route = (NetworkRoute) factory.createRoute(net.link1.getId(), net.link3.getId());
			route.setLinkIds(net.link1.getId(), Arrays.asList(net.link2.getId()), net.link3.getId());
			leg.setRoute(route);
			Activity a2 = net.population.getFactory().createActivityFromLinkId("w", net.link3.getId());
			plan.addActivity(a2);
			net.population.addPerson(p);

			Id<Vehicle> vehicleId = Id.create(p.getId(), Vehicle.class);
			VehicleUtils.insertVehicleIdsIntoAttributes(p, Map.of(leg.getMode(), vehicleId));
			Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, modesType.get(leg.getMode()));
			sc.getVehicles().addVehicle(vehicle);
		}

		Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkTravelTimes = new HashMap<>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new VehicleLinkTravelTimeEventHandler(vehicleLinkTravelTimes));

		PrepareForSimUtils.createDefaultPrepareForSim(sc).run();

		new QSimBuilder(sc.getConfig()) //
				.useDefaults() //
				.build(sc, manager) //
				.run();

		Map<Id<Link>, Double> travelTime1 = vehicleLinkTravelTimes.get(Id.createVehicleId("2"));
		Map<Id<Link>, Double> travelTime2 = vehicleLinkTravelTimes.get(Id.createVehicleId("1"));

		int walkTravelTime = travelTime1.get(Id.createLinkId("2")).intValue();
		int carTravelTime = travelTime2.get(Id.createLinkId("2")).intValue();

//		if(this.isUsingFastCapacityUpdate) {
			Assertions.assertEquals(115, carTravelTime, "Wrong car travel time");
			Assertions.assertEquals(1009, walkTravelTime, "Wrong walk travel time.");
			Assertions.assertEquals(894, walkTravelTime-carTravelTime, "Seepage is not implemented");
//		} else {
//			Assert.assertEquals("Wrong car travel time", 116, carTravelTime);
//			Assert.assertEquals("Wrong walk travel time.", 1010, walkTravelTime);
//			Assert.assertEquals("Seepage is not implemented", 894, walkTravelTime-carTravelTime);
//		}
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
			config.qsim().setMainModes(Arrays.asList(TransportMode.car,TransportMode.walk));
			config.qsim().setLinkDynamics(LinkDynamics.SeepageQ);
			config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

			config.qsim().setSeepModes(Arrays.asList(TransportMode.walk) );
			config.qsim().setSeepModeStorageFree(false);
			config.qsim().setRestrictingSeepage(true);

			network = scenario.getNetwork();
			this.network.setCapacityPeriod(Time.parseTime("1:00:00"));
			double x = -100.0;
			Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord(x, 0.0));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord(0.0, 0.0));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord(0.0, 1000.0));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord(0.0, 1100.0));

			Set<String> allowedModes = new HashSet<String>(); allowedModes.addAll(Arrays.asList(TransportMode.car,TransportMode.walk));
			final Node fromNode = node1;
			final Node toNode = node2;

			link1 = NetworkUtils.createAndAddLink(network, Id.createLinkId("1"), fromNode, toNode, 100, 25, 36000, 1, null, "22");
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			link2 = NetworkUtils.createAndAddLink(network, Id.createLinkId("2"), fromNode1, toNode1, 1000, 25, 60, 1, null, "22");
			final Node fromNode2 = node3;
			final Node toNode2 = node4;	//flow capacity is 1 PCU per min.
			link3 = NetworkUtils.createAndAddLink(network, Id.createLinkId("3"), fromNode2, toNode2, 100, 25, 36000, 1, null, "22");

			for(Link l :network.getLinks().values()){
				l.setAllowedModes(allowedModes);
			}

			population = scenario.getPopulation();
		}
	}
	private static class VehicleLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Map<Id<Link>, Double>> vehicleLinkTravelTimes;

		public VehicleLinkTravelTimeEventHandler(final Map<Id<Vehicle>, Map<Id<Link>, Double>> agentTravelTimes) {
			this.vehicleLinkTravelTimes = agentTravelTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Map<Id<Link>, Double> travelTimes = this.vehicleLinkTravelTimes.get(event.getVehicleId());
			if (travelTimes == null) {
				travelTimes = new HashMap<>();
				this.vehicleLinkTravelTimes.put(event.getVehicleId(), travelTimes);
			}
			travelTimes.put(event.getLinkId(), Double.valueOf(event.getTime()));
			if ( event.getLinkId().equals( Id.createLinkId("2") ) ) {
				log.info( event );
			}
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
			if ( event.getLinkId().equals( Id.createLinkId("2") ) ) {
				log.info( event );
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}
}
