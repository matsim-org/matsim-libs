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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

/**
 * Tests that two persons can leave a link at the same time if flow capacity permits
 * In other words, test if qsim can handle capacity more than 3600 PCU/Hr.
 * If the flow capacity is 3601 PCU/Hr it will allow the two vehicles.
 *
 * @author amit
 */
public class FlowCapacityVariationTest {

	@Test
	void twoCarsLeavingTimes() {
		vehiclesLeavingSameTime(TransportMode.car,3601);
	}

	@Test
	void twoMotorbikesTravelTime(){
		/* linkCapacity higher than 1PCU/sec*/
		vehiclesLeavingSameTime("motorbike",3601);

		/*link capacuty higher than 1motorbike/sec = 0.25PCU/sec */
		vehiclesLeavingSameTime("motorbike",1800);
	}

	@Test
	void twoBikesTravelTime(){
		/* linkCapacity higher than 1PCU/sec */
		vehiclesLeavingSameTime(TransportMode.bike,3601);

		/* link capacuty higher than 1motorbike/sec = 0.25PCU/sec */
		vehiclesLeavingSameTime(TransportMode.bike,1800);
	}

	private void vehiclesLeavingSameTime(String travelMode, double linkCapacity){
		PseudoInputs net = new PseudoInputs(travelMode);
		net.createNetwork(linkCapacity);
		net.createPopulation();

		Map<Id<Vehicle>, Map<Id<Link>, double[]>> vehicleLinkTravelTimes = new HashMap<>();

		EventsManager manager = EventsUtils.createEventsManager();
		manager.addHandler(new VehicleLinkTravelTimeEventHandler(vehicleLinkTravelTimes));

		PrepareForSimUtils.createDefaultPrepareForSim(net.scenario).run();
		new QSimBuilder(net.scenario.getConfig()) //
			.useDefaults() //
			.build(net.scenario, manager) //
			.run();

		Map<Id<Link>, double[]> times1 = vehicleLinkTravelTimes.get(Id.create("1", Vehicle.class));
		Map<Id<Link>, double[]> times2 = vehicleLinkTravelTimes.get(Id.create("2", Vehicle.class));

		int linkEnterTime1 = (int)times1.get(Id.create("2", Link.class))[0];
		int linkEnterTime2 = (int)times2.get(Id.create("2", Link.class))[0];

		int linkLeaveTime1 = (int)times1.get(Id.create("2", Link.class))[1];
		int linkLeaveTime2 = (int)times2.get(Id.create("2", Link.class))[1];

		Assertions.assertEquals(0, linkEnterTime1-linkEnterTime2, travelMode+ " entered at different time");
		Assertions.assertEquals(0, linkLeaveTime1-linkLeaveTime2, travelMode +" entered at same time but not leaving the link at the same time.");
	}

	private static final class PseudoInputs{

		final Config config;
		final Scenario scenario ;
		Network network;
		final Population population;
		Link link1;
		Link link2;
		Link link3;
		private String travelMode;

		public PseudoInputs(String travelMode){

			this.travelMode = travelMode;

			scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
			config = scenario.getConfig();
			config.qsim().setMainModes(Arrays.asList(travelMode));
			config.qsim().setUsingFastCapacityUpdate(true);
			config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
			population = scenario.getPopulation();
		}

		private void createNetwork(double linkCapacity){

			network = scenario.getNetwork();

			double x = -100.0;
			Node node1 = NetworkUtils.createAndAddNode(network, Id.create("1", Node.class), new Coord(x, 0.0));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.create("2", Node.class), new Coord(0.0, 0.0));
			Node node3 = NetworkUtils.createAndAddNode(network, Id.create("3", Node.class), new Coord(0.0, 1000.0));
			Node node4 = NetworkUtils.createAndAddNode(network, Id.create("4", Node.class), new Coord(0.0, 1100.0));
			final Node fromNode = node1;
			final Node toNode = node2;

			link1 = NetworkUtils.createAndAddLink(network, Id.create("1", Link.class), fromNode, toNode, 1000, 25, 7200, 1, null, "22");
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			final double capacity = linkCapacity;
			link2 = NetworkUtils.createAndAddLink(network, Id.create("2", Link.class), fromNode1, toNode1, 1000, 25, capacity, 1, null, "22");
			final Node fromNode2 = node3;
			final Node toNode2 = node4;
			link3 = NetworkUtils.createAndAddLink(network, Id.create("3", Link.class), fromNode2, toNode2, 1000, 25, 7200, 1, null, "22");

		}

		private void createPopulation(){

			// Vehicles info
//			scenario.getConfig().qsim().setUseDefaultVehicles(false);
			scenario.getConfig().qsim().setVehiclesSource( VehiclesSource.fromVehiclesData ) ;

			VehicleType vt = VehicleUtils.getFactory().createVehicleType(Id.create(travelMode, VehicleType.class));
			vt.setMaximumVelocity(travelMode == "bike" ? 5.0 : 20.0 );
			vt.setPcuEquivalents(travelMode == "car" ? 1.0 : 0.25);
			scenario.getVehicles().addVehicleType(vt);

			for(int i=1;i<3;i++){
				Id<Person> id = Id.createPersonId(i);
				Person p = population.getFactory().createPerson(id);
				Plan plan = population.getFactory().createPlan();
				p.addPlan(plan);
				Activity a1 = population.getFactory().createActivityFromLinkId("h", link1.getId());

				a1.setEndTime(0 * 3600);
				Leg leg = population.getFactory().createLeg(travelMode);
				plan.addActivity(a1);
				plan.addLeg(leg);
				LinkNetworkRouteFactory factory = new LinkNetworkRouteFactory();
				NetworkRoute route;
				List<Id<Link>> linkIds = new ArrayList<Id<Link>>();
				route = (NetworkRoute) factory.createRoute(link1.getId(), link3.getId());
				linkIds.add(link2.getId());
				route.setLinkIds(link1.getId(), linkIds, link3.getId());
				leg.setRoute(route);

				Activity a2 = population.getFactory().createActivityFromLinkId("w", link3.getId());
				plan.addActivity(a2);
				population.addPerson(p);

				Id<Vehicle> vehId = Id.create(i, Vehicle.class);
				VehicleUtils.insertVehicleIdsIntoAttributes(p, Map.of(travelMode, vehId));
				Vehicle veh = VehicleUtils.getFactory().createVehicle(vehId, vt);
				scenario.getVehicles().addVehicle(veh);
			}
		}

	}

	private static class VehicleLinkTravelTimeEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler {

		private final Map<Id<Vehicle>, Map<Id<Link>, double[]>> vehicleLinkEnterLeaveTimes;

		public VehicleLinkTravelTimeEventHandler(Map<Id<Vehicle>, Map<Id<Link>, double[]>> agentLinkEnterLeaveTimes) {
			this.vehicleLinkEnterLeaveTimes = agentLinkEnterLeaveTimes;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			LogManager.getLogger(VehicleLinkTravelTimeEventHandler.class).info(event.toString());
			Map<Id<Link>, double[]> times = this.vehicleLinkEnterLeaveTimes.get(event.getVehicleId());
			if (times == null) {
				times = new HashMap<>();
				double [] linkEnterLeaveTime = {Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
				times.put(event.getLinkId(), linkEnterLeaveTime);
				this.vehicleLinkEnterLeaveTimes.put(event.getVehicleId(), times);
			}
			double linkLeaveTime;
			if(times.get(event.getLinkId())!=null){
				linkLeaveTime = times.get(event.getLinkId())[1];
			} else linkLeaveTime = Double.POSITIVE_INFINITY;

			double [] linkEnterTime = {event.getTime(),linkLeaveTime};
			times.put(event.getLinkId(), linkEnterTime);
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			LogManager.getLogger(VehicleLinkTravelTimeEventHandler.class).info(event.toString());
			Map<Id<Link>, double[]> times = this.vehicleLinkEnterLeaveTimes.get(event.getVehicleId());
			if (times != null) {
				double linkEnterTime = times.get(event.getLinkId())[0];
				double [] linkEnterLeaveTime = {linkEnterTime,event.getTime()};
				times.put(event.getLinkId(), linkEnterLeaveTime);
			}
		}

		@Override
		public void reset(int iteration) {
		}
	}
}
