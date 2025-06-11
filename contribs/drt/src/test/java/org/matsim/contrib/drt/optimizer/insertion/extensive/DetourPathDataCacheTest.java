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
import static org.mockito.Mockito.mock;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DetourPathDataCacheTest {
	private final Link pickupLink = link("pickupLink");
	private final Link dropoffLink = link("dropoffLink");
	private final Link startLink = link("startLink");
	private final Link stop0Link = link("stop0Link");
	private final Link stop1Link = link("stop1Link");

	private final PathData start_pickup = mock(PathData.class);
	private final PathData stop0_pickup = mock(PathData.class);
	private final PathData stop1_pickup = mock(PathData.class);

	private final PathData pickup_stop0 = mock(PathData.class);
	private final PathData pickup_stop1 = mock(PathData.class);

	private final PathData pickup_dropoff = mock(PathData.class);

	private final PathData stop0_dropoff = mock(PathData.class);
	private final PathData stop1_dropoff = mock(PathData.class);

	private final PathData dropoff_stop0 = mock(PathData.class);
	private final PathData dropoff_stop1 = mock(PathData.class);

	private final DrtRequest request = DrtRequest.newBuilder().fromLink(pickupLink).toLink(dropoffLink).build();
	private final VehicleEntry entry = entry(startLink, stop0Link, stop1Link);

	private final ImmutableMap<Link, PathData> pathToPickupMap = ImmutableMap.of(startLink, start_pickup, stop0Link,
			stop0_pickup, stop1Link, stop1_pickup);

	private final ImmutableMap<Link, PathData> pathFromPickupMap = ImmutableMap.of(stop0Link, pickup_stop0, stop1Link,
			pickup_stop1, dropoffLink, pickup_dropoff);

	private final ImmutableMap<Link, PathData> pathToDropoffMap = ImmutableMap.of(pickupLink, pickup_dropoff, stop0Link,
			stop0_dropoff, stop1Link, stop1_dropoff);

	private final ImmutableMap<Link, PathData> pathFromDropoffMap = ImmutableMap.of(stop0Link, dropoff_stop0, stop1Link,
			dropoff_stop1);

	private static final PathData ZERO_DETOUR = mock(PathData.class);
	private final DetourPathDataCache detourPathDataCache = new DetourPathDataCache(pathToPickupMap, pathFromPickupMap,
			pathToDropoffMap, pathFromDropoffMap, ZERO_DETOUR);

	private static final IntegerLoadType LOAD_TYPE = new IntegerLoadType("passengers");

	@Test
	void insertion_0_0() {
		assertInsertion(0, 0, start_pickup, pickup_dropoff, null, dropoff_stop0);
	}

	@Test
	void insertion_0_1() {
		assertInsertion(0, 1, start_pickup, pickup_stop0, stop0_dropoff, dropoff_stop1);
	}

	@Test
	void insertion_0_2() {
		assertInsertion(0, 2, start_pickup, pickup_stop0, stop1_dropoff, ZERO_DETOUR);
	}

	@Test
	void insertion_1_1() {
		assertInsertion(1, 1, stop0_pickup, pickup_dropoff, null, dropoff_stop1);
	}

	@Test
	void insertion_1_2() {
		assertInsertion(1, 2, stop0_pickup, pickup_stop1, stop1_dropoff, ZERO_DETOUR);
	}

	@Test
	void insertion_2_2() {
		assertInsertion(2, 2, stop1_pickup, pickup_dropoff, null, ZERO_DETOUR);
	}

	private void assertInsertion(int pickupIdx, int dropoffIdx, PathData detourToPickup, PathData detourFromPickup,
			PathData detourToDropoff, PathData detourFromDropoff) {
		Insertion insertion = new Insertion(request, entry, pickupIdx, dropoffIdx);
		var actual = detourPathDataCache.createInsertionDetourData(insertion);

		var expectedInsertionDetourData = new InsertionDetourData(detourToPickup, detourFromPickup, detourToDropoff,
				detourFromDropoff);

		assertThat(actual).usingRecursiveComparison().isEqualTo(expectedInsertionDetourData);
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private VehicleEntry entry(Link startLink, Link... stopLinks) {
		return new VehicleEntry(null, new Waypoint.Start(null, startLink, 0, LOAD_TYPE.fromInt(0)),
				Arrays.stream(stopLinks).map(this::stop).collect(ImmutableList.toImmutableList()), null, null, 0);
	}

	private Waypoint.Stop stop(Link link) {
		return new Waypoint.Stop(new DefaultDrtStopTask(0, 60, link), LOAD_TYPE.getEmptyLoad(), LOAD_TYPE);
	}
}
