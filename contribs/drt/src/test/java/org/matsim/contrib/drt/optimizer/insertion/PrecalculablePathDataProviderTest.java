/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.optimizer.insertion.PrecalculablePathDataProvider.getPathDataSet;

import java.util.Arrays;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.VehicleData.Start;
import org.matsim.contrib.drt.optimizer.VehicleData.Stop;
import org.matsim.contrib.drt.optimizer.insertion.DetourDataProvider.DetourDataSet;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.testcases.fakes.FakeLink;
import org.matsim.testcases.fakes.FakeNode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author Michal Maciejewski (michalm)
 */
public class PrecalculablePathDataProviderTest {
	private final Node node0 = node("0");
	private final Node node1 = node("1");
	private final Node node2 = node("2");
	private final Node node3 = node("3");
	private final Node node4 = node("4");

	private final Link pickupLink = link(node0, node1);
	private final Link dropoffLink = link(node1, node2);
	private final Link startLink = link(node2, node3);
	private final Link stop0Link = link(node3, node4);
	private final Link stop1Link = link(node4, node0);

	private final PathData start_pickup = pathData();
	private final PathData stop0_pickup = pathData();
	private final PathData stop1_pickup = pathData();

	private final PathData pickup_stop0 = pathData();
	private final PathData pickup_stop1 = pathData();

	private final PathData pickup_dropoff = pathData();

	private final PathData stop0_dropoff = pathData();
	private final PathData stop1_dropoff = pathData();

	private final PathData dropoff_stop0 = pathData();
	private final PathData dropoff_stop1 = pathData();

	private final PathData dropoff_zeroPath = pathData();

	@Test
	public void testGetPathDataSet() {
		DrtRequest request = DrtRequest.newBuilder().fromLink(pickupLink).toLink(dropoffLink).build();
		Entry entry = entry(startLink, stop0Link, stop1Link);
		var pathToPickupMap = ImmutableMap.of(startLink.getId(), start_pickup, stop0Link.getId(), stop0_pickup,
				stop1Link.getId(), stop1_pickup);

		var pathFromPickupMap = ImmutableMap.of(stop0Link.getId(), pickup_stop0, stop1Link.getId(), pickup_stop1,
				dropoffLink.getId(), pickup_dropoff);

		var pathToDropoffMap = ImmutableMap.of(pickupLink.getId(), pickup_dropoff, stop0Link.getId(), stop0_dropoff,
				stop1Link.getId(), stop1_dropoff);

		var pathFromDropoffMap = ImmutableMap.of(dropoffLink.getId(), dropoff_zeroPath, stop0Link.getId(),
				dropoff_stop0, stop1Link.getId(), dropoff_stop1);

		DetourDataSet<PathData> pathDataSet = getPathDataSet(request, entry, pathToPickupMap, pathFromPickupMap,
				pathToDropoffMap, pathFromDropoffMap);

		DetourDataSet<PathData> expectedDataSet = new DetourDataSet<>(//
				asArray(start_pickup, stop0_pickup, stop1_pickup),//
				asArray(pickup_dropoff, pickup_stop0, pickup_stop1),//
				asArray(null, stop0_dropoff, stop1_dropoff),//
				asArray(null, dropoff_stop0, dropoff_stop1));

		assertThat(pathDataSet).isEqualToComparingFieldByField(expectedDataSet);
	}

	private PathData[] asArray(PathData... pathData) {
		return pathData;
	}

	private Link link(Node from, Node to) {
		return new FakeLink(Id.createLinkId(from.getId() + "_" + to.getId()), from, to);
	}

	private Node node(String id) {
		return new FakeNode(Id.createNodeId(id));
	}

	private Entry entry(Link startLink, Link... stopLinks) {
		return new Entry(null, new Start(null, startLink, 0, 0),
				Arrays.stream(stopLinks).map(this::stop).collect(ImmutableList.toImmutableList()));
	}

	private Stop stop(Link link) {
		return new Stop(new DrtStopTask(0, 60, link), 0);
	}

	private PathData pathData() {
		return new PathData(new Path(null, ImmutableList.of(), 0, 0), 0);
	}
}
