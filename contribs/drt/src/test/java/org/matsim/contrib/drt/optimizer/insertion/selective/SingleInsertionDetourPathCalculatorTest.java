/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion.selective;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.dvrp.path.VrpPaths.FIRST_LINK_TT;
import static org.matsim.contrib.dvrp.path.VrpPaths.getLastLinkTT;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.Waypoint.Dropoff;
import org.matsim.contrib.drt.optimizer.Waypoint.End;
import org.matsim.contrib.drt.optimizer.Waypoint.Pickup;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.InsertionPoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.testcases.fakes.FakeNode;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SingleInsertionDetourPathCalculatorTest {

	private final Link beforePickupLink = link("before_pickup");
	private final Link pickupLink = link("pickup");
	private final Link afterPickupLink = link("after_pickup");

	private final Link beforeDropoffLink = link("before_dropoff");
	private final Link dropoffLink = link("dropoff");
	private final Link afterDropoffLink = link("after_dropoff");

	private final DrtRequest request = DrtRequest.newBuilder()
			.fromLink(pickupLink)
			.toLink(dropoffLink)
			.earliestStartTime(100)
			.latestStartTime(200)
			.latestArrivalTime(500)
			.build();

	private final LeastCostPathCalculator pathCalculator = mock(LeastCostPathCalculator.class);
	private final SingleInsertionDetourPathCalculator detourPathCalculator = new SingleInsertionDetourPathCalculator(
			null, new FreeSpeedTravelTime(), null, 1, (network, travelCosts, travelTimes) -> pathCalculator);

	@After
	public void after() {
		detourPathCalculator.notifyMobsimBeforeCleanup(null);
	}

	@Test
	public void calculatePaths() {
		var pathToPickup = mockCalcLeastCostPath(beforePickupLink, pickupLink, request.getEarliestStartTime(), 11);
		var pathFromPickup = mockCalcLeastCostPath(pickupLink, afterPickupLink, request.getEarliestStartTime(), 22);
		var pathToDropoff = mockCalcLeastCostPath(beforeDropoffLink, dropoffLink, request.getLatestArrivalTime(), 33);
		var pathFromDropoff = mockCalcLeastCostPath(dropoffLink, afterDropoffLink, request.getLatestArrivalTime(), 44);

		var pickup = insertionPoint(waypoint(beforePickupLink), waypoint(afterPickupLink));
		var dropoff = insertionPoint(waypoint(beforeDropoffLink), waypoint(afterDropoffLink));
		var insertion = new Insertion(null, pickup, dropoff);

		var insertionWithDetourData = detourPathCalculator.calculatePaths(request, insertion);

		assertPathData(insertionWithDetourData.detourToPickup, pathToPickup, pickupLink);
		assertPathData(insertionWithDetourData.detourFromPickup, pathFromPickup, afterPickupLink);
		assertPathData(insertionWithDetourData.detourToDropoff, pathToDropoff, dropoffLink);
		assertPathData(insertionWithDetourData.detourFromDropoff, pathFromDropoff, afterDropoffLink);
	}

	@Test
	public void calculatePaths_dropoffAfterPickup_dropoffAtEnd() {
		//compute only 2 paths (instead of 4)
		var pathToPickup = mockCalcLeastCostPath(beforePickupLink, pickupLink, request.getEarliestStartTime(), 11);
		var pathFromPickup = mockCalcLeastCostPath(pickupLink, dropoffLink, request.getEarliestStartTime(), 22);

		//use specific class of waypoint to allow for detecting the special case
		var pickup = insertionPoint(waypoint(beforePickupLink), waypoint(dropoffLink, Dropoff.class));
		var dropoff = insertionPoint(waypoint(pickupLink, Pickup.class), waypoint(dropoffLink, End.class));
		var insertion = new Insertion(null, pickup, dropoff);

		var insertionWithDetourData = detourPathCalculator.calculatePaths(request, insertion);

		assertPathData(insertionWithDetourData.detourToPickup, pathToPickup, pickupLink);
		assertPathData(insertionWithDetourData.detourFromPickup, pathFromPickup, afterPickupLink);
		//this path data should not be used and is null
		assertThat(insertionWithDetourData.detourToDropoff).isNull();
		//this path data is of zero duration
		assertThat(insertionWithDetourData.detourFromDropoff.getTravelTime()).isZero();
	}

	@Test
	public void calculatePaths_noDetours() {
		var pickup = insertionPoint(waypoint(pickupLink), waypoint(pickupLink));
		var dropoff = insertionPoint(waypoint(dropoffLink), waypoint(dropoffLink));
		var insertion = new Insertion(null, pickup, dropoff);

		var insertionWithDetourData = detourPathCalculator.calculatePaths(request, insertion);

		assertThat(insertionWithDetourData.detourToPickup.getTravelTime()).isZero();
		assertThat(insertionWithDetourData.detourFromPickup.getTravelTime()).isZero();
		assertThat(insertionWithDetourData.detourToDropoff.getTravelTime()).isZero();
		assertThat(insertionWithDetourData.detourFromDropoff.getTravelTime()).isZero();
	}

	private Path mockCalcLeastCostPath(Link fromLink, Link toLink, double startTimeArg, double pathTravelTime) {
		var fromNode = fromLink.getToNode();
		var toNode = toLink.getFromNode();
		var path = new Path(List.of(fromNode, toNode), List.of(), pathTravelTime, pathTravelTime + 1000);
		when(pathCalculator.calcLeastCostPath(eq(fromNode), eq(toNode), eq(startTimeArg + FIRST_LINK_TT), isNull(),
				isNull())).thenReturn(path);
		return path;
	}

	private void assertPathData(PathData pathData, Path inputPath, Link toLink) {
		assertThat(pathData.getTravelTime()).isEqualTo(
				inputPath.travelTime + FIRST_LINK_TT + getLastLinkTT(new FreeSpeedTravelTime(), toLink, Double.NaN));
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id), node(id + "_from"), node(id + "_to"));
	}

	private Node node(String id) {
		return new FakeNode(Id.createNodeId(id));
	}

	private InsertionPoint insertionPoint(Waypoint beforeWaypoint, Waypoint afterWaypoint) {
		return new InsertionPoint(-1, beforeWaypoint, null, afterWaypoint);
	}

	private Waypoint waypoint(Link afterLink) {
		return waypoint(afterLink, Waypoint.class);
	}

	private Waypoint waypoint(Link afterLink, Class<? extends Waypoint> clazz) {
		var waypoint = mock(clazz);
		when(waypoint.getLink()).thenReturn(afterLink);
		return waypoint;
	}
}
