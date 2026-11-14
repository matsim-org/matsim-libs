/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayTravelTimeInProgressTripsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

/**
 * Feeds events directly into {@link WithinDayTravelTime} to test how trips that are still in progress
 * on a link are taken into account.
 */
public class WithinDayTravelTimeInProgressTripsTest {

	// free speed travel time 10s, so the initial time window is 25s
	private static final double FREE_SPEED_TRAVEL_TIME = 10.0;

	private WithinDayTravelTime travelTime;
	private Link link;
	private double now;

	@BeforeEach
	void setUp() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();
		Node from = NetworkUtils.createAndAddNode(network, Id.createNodeId("from"), new Coord(0, 0));
		Node to = NetworkUtils.createAndAddNode(network, Id.createNodeId("to"), new Coord(100, 0));
		link = NetworkUtils.createAndAddLink(network, Id.createLinkId("link"), from, to, 100, 100 / FREE_SPEED_TRAVEL_TIME, 3600, 1);

		travelTime = new WithinDayTravelTime(scenario, null);
		travelTime.notifyMobsimInitialized(new MobsimInitializedEvent<>(null));
		now = 0;
	}

	@AfterEach
	void tearDown() {
		travelTime.notifyMobsimBeforeCleanup(new MobsimBeforeCleanupEvent<>(null));
	}

	@Test
	void blockedLink_travelTimeRisesWithElapsedTimeOfVehicleOnLink() {
		enter("blocked", 0);

		for (double t = 100; t <= 1000; t += 100) {
			assertEquals(t, travelTimeAt(t), 0, "travel time at " + t);
		}
	}

	@Test
	void blockedLink_vehicleLeavingTraffic_returnsToFreeSpeed() {
		enter("parking", 0);
		assertEquals(100, travelTimeAt(100), 0);

		travelTime.handleEvent(new VehicleLeavesTrafficEvent(100, null, link.getId(), vehicleId("parking"), TransportMode.car, 1.0));
		assertEquals(FREE_SPEED_TRAVEL_TIME, travelTimeAt(101), 0);
	}

	@Test
	void blockedLink_vehicleAborted_returnsToFreeSpeed() {
		enter("aborted", 0);
		assertEquals(100, travelTimeAt(100), 0);

		travelTime.handleEvent(new VehicleAbortsEvent(100, vehicleId("aborted"), link.getId()));
		assertEquals(FREE_SPEED_TRAVEL_TIME, travelTimeAt(101), 0);
	}

	@Test
	void trickleOutflow_noDropToFreeSpeedWhileVehiclesAreOnLink() {
		// a queue of vehicles enters at once; one leaves every 200s, i.e. gaps are far longer than the time window
		int queueLength = 5;
		for (int i = 0; i < queueLength; i++) {
			enter("queued" + i, 0);
		}

		for (int i = 0; i < queueLength; i++) {
			double leaveTime = 200 * (i + 1);
			for (double t = leaveTime - 199; t <= leaveTime; t++) {
				// the vehicles still queued entered at 0
				assertEquals(Math.max(FREE_SPEED_TRAVEL_TIME, t), travelTimeAt(t), 0, "travel time at " + t);
			}
			leave("queued" + i, leaveTime);
		}
	}

	@Test
	void emptyLink_returnsToFreeSpeedAfterLastVehicleLeavesAndWindowPasses() {
		enter("congested", 0);
		assertEquals(50, travelTimeAt(50), 0);
		leave("congested", 50);

		assertEquals(50, travelTimeAt(51), 0);
		// the trip ages out of the (enlarged) window of 2 * 50s
		assertEquals(FREE_SPEED_TRAVEL_TIME, travelTimeAt(151), 0);
	}

	@Test
	void flowingTraffic_travelTimeIsMeanOfCompletedTrips() {
		// one vehicle enters every second and needs 12s
		int tripTime = 12;
		for (int i = 0; i < 100; i++) {
			// once trips have been completed, the vehicles on the link have not been on it longer than the
			// completed trips took, so the mean of the completed trips is reported (as before trips in progress were considered)
			if (i > tripTime) assertEquals(tripTime, travelTimeAt(i), 0, "travel time at " + i);
			enter("flowing" + i, i);
			if (i >= tripTime) {
				leave("flowing" + (i - tripTime), i);
			}
		}
	}

	private void enter(String vehicle, double time) {
		travelTime.handleEvent(new LinkEnterEvent(time, vehicleId(vehicle), link.getId()));
	}

	private void leave(String vehicle, double time) {
		travelTime.handleEvent(new LinkLeaveEvent(time, vehicleId(vehicle), link.getId()));
	}

	private static Id<Vehicle> vehicleId(String vehicle) {
		return Id.createVehicleId(vehicle);
	}

	/**
	 * Advances the sim time step by step (as the mobsim does) until the given time and returns the link travel time.
	 */
	private double travelTimeAt(double time) {
		while (now < time) {
			now++;
			travelTime.notifyMobsimBeforeSimStep(new MobsimBeforeSimStepEvent<>(null, now));
		}
		return travelTime.getLinkTravelTime(link, time, null, null);
	}
}
