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

import org.assertj.core.api.ListAssert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleData.Start;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionGeneratorTest {
	private static final int CAPACITY = 4;

	private final Link fromLink = link("from");
	private final Link toLink = link("to");
	private final DrtRequest drtRequest = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).build();

	private final Link depotLink = link("depot");
	private final DvrpVehicleSpecification vehicleSpecification = ImmutableDvrpVehicleSpecification.newBuilder()
			.id(Id.create("v1", DvrpVehicle.class))
			.capacity(CAPACITY)
			.startLinkId(depotLink.getId())
			.serviceBeginTime(0)
			.serviceEndTime(24 * 3600)
			.build();
	private final DvrpVehicle vehicle = new DvrpVehicleImpl(vehicleSpecification, depotLink);

	@Test
	public void generateInsertions_startEmpty_noStops() {
		Start start = new Start(null, link("0"), 0, 0); //no stops => must be empty
		VehicleData.Entry entry = entry(start);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//pickup after start
				insertion(entry, 0, 0));
	}

	@Test
	public void generateInsertions_startNotFull_oneStop() {
		Start start = new Start(null, link("start"), 0, 1); // 1 pax aboard
		VehicleData.Stop stop0 = stop(link("stop0"), 0);//drop off 1 pax
		VehicleData.Entry entry = entry(start, stop0);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//pickup after start
				insertion(entry, 0, 0), insertion(entry, 0, 1),
				//picup after stop 0
				insertion(entry, 1, 1));
	}

	@Test
	public void generateInsertions_startFull_oneStop() {
		Start start = new Start(null, link("start"), 0, CAPACITY); //full
		VehicleData.Stop stop0 = stop(link("stop0"), 0);//drop off 4 pax
		VehicleData.Entry entry = entry(start, stop0);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//no pickup after stop
				//pickup after stop 0
				insertion(entry, 1, 1));
	}

	@Test
	public void generateInsertions_startEmpty_twoStops_notFullBetweenStops() {
		Start start = new Start(null, link("start"), 0, 0); //empty
		VehicleData.Stop stop0 = stop(link("stop0"), 1);//pick up 1 pax
		VehicleData.Stop stop1 = stop(link("stop1"), 0);//drop off 1 pax
		VehicleData.Entry entry = entry(start, stop0, stop1);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//pickup after start
				insertion(entry, 0, 0), insertion(entry, 0, 1), insertion(entry, 0, 2),
				//pickup after stop 0
				insertion(entry, 1, 1), insertion(entry, 1, 2),
				//pickup after stop 1
				insertion(entry, 2, 2));
	}

	@Test
	public void generateInsertions_startEmpty_twoStops_fullBetweenStops() {
		Start start = new Start(null, link("start"), 0, 0); //empty
		VehicleData.Stop stop0 = stop(link("stop0"), CAPACITY);//pick up 4 pax (full)
		VehicleData.Stop stop1 = stop(link("stop1"), 0);//drop off 4 pax
		VehicleData.Entry entry = entry(start, stop0, stop1);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//pickup after start
				insertion(entry, 0, 0),
				//no pickup after stop 0
				//pickup after stop 1
				insertion(entry, 2, 2));
	}

	@Test
	public void generateInsertions_startFull_twoStops_notFullBetweenStops() {
		Start start = new Start(null, link("start"), 0, CAPACITY); //full
		VehicleData.Stop stop0 = stop(link("stop0"), 2);//drop off 2 pax
		VehicleData.Stop stop1 = stop(link("stop1"), 0);//drop off 2 pax
		VehicleData.Entry entry = entry(start, stop0, stop1);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//no pickup after start
				//pickup after stop 0
				insertion(entry, 1, 1), insertion(entry, 1, 2),
				//pickup after stop 1
				insertion(entry, 2, 2));
	}

	@Test
	public void generateInsertions_startFull_twoStops_fullBetweenStops() {
		Start start = new Start(null, link("start"), 0, CAPACITY); //full
		VehicleData.Stop stop0 = stop(link("stop0"), CAPACITY);//drop off 1 pax, pickup 1 pax (full)
		VehicleData.Stop stop1 = stop(link("stop1"), 0);//drop off 4 pax
		VehicleData.Entry entry = entry(start, stop0, stop1);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//no pickup after start
				//no pickup after stop 0
				//pickup after stop 1
				insertion(entry, 2, 2));
	}

	@Test
	public void generateInsertions_startNotFull_threeStops_emptyBetweenStops01_fullBetweenStops12() {
		Start start = new Start(null, link("start"), 0, 1); //empty
		VehicleData.Stop stop0 = stop(link("stop0"), 0);// dropoff 1 pax
		VehicleData.Stop stop1 = stop(link("stop1"), CAPACITY);// pickup 4 pax
		VehicleData.Stop stop2 = stop(link("stop2"), 0);// dropoff 4 pax
		VehicleData.Entry entry = entry(start, stop0, stop1, stop2);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//pickup after start
				insertion(entry, 0, 0), insertion(entry, 0, 1),
				//pickup after stop 0
				insertion(entry, 1, 1),
				//pickup after stop 1
				//pickup after stop 2
				insertion(entry, 3, 3));
	}

	@Test
	public void generateInsertions_startFull_threeStops_emptyBetweenStops01_fullBetweenStops12() {
		Start start = new Start(null, link("start"), 0, CAPACITY); //full
		VehicleData.Stop stop0 = stop(link("stop0"), 0);// dropoff 4 pax
		VehicleData.Stop stop1 = stop(link("stop1"), CAPACITY);// pickup 4 pax
		VehicleData.Stop stop2 = stop(link("stop2"), 0);// dropoff 4 pax
		VehicleData.Entry entry = entry(start, stop0, stop1, stop2);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//no pickup after start
				//pickup after stop 0
				insertion(entry, 1, 1),
				//pickup after stop 1
				//pickup after stop 2
				insertion(entry, 3, 3));
	}

	@Test
	public void generateInsertions_noDetourForPickup_noDuplicatedInsertions() {
		Start start = new Start(null, link("start"), 0, 1); // 1 pax
		VehicleData.Stop stop0 = stop(fromLink, 0);//dropoff 1 pax
		VehicleData.Entry entry = entry(start, stop0);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//no pickup after start (pickup is exactly at stop0)
				//pickup after stop 0
				insertion(entry, 1, 1));
	}

	@Test
	public void generateInsertions_noDetourForDropoff_noDuplicatedInsertions() {
		Start start = new Start(null, link("start"), 0, 1); // 1 pax
		VehicleData.Stop stop0 = stop(toLink, 0);//dropoff 1 pax
		VehicleData.Entry entry = entry(start, stop0);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//pickup after start: insertion(0, 0) is a duplicate of insertion(0, 1)
				insertion(entry, 0, 1),
				//pickup after stop 0
				insertion(entry, 1, 1));
	}

	@Test
	public void generateInsertions_noDetourForDropoff_vehicleOutgoingFullAfterDropoff_insertionPossible() {
		// a special case where we allow inserting the dropoff after a stop despite outgoingOccupancy == maxCapacity
		// this is only because the the dropoff happens exactly at (not after) the stop
		Start start = new Start(null, link("start"), 0, 1); // 1 pax
		VehicleData.Stop stop0 = stop(toLink, CAPACITY);//dropoff 1 pax
		VehicleData.Stop stop1 = stop(link("stop1"), 0);//dropoff 1 pax
		VehicleData.Entry entry = entry(start, stop0, stop1);
		assertThatInsertions(drtRequest, entry).containsExactly(
				//pickup after start: insertion(0, 0) is a duplicate of insertion(0, 1)
				insertion(entry, 0, 1),
				//pickup after stop 0
				insertion(entry, 2, 2));
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private ListAssert<Insertion> assertThatInsertions(DrtRequest drtRequest, VehicleData.Entry entry) {
		int stopCount = entry.stops.size();
		int endOccupancy = stopCount > 0 ? entry.stops.get(stopCount - 1).outgoingOccupancy : entry.start.occupancy;
		Preconditions.checkArgument(endOccupancy == 0);//make sure the input is valid

		return assertThat(new InsertionGenerator().generateInsertions(drtRequest, entry));
	}

	private Insertion insertion(VehicleData.Entry entry, int pickupIdx, int dropoffIdx) {
		return new Insertion(drtRequest, entry, pickupIdx, dropoffIdx);
	}

	private VehicleData.Stop stop(Link link, int outputOccupancy) {
		return new VehicleData.Stop(new DrtStopTask(0, 0, link), outputOccupancy);
	}

	private VehicleData.Entry entry(Start start, VehicleData.Stop... stops) {
		return new VehicleData.Entry(vehicle, start, ImmutableList.copyOf(stops));
	}
}
