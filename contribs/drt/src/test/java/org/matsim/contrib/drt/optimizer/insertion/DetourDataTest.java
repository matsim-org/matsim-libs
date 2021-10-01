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

import java.util.Arrays;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DetourDataTest {
	private final Link pickupLink = link("pickupLink");
	private final Link dropoffLink = link("dropoffLink");
	private final Link startLink = link("startLink");
	private final Link stop0Link = link("stop0Link");
	private final Link stop1Link = link("stop1Link");

	private final String start_pickup = "start_pickup";
	private final String stop0_pickup = "stop0_pickup";
	private final String stop1_pickup = "stop1_pickup";

	private final String pickup_stop0 = "pickup_stop0";
	private final String pickup_stop1 = "pickup_stop1";

	private final String pickup_dropoff = "pickup_dropoff";

	private final String stop0_dropoff = "stop0_dropoff";
	private final String stop1_dropoff = "stop1_dropoff";

	private final String dropoff_stop0 = "dropoff_stop0";
	private final String dropoff_stop1 = "dropoff_stop1";

	private final DrtRequest request = DrtRequest.newBuilder().fromLink(pickupLink).toLink(dropoffLink).build();
	private final VehicleEntry entry = entry(startLink, stop0Link, stop1Link);

	private final ImmutableMap<Link, String> pathToPickupMap = ImmutableMap.of(startLink, start_pickup, stop0Link,
			stop0_pickup, stop1Link, stop1_pickup);

	private final ImmutableMap<Link, String> pathFromPickupMap = ImmutableMap.of(stop0Link, pickup_stop0, stop1Link,
			pickup_stop1, dropoffLink, pickup_dropoff);

	private final ImmutableMap<Link, String> pathToDropoffMap = ImmutableMap.of(pickupLink, pickup_dropoff, stop0Link,
			stop0_dropoff, stop1Link, stop1_dropoff);

	private final ImmutableMap<Link, String> pathFromDropoffMap = ImmutableMap.of(stop0Link, dropoff_stop0, stop1Link,
			dropoff_stop1);

	private static final String ZERO_DETOUR = "zero_detour";
	private final DetourData<String> detourData = new DetourData<>(pathToPickupMap, pathFromPickupMap, pathToDropoffMap,
			pathFromDropoffMap, ZERO_DETOUR);

	@Test
	public void insertion_0_0() {
		assertInsertion(0, 0, start_pickup, pickup_dropoff, null, dropoff_stop0);
	}

	@Test
	public void insertion_0_1() {
		assertInsertion(0, 1, start_pickup, pickup_stop0, stop0_dropoff, dropoff_stop1);
	}

	@Test
	public void insertion_0_2() {
		assertInsertion(0, 2, start_pickup, pickup_stop0, stop1_dropoff, ZERO_DETOUR);
	}

	@Test
	public void insertion_1_1() {
		assertInsertion(1, 1, stop0_pickup, pickup_dropoff, null, dropoff_stop1);
	}

	@Test
	public void insertion_1_2() {
		assertInsertion(1, 2, stop0_pickup, pickup_stop1, stop1_dropoff, ZERO_DETOUR);
	}

	@Test
	public void insertion_2_2() {
		assertInsertion(2, 2, stop1_pickup, pickup_dropoff, null, ZERO_DETOUR);
	}

	private void assertInsertion(int pickupIdx, int dropoffIdx, String detourToPickup, String detourFromPickup,
			String detourToDropoff, String detourFromDropoff) {
		Insertion insertion = new Insertion(request, entry, pickupIdx, dropoffIdx);
		var actual = detourData.createInsertionWithDetourData(insertion);
		var expected = new InsertionWithDetourData<>(insertion, detourToPickup, detourFromPickup, detourToDropoff,
				detourFromDropoff);
		assertThat(actual).isEqualToComparingFieldByField(expected);
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private VehicleEntry entry(Link startLink, Link... stopLinks) {
		return new VehicleEntry(null, new Waypoint.Start(null, startLink, 0, 0),
				Arrays.stream(stopLinks).map(this::stop).collect(ImmutableList.toImmutableList()));
	}

	private Waypoint.Stop stop(Link link) {
		return new Waypoint.Stop(new DrtStopTask(0, 60, link), 0);
	}

	@Test
	public void testCreate() {
		Insertion insertion = new Insertion(request, entry, 0, 1);
		var timeEstimates = ImmutableTable.<Link, Link, Double>builder()//
				.put(startLink, pickupLink, 12.)
				.put(pickupLink, stop0Link, 34.)
				.put(stop0Link, dropoffLink, 56.)
				.put(dropoffLink, stop1Link, 78.)
				.build();
		var detourData = DetourData.create(timeEstimates::get, request).createInsertionWithDetourData(insertion);

		var expectedDetourData = new InsertionWithDetourData<>(insertion, 12., 34., 56., 78.);
		assertThat(detourData).isEqualToComparingFieldByField(expectedDetourData);
	}
}
