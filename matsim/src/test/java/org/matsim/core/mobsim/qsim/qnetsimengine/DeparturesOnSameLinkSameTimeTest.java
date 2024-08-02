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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
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
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import java.util.*;

/**
 * @author amit
 */

public class DeparturesOnSameLinkSameTimeTest {

	/**
	 * Two cases where two motorbikes or two cars depart at the same time on link l_1.
	 * <p> Since, the flow capacity of link l_1 is 1 PCU per sec, both motorbike should be able to leave the link l_1 at the same time
	 * whereas cars should leave at a gap of one second.
	 */
	@Test
	void test4LinkEnterTimeOfCarAndBike() {

		Id<Vehicle> firstAgent = Id.createVehicleId(1);
		Id<Vehicle> secondAgent = Id.createVehicleId(2);

		Id<Link> departureLink = Id.createLinkId(1);

		Map<Id<Vehicle>,Map<Id<Link>, Double>> motorbikeLinkLeaveTime = getLinkEnterTime("motorbike",3600);
		Map<Id<Vehicle>,Map<Id<Link>, Double>> carLinkLeaveTime = getLinkEnterTime(TransportMode.car,3600);

		double diff_carAgents_departureLink_LeaveTimes = carLinkLeaveTime.get(secondAgent).get(departureLink) - carLinkLeaveTime.get(firstAgent).get(departureLink);
		Assertions.assertEquals(1., Math.abs(diff_carAgents_departureLink_LeaveTimes), MatsimTestUtils.EPSILON, "Both car agents should leave at the gap of 1 sec." );

		double diff_motorbikeAgents_departureLink_LeaveTimes = motorbikeLinkLeaveTime.get(secondAgent).get(departureLink) - motorbikeLinkLeaveTime.get(firstAgent).get(departureLink);
		Assertions.assertEquals(0., diff_motorbikeAgents_departureLink_LeaveTimes, MatsimTestUtils.EPSILON, "Both motorbike agents should leave at the same time." );

		// for flow cap more than 3600, both cars also should leave link l_1 at the same time.
		carLinkLeaveTime = getLinkEnterTime(TransportMode.car,3601);

		diff_carAgents_departureLink_LeaveTimes = carLinkLeaveTime.get(secondAgent).get(departureLink) - carLinkLeaveTime.get(firstAgent).get(departureLink);
		Assertions.assertEquals(0., diff_carAgents_departureLink_LeaveTimes, MatsimTestUtils.EPSILON, "Both car agents should leave at the same time" );
	}

	private Map<Id<Vehicle>,Map<Id<Link>, Double>> getLinkEnterTime (String travelMode, double departureLinkCapacity){

		PseudoInputs inputs = new PseudoInputs(travelMode);
		inputs.createNetwork(departureLinkCapacity);
		inputs.createPopulation();

		final Map<Id<Vehicle>,Map<Id<Link>, Double>> linkLeaveTimes = new HashMap<>() ;

		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {
				linkLeaveTimes.clear();
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {

				if(linkLeaveTimes.containsKey(event.getVehicleId())){

					Map<Id<Link>, Double> times = linkLeaveTimes.get(event.getVehicleId());
					times.put(event.getLinkId(), event.getTime());
					linkLeaveTimes.put(event.getVehicleId(), times);

				} else {

					Map<Id<Link>, Double> times = new HashMap<Id<Link>, Double>();
					times.put(event.getLinkId(), event.getTime());
					linkLeaveTimes.put(event.getVehicleId(), times);

				}
			}
		});

		PrepareForSimUtils.createDefaultPrepareForSim(inputs.scenario).run();
		new QSimBuilder(inputs.scenario.getConfig()) //
			.useDefaults() //
			.build(inputs.scenario, events) //
			.run();

		return linkLeaveTimes;
	}

	/**
	 * Corridor link
	 * <p> o-----o------o
	 *
	 */
	private static class PseudoInputs {

		Scenario scenario;
		Config config;
		Network network;
		Population population;
		Link link1;
		Link link2;
		private String travelMode;

		public PseudoInputs(String travelMode) {
			this.travelMode = travelMode;
			config=ConfigUtils.createConfig();
			this.scenario = ScenarioUtils.loadScenario(config);
			config.qsim().setMainModes(Arrays.asList(travelMode));
			config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

			//following is necessary for mixed traffic, providing a route was obstructing
			// the requirement of these which might be all right in some cases. Amit Jan'18
			config.routing().setNetworkModes(Arrays.asList(travelMode));
			config.travelTimeCalculator().setAnalyzedModesAsString(travelMode );
			config.travelTimeCalculator().setSeparateModes(true);
			config.scoring().getOrCreateModeParams(travelMode);

			network = this.scenario.getNetwork();
			population = this.scenario.getPopulation();
		}

		private void createNetwork(double departureLinkCapacity){

			Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord(0, 0));
			Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord(100, 10));
			double y = -10;
			Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord(300, y));
			final Node fromNode = node1;
			final Node toNode = node2;
			final double capacity = departureLinkCapacity;

			link1 = NetworkUtils.createAndAddLink(network, Id.createLinkId("1"), fromNode, toNode, 1000.0, 20.0, capacity, 1, null, "7");
			final Node fromNode1 = node2;
			final Node toNode1 = node3;
			link2 = NetworkUtils.createAndAddLink(network, Id.createLinkId("2"), fromNode1, toNode1, 1000.0, 20.0, 3600, 1, null, "7");
		}

		private void createPopulation(){

			// Vehicles info
//			scenario.getConfig().qsim().setUseDefaultVehicles(false);
			scenario.getConfig().qsim().setVehiclesSource( VehiclesSource.fromVehiclesData ) ;
			scenario.getConfig().qsim().setUsingFastCapacityUpdate(true);

			VehicleType vt = VehicleUtils.getFactory().createVehicleType(Id.create(travelMode, VehicleType.class));
			vt.setMaximumVelocity(20);
			vt.setPcuEquivalents(travelMode == "motorbike" ? 0.25 : 1.0);
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
				route = (NetworkRoute) factory.createRoute(link1.getId(), link2.getId());
				linkIds.add(link2.getId());
				route.setLinkIds(link1.getId(), linkIds, link2.getId());
				leg.setRoute(route);

				Activity a2 = population.getFactory().createActivityFromLinkId("w", link2.getId());
				plan.addActivity(a2);
				population.addPerson(p);

				Id<Vehicle> vehId = Id.createVehicleId(i);
				VehicleUtils.insertVehicleIdsIntoAttributes(p, Map.of(travelMode, vehId));
				Vehicle veh = VehicleUtils.getFactory().createVehicle(vehId, vt);
				scenario.getVehicles().addVehicle(veh);
			}
		}
	}
}
