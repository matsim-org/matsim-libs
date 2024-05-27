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

package org.matsim.contrib.drt.optimizer.insertion.extensive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.core.router.util.LeastCostPathCalculator.Path;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.testcases.fakes.FakeNode;
import org.mockito.ArgumentMatchers;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MultiInsertionDetourPathCalculatorTest {
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

	private final OneToManyPathSearch pathSearch = mock(OneToManyPathSearch.class);

	private final MultiInsertionDetourPathCalculator detourPathCalculator = new MultiInsertionDetourPathCalculator(
			pathSearch, pathSearch, pathSearch, pathSearch, 1);

	@AfterEach
	public void after() {
		detourPathCalculator.notifyMobsimBeforeCleanup(null);
	}

	@Test
	void calculatePaths() {
		var pathToPickup = mockCalcPathData(pickupLink, beforePickupLink, request.getEarliestStartTime(), false, 11);
		var pathFromPickup = mockCalcPathData(pickupLink, afterPickupLink, request.getEarliestStartTime(), true, 22);
		var pathToDropoff = mockCalcPathData(dropoffLink, beforeDropoffLink, request.getLatestArrivalTime(), false, 33);
		var pathFromDropoff = mockCalcPathData(dropoffLink, afterDropoffLink, request.getLatestArrivalTime(), true, 44);

		var pickup = insertionPoint(waypoint(beforePickupLink), waypoint(afterPickupLink));
		var dropoff = insertionPoint(waypoint(beforeDropoffLink), waypoint(afterDropoffLink));
		var insertion = new InsertionGenerator.Insertion(null, pickup, dropoff);

		var detourData = detourPathCalculator.calculatePaths(request, List.of(insertion));
		var insertionWithDetourData = detourData.createInsertionDetourData(insertion);

		assertThat(insertionWithDetourData.detourToPickup).isEqualTo(pathToPickup);
		assertThat(insertionWithDetourData.detourFromPickup).isEqualTo(pathFromPickup);
		assertThat(insertionWithDetourData.detourToDropoff).isEqualTo(pathToDropoff);
		assertThat(insertionWithDetourData.detourFromDropoff).isEqualTo(pathFromDropoff);
	}

	@Test
	void calculatePaths_dropoffAfterPickup_dropoffAtEnd() {
		//compute only 2 paths (instead of 4)
		var pathToPickup = mockCalcPathData(pickupLink, beforePickupLink, request.getEarliestStartTime(), false, 11);
		var pathFromPickup = mockCalcPathData(pickupLink, dropoffLink, request.getEarliestStartTime(), true, 22);

		//use specific class of waypoint to allow for detecting the special case
		var pickup = insertionPoint(waypoint(beforePickupLink), waypoint(dropoffLink, Waypoint.Dropoff.class));
		var dropoff = insertionPoint(waypoint(pickupLink, Waypoint.Pickup.class),
				waypoint(dropoffLink, Waypoint.End.class));
		var insertion = new InsertionGenerator.Insertion(null, pickup, dropoff);

		var detourData = detourPathCalculator.calculatePaths(request, List.of(insertion));
		var insertionWithDetourData = detourData.createInsertionDetourData(insertion);

		assertThat(insertionWithDetourData.detourToPickup).isEqualTo(pathToPickup);
		assertThat(insertionWithDetourData.detourFromPickup).isEqualTo(pathFromPickup);
		assertThat(insertionWithDetourData.detourToDropoff).isNull();
		assertThat(insertionWithDetourData.detourFromDropoff.getTravelTime()).isZero();
	}

	@Test
	void calculatePaths_noDetours() {
		// OneToManyPathSearch.calcPathDataMap() returns a map that contains entries for all toLinks
		// (unless the stop criterion terminates computations earlier)
		// If fromLink is in toLinks than PathData.EMPTY is mapped for such a link
		when(pathSearch.calcPathDataMap(eq(pickupLink), eqSingleLinkCollection(pickupLink),
				eq(request.getEarliestStartTime()), anyBoolean())).thenReturn(Map.of(pickupLink, PathData.EMPTY));
		when(pathSearch.calcPathDataMap(eq(dropoffLink), eqSingleLinkCollection(dropoffLink),
				eq(request.getLatestArrivalTime()), anyBoolean())).thenReturn(Map.of(dropoffLink, PathData.EMPTY));

		var pickup = insertionPoint(waypoint(pickupLink), waypoint(pickupLink));
		var dropoff = insertionPoint(waypoint(dropoffLink), waypoint(dropoffLink));
		var insertion = new InsertionGenerator.Insertion(null, pickup, dropoff);

		var detourData = detourPathCalculator.calculatePaths(request, List.of(insertion));
		var insertionWithDetourData = detourData.createInsertionDetourData(insertion);

		assertThat(insertionWithDetourData.detourToPickup).isEqualTo(PathData.EMPTY);
		assertThat(insertionWithDetourData.detourFromPickup).isEqualTo(PathData.EMPTY);
		assertThat(insertionWithDetourData.detourToDropoff).isEqualTo(PathData.EMPTY);
		assertThat(insertionWithDetourData.detourFromDropoff).isEqualTo(PathData.EMPTY);
	}

	private PathData mockCalcPathData(Link fromLink, Link toLink, double startTimeArg, boolean forward,
			double pathTravelTime) {
		var fromNode = fromLink.getToNode();
		var toNode = toLink.getFromNode();
		var path = new Path(List.of(fromNode, toNode), List.of(), pathTravelTime, pathTravelTime + 1000);
		var pathData = new PathData(path, 99);
		when(pathSearch.calcPathDataMap(eq(fromLink), eqSingleLinkCollection(toLink), eq(startTimeArg),
				eq(forward))).thenReturn(Map.of(toLink, pathData));
		return pathData;
	}

	private Collection<Link> eqSingleLinkCollection(Link link) {
		return ArgumentMatchers.argThat(argument -> argument.contains(link) && argument.size() == 1);
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id), node(id + "_from"), node(id + "_to"));
	}

	private Node node(String id) {
		return new FakeNode(Id.createNodeId(id));
	}

	private InsertionGenerator.InsertionPoint insertionPoint(Waypoint beforeWaypoint, Waypoint afterWaypoint) {
		return new InsertionGenerator.InsertionPoint(-1, beforeWaypoint, null, afterWaypoint);
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
