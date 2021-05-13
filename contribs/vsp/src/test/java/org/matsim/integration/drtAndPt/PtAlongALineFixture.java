/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.integration.drtAndPt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.TransitScheduleValidator;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehiclesFactory;

/**
 * @author Michal Maciejewski (michalm)
 */
class PtAlongALineFixture {

	private final Id<Link> TR_LINK_m1_0_ID = Id.createLinkId("trLinkm1-0");
	private final Id<Link> TR_LINK_0_1_ID = Id.createLinkId("trLink0-1");
	private final Id<Link> TR_LONG_LINK_LEFT_ID = Id.createLinkId("trLinkLongLeft");
	private final Id<Link> TR_LINK_MIDDLE_ID = Id.createLinkId("trLinkMiddle");
	private final Id<Link> TR_LONG_LINK_RIGHT_ID = Id.createLinkId("trLinkLongRight");
	private final Id<Link> TR_LINK_LASTM1_LAST_ID = Id.createLinkId("trLinkLastm1-Last");
	private final Id<Link> TR_LINK_LAST_LASTp1_ID = Id.createLinkId("trLinkLast-Lastp1");

	private final Id<TransitStopFacility> tr_stop_fac_0_ID = Id.create("StopFac0", TransitStopFacility.class);
	private final Id<TransitStopFacility> tr_stop_fac_10000_ID = Id.create("StopFac10000", TransitStopFacility.class);
	private final Id<TransitStopFacility> tr_stop_fac_5000_ID = Id.create("StopFac5000", TransitStopFacility.class);

	private final Id<VehicleType> busTypeID = Id.create("bus", VehicleType.class);

	Scenario createScenario(Config config, long numberOfPersons) {
		Scenario scenario = ScenarioUtils.createScenario(config);
		// don't load anything

		final int lastNodeIdx = 1000;
		final double deltaX = 100.;

		createAndAddCarNetwork(scenario, lastNodeIdx, deltaX);

		createAndAddPopulation(scenario, "pt", numberOfPersons);

		final double deltaY = 1000.;

		createAndAddTransitNetwork(scenario, lastNodeIdx, deltaX, deltaY);

		createAndAddTransitStopFacilities(scenario, lastNodeIdx, deltaX, deltaY);

		createAndAddTransitVehicleType(scenario);

		createAndAddTransitLine(scenario);

		TransitScheduleValidator.printResult(
				TransitScheduleValidator.validateAll(scenario.getTransitSchedule(), scenario.getNetwork()));
		return scenario;
	}

	void createAndAddTransitNetwork(Scenario scenario, int lastNodeIdx, double deltaX, double deltaY) {
		NetworkFactory nf = scenario.getNetwork().getFactory();

		Node nodem1 = nf.createNode(Id.createNodeId("trNodeM1"), new Coord(-100, deltaY));
		scenario.getNetwork().addNode(nodem1);
		// ---
		Node node0 = nf.createNode(Id.createNodeId("trNode0"), new Coord(0, deltaY));
		scenario.getNetwork().addNode(node0);
		createAndAddTransitLink(scenario, nodem1, node0, TR_LINK_m1_0_ID);
		// ---
		Node node1 = nf.createNode(Id.createNodeId("trNode1"), new Coord(deltaX, deltaY));
		scenario.getNetwork().addNode(node1);
		createAndAddTransitLink(scenario, node0, node1, TR_LINK_0_1_ID);
		// ---
		Node nodeMiddleLeft = nf.createNode(Id.createNodeId("trNodeMiddleLeft"),
				new Coord(0.5 * (lastNodeIdx - 1) * deltaX, deltaY));
		scenario.getNetwork().addNode(nodeMiddleLeft);
		{
			createAndAddTransitLink(scenario, node1, nodeMiddleLeft, TR_LONG_LINK_LEFT_ID);
		}
		// ---
		Node nodeMiddleRight = nf.createNode(Id.createNodeId("trNodeMiddleRight"),
				new Coord(0.5 * (lastNodeIdx + 1) * deltaX, deltaY));
		scenario.getNetwork().addNode(nodeMiddleRight);
		createAndAddTransitLink(scenario, nodeMiddleLeft, nodeMiddleRight, TR_LINK_MIDDLE_ID);
		// ---
		Node nodeLastm1 = nf.createNode(Id.createNodeId("trNodeLastm1"), new Coord((lastNodeIdx - 1) * deltaX, deltaY));
		scenario.getNetwork().addNode(nodeLastm1);
		createAndAddTransitLink(scenario, nodeMiddleRight, nodeLastm1, TR_LONG_LINK_RIGHT_ID);

		// ---
		Node nodeLast = nf.createNode(Id.createNodeId("trNodeLast"), new Coord(lastNodeIdx * deltaX, deltaY));
		scenario.getNetwork().addNode(nodeLast);
		createAndAddTransitLink(scenario, nodeLastm1, nodeLast, TR_LINK_LASTM1_LAST_ID);
		// ---
		Node nodeLastp1 = nf.createNode(Id.createNodeId("trNodeLastp1"),
				new Coord(lastNodeIdx * deltaX + 100., deltaY));
		scenario.getNetwork().addNode(nodeLastp1);
		createAndAddTransitLink(scenario, nodeLast, nodeLastp1, TR_LINK_LAST_LASTp1_ID);
	}

	void createAndAddTransitStopFacilities(Scenario scenario, int lastNodeIdx, double deltaX, double deltaY) {
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory tsf = schedule.getFactory();

		TransitStopFacility stopFacility0 = tsf.createTransitStopFacility(tr_stop_fac_0_ID, new Coord(deltaX, deltaY),
				false);
		stopFacility0.setLinkId(TR_LINK_0_1_ID);
		schedule.addStopFacility(stopFacility0);

		TransitStopFacility stopFacility5000 = tsf.createTransitStopFacility(tr_stop_fac_5000_ID,
				new Coord(0.5 * (lastNodeIdx - 1) * deltaX, deltaY), false);
		stopFacility5000.setLinkId(TR_LINK_MIDDLE_ID);
		stopFacility5000.getAttributes().putAttribute("drtAccessible", "true");
		stopFacility5000.getAttributes().putAttribute("bikeAccessible", "true");

		schedule.addStopFacility(stopFacility5000);

		TransitStopFacility stopFacility10000 = tsf.createTransitStopFacility(tr_stop_fac_10000_ID,
				new Coord((lastNodeIdx - 1) * deltaX, deltaY), false);
		stopFacility10000.setLinkId(TR_LINK_LASTM1_LAST_ID);
		schedule.addStopFacility(stopFacility10000);
	}

	void createAndAddTransitVehicleType(Scenario scenario) {
		VehiclesFactory tvf = scenario.getTransitVehicles().getFactory();
		VehicleType busType = tvf.createVehicleType(busTypeID);
		{
			VehicleCapacity capacity = busType.getCapacity();
			capacity.setSeats(100);
		}
		{
			busType.setMaximumVelocity(100. / 3.6);
		}
		scenario.getTransitVehicles().addVehicleType(busType);
	}

	void createAndAddTransitLine(Scenario scenario) {
		PopulationFactory pf = scenario.getPopulation().getFactory();
		TransitSchedule schedule = scenario.getTransitSchedule();
		TransitScheduleFactory tsf = schedule.getFactory();
		VehiclesFactory tvf = scenario.getTransitVehicles().getFactory();

		List<Id<Link>> linkIds = new ArrayList<>();
		linkIds.add(TR_LINK_0_1_ID);
		linkIds.add(TR_LONG_LINK_LEFT_ID);
		linkIds.add(TR_LINK_MIDDLE_ID);
		linkIds.add(TR_LONG_LINK_RIGHT_ID);
		linkIds.add(TR_LINK_LASTM1_LAST_ID);
		NetworkRoute route = createNetworkRoute(TR_LINK_m1_0_ID, linkIds, TR_LINK_LAST_LASTp1_ID, pf);

		List<TransitRouteStop> stops = new ArrayList<>();
		{
			stops.add(tsf.createTransitRouteStop(schedule.getFacilities().get(tr_stop_fac_0_ID), 0., 0.));
			stops.add(tsf.createTransitRouteStop(schedule.getFacilities().get(tr_stop_fac_5000_ID), 1., 1.));
			stops.add(tsf.createTransitRouteStop(schedule.getFacilities().get(tr_stop_fac_10000_ID), 1., 1.));
		}
		{
			TransitRoute transitRoute = tsf.createTransitRoute(Id.create("route1", TransitRoute.class), route, stops,
					"bus");
			for (int ii = 0; ii < 100; ii++) {
				String str = "tr_" + ii;

				scenario.getTransitVehicles()
						.addVehicle(tvf.createVehicle(Id.createVehicleId(str),
								scenario.getTransitVehicles().getVehicleTypes().get(busTypeID)));

				Departure departure = tsf.createDeparture(Id.create(str, Departure.class), 7. * 3600. + ii * 300);
				departure.setVehicleId(Id.createVehicleId(str));
				transitRoute.addDeparture(departure);
			}
			TransitLine line = tsf.createTransitLine(Id.create("line1", TransitLine.class));
			line.addRoute(transitRoute);

			schedule.addTransitLine(line);
		}
	}

	static void createAndAddCarNetwork(Scenario scenario, int lastNodeIdx, double deltaX) {
		// Construct a network and facilities along a line:
		// 0 --(0-1)-- 1 --(2-1)-- 2 -- ...
		// with a facility of same ID attached to each link.

		NetworkFactory nf = scenario.getNetwork().getFactory();
		ActivityFacilitiesFactory ff = scenario.getActivityFacilities().getFactory();

		Node prevNode;
		{
			Node node = nf.createNode(Id.createNodeId(0), new Coord(0., 0.));
			scenario.getNetwork().addNode(node);
			prevNode = node;
		}
		for (int ii = 1; ii <= lastNodeIdx; ii++) {
			Node node = nf.createNode(Id.createNodeId(ii), new Coord(ii * deltaX, 0.));
			scenario.getNetwork().addNode(node);
			// ---
			addLinkAndFacility(scenario, nf, ff, prevNode, node);
			addLinkAndFacility(scenario, nf, ff, node, prevNode);
			// ---
			prevNode = node;
		}
	}

	private static void addLinkAndFacility(Scenario scenario, NetworkFactory nf, ActivityFacilitiesFactory ff,
			Node prevNode, Node node) {
		final String str = prevNode.getId() + "-" + node.getId();
		Link link = nf.createLink(Id.createLinkId(str), prevNode, node);
		Set<String> set = new HashSet<>();
		set.add("car");
		link.setAllowedModes(set);
		link.setLength(CoordUtils.calcEuclideanDistance(prevNode.getCoord(), node.getCoord()));
		link.setCapacity(3600.);
		link.setFreespeed(50. / 3.6);
		scenario.getNetwork().addLink(link);
		// ---
		ActivityFacility af = ff.createActivityFacility(Id.create(str, ActivityFacility.class), link.getCoord(),
				link.getId());
		ActivityOption option = ff.createActivityOption("shop");
		af.addActivityOption(option);
		scenario.getActivityFacilities().addActivityFacility(af);
	}

	private static NetworkRoute createNetworkRoute(Id<Link> startLinkId, List<Id<Link>> linkIds, Id<Link> endLinkId,
			PopulationFactory pf) {
		NetworkRoute route = pf.getRouteFactories().createRoute(NetworkRoute.class, startLinkId, endLinkId);
		route.setLinkIds(startLinkId, linkIds, endLinkId);
		return route;
	}

	private static void createAndAddTransitLink(Scenario scenario, Node node0, Node node1, Id<Link> TR_LINK_0_1_ID) {
		Link trLink = scenario.getNetwork().getFactory().createLink(TR_LINK_0_1_ID, node0, node1);
		trLink.setFreespeed(100. / 3.6);
		trLink.setCapacity(100000.);
		scenario.getNetwork().addLink(trLink);
	}

	static void createAndAddPopulation(Scenario scenario, String mode, long numberOfPersons) {
		PopulationFactory pf = scenario.getPopulation().getFactory();
		List<ActivityFacility> facilitiesAsList = new ArrayList<>(
				scenario.getActivityFacilities().getFacilities().values());
		final Id<ActivityFacility> activityFacilityId = facilitiesAsList.get(facilitiesAsList.size() - 1).getId();
		for (int jj = 0; jj < numberOfPersons; jj++) {
			Person person = pf.createPerson(Id.createPersonId(jj));
			{
				scenario.getPopulation().addPerson(person);
				Plan plan = pf.createPlan();
				person.addPlan(plan);

				// --- 1st location at randomly selected facility:
				int idx = MatsimRandom.getRandom().nextInt(facilitiesAsList.size());
				;
				Id<ActivityFacility> homeFacilityId = facilitiesAsList.get(idx).getId();
				;
				Activity home = pf.createActivityFromActivityFacilityId("dummy", homeFacilityId);
				if (jj == 0) {
					home.setEndTime(7.
							* 3600.); // one agent one sec earlier so that for all others the initial acts are visible in VIA
				} else {
					home.setEndTime(7. * 3600. + 1.);
				}
				plan.addActivity(home);
				{
					Leg leg = pf.createLeg(mode);
					leg.setDepartureTime(7. * 3600.);
					leg.setTravelTime(1800.);
					plan.addLeg(leg);
				}
				{
					Activity shop = pf.createActivityFromActivityFacilityId("dummy", activityFacilityId);
					plan.addActivity(shop);
				}
			}
		}
	}
}
