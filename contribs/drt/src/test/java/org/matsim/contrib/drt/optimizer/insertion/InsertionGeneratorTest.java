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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.testcases.fakes.FakeLink;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DropoffDetourInfo;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.PickupDetourInfo;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionGeneratorTest {
	private static final int CAPACITY = 4;

	private static final int STOP_DURATION = 10;

	private static final double TIME_TO_PICKUP = 100;
	private static final double TIME_FROM_PICKUP = 200;//also pickup -> dropoff
	private static final double TIME_TO_DROPOFF = 300;
	private static final double TIME_FROM_DROPOFF = 400;
	private static final double TIME_REPLACED_DRIVE = 100;

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
	public void startEmpty_noStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 0); //no stops => must be empty
		VehicleEntry entry = entry(start);

		var insertions = new ArrayList<InsertionWithDetourData>();
		{//00
			var insertion = new Insertion(drtRequest, entry, 0, 0);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP);
			var dropoff = new DropoffDetourInfo(start.time + pickup.pickupTimeLoss, STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		assertInsertionsWithDetour(drtRequest, entry, insertions);
	}

	@Test
	public void startNotFull_oneStop() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 1); // 1 pax aboard
		Waypoint.Stop stop0 = stop(start.time + TIME_REPLACED_DRIVE, link("stop0"), 0);//drop off 1 pax
		VehicleEntry entry = entry(start, stop0);

		var insertions = new ArrayList<InsertionWithDetourData>();
		{//00
			var insertion = new Insertion(drtRequest, entry, 0, 0);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(pickup.departureTime + TIME_FROM_PICKUP,
					STOP_DURATION + TIME_FROM_DROPOFF);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//01
			var insertion = new Insertion(drtRequest, entry, 0, 1);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(stop0.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					TIME_TO_DROPOFF + STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//11
			var insertion = new Insertion(drtRequest, entry, 1, 1);
			var pickup = new PickupDetourInfo(stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP);
			var dropoff = new DropoffDetourInfo(stop0.getDepartureTime() + pickup.pickupTimeLoss, STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		assertInsertionsWithDetour(drtRequest, entry, insertions);
	}

	@Test
	public void startFull_oneStop() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(start.time + TIME_REPLACED_DRIVE, link("stop0"), 0);//drop off 4 pax
		VehicleEntry entry = entry(start, stop0);

		var insertions = new ArrayList<InsertionWithDetourData>();
		{//11
			var insertion = new Insertion(drtRequest, entry, 1, 1);
			var pickup = new PickupDetourInfo(stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP);
			var dropoff = new DropoffDetourInfo(stop0.getDepartureTime() + pickup.pickupTimeLoss, STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		assertInsertionsWithDetour(drtRequest, entry, insertions);
	}

	@Test
	public void startEmpty_twoStops_notFullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 0); //empty
		Waypoint.Stop stop0 = stop(start.time + TIME_REPLACED_DRIVE, link("stop0"), 1);//pick up 1 pax
		Waypoint.Stop stop1 = stop(stop0.getDepartureTime() + TIME_REPLACED_DRIVE, link("stop1"), 0);//drop off 1 pax
		VehicleEntry entry = entry(start, stop0, stop1);

		var insertions = new ArrayList<InsertionWithDetourData>();
		{//00
			var insertion = new Insertion(drtRequest, entry, 0, 0);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(pickup.departureTime + TIME_FROM_PICKUP,
					STOP_DURATION + TIME_FROM_DROPOFF);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//01
			var insertion = new Insertion(drtRequest, entry, 0, 1);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(stop0.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					TIME_TO_DROPOFF + STOP_DURATION + TIME_FROM_DROPOFF - TIME_REPLACED_DRIVE);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//02
			var insertion = new Insertion(drtRequest, entry, 0, 2);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(stop1.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					TIME_TO_DROPOFF + STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//11
			var insertion = new Insertion(drtRequest, entry, 1, 1);
			var pickup = new PickupDetourInfo(stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(pickup.departureTime + TIME_FROM_PICKUP,
					STOP_DURATION + TIME_FROM_DROPOFF);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//12
			var insertion = new Insertion(drtRequest, entry, 1, 2);
			var pickup = new PickupDetourInfo(stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(stop1.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					TIME_TO_DROPOFF + STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//22
			var insertion = new Insertion(drtRequest, entry, 2, 2);
			var pickup = new PickupDetourInfo(stop1.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP);
			var dropoff = new DropoffDetourInfo(stop1.getDepartureTime() + pickup.pickupTimeLoss, STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		assertInsertionsWithDetour(drtRequest, entry, insertions);
	}

	@Test
	public void startEmpty_twoStops_notFullBetweenStops_tightSlackTimes() {
		//same as startEmpty_twoStops_notFullBetweenStops() but with different slack times
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 0); //empty
		Waypoint.Stop stop0 = stop(start.time + TIME_REPLACED_DRIVE, link("stop0"), 1);//pick up 1 pax
		Waypoint.Stop stop1 = stop(stop0.getDepartureTime() + TIME_REPLACED_DRIVE, link("stop1"), 0);//drop off 1 pax

		double[] slackTimes = { 0, // impossible insertions: 00, 01, 02 (pickup at 0 is not possible)
				500, // additional impossible insertions: 11 (too long total detour); however 12 is possible
				1000 }; // 22 is possible
		VehicleEntry entry = new VehicleEntry(vehicle, start, ImmutableList.of(stop0, stop1), slackTimes);

		var insertions = new ArrayList<InsertionWithDetourData>();
		{//12
			var insertion = new Insertion(drtRequest, entry, 1, 2);
			var pickup = new PickupDetourInfo(stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(stop1.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					TIME_TO_DROPOFF + STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//22
			var insertion = new Insertion(drtRequest, entry, 2, 2);
			var pickup = new PickupDetourInfo(stop1.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP);
			var dropoff = new DropoffDetourInfo(stop1.getDepartureTime() + pickup.pickupTimeLoss, STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		assertInsertionsWithDetour(drtRequest, entry, insertions);
	}

	@Test
	public void startEmpty_twoStops_fullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 0); //empty
		Waypoint.Stop stop0 = stop(0, link("stop0"), CAPACITY);//pick up 4 pax (full)
		Waypoint.Stop stop1 = stop(0, link("stop1"), 0);//drop off 4 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertionsOnly(drtRequest, entry,
				//pickup after start
				new Insertion(drtRequest, entry, 0, 0),
				//no pickup after stop 0
				//pickup after stop 1
				new Insertion(drtRequest, entry, 2, 2));
	}

	@Test
	public void startFull_twoStops_notFullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(0, link("stop0"), 2);//drop off 2 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), 0);//drop off 2 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertionsOnly(drtRequest, entry,
				//no pickup after start
				//pickup after stop 0
				new Insertion(drtRequest, entry, 1, 1),//
				new Insertion(drtRequest, entry, 1, 2),
				//pickup after stop 1
				new Insertion(drtRequest, entry, 2, 2));
	}

	@Test
	public void startFull_twoStops_fullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(0, link("stop0"), CAPACITY);//drop off 1 pax, pickup 1 pax (full)
		Waypoint.Stop stop1 = stop(0, link("stop1"), 0);//drop off 4 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertionsOnly(drtRequest, entry,
				//no pickup after start
				//no pickup after stop 0
				//pickup after stop 1
				new Insertion(drtRequest, entry, 2, 2));
	}

	@Test
	public void startNotFull_threeStops_emptyBetweenStops01_fullBetweenStops12() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 1); //empty
		Waypoint.Stop stop0 = stop(0, link("stop0"), 0);// dropoff 1 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), CAPACITY);// pickup 4 pax
		Waypoint.Stop stop2 = stop(0, link("stop2"), 0);// dropoff 4 pax
		VehicleEntry entry = entry(start, stop0, stop1, stop2);
		assertInsertionsOnly(drtRequest, entry,
				//pickup after start
				new Insertion(drtRequest, entry, 0, 0),//
				new Insertion(drtRequest, entry, 0, 1),
				//pickup after stop 0
				new Insertion(drtRequest, entry, 1, 1),
				//pickup after stop 1
				//pickup after stop 2
				new Insertion(drtRequest, entry, 3, 3));
	}

	@Test
	public void startFull_threeStops_emptyBetweenStops01_fullBetweenStops12() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(0, link("stop0"), 0);// dropoff 4 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), CAPACITY);// pickup 4 pax
		Waypoint.Stop stop2 = stop(0, link("stop2"), 0);// dropoff 4 pax
		VehicleEntry entry = entry(start, stop0, stop1, stop2);
		assertInsertionsOnly(drtRequest, entry,
				//no pickup after start
				//pickup after stop 0
				new Insertion(drtRequest, entry, 1, 1),
				//pickup after stop 1
				//pickup after stop 2
				new Insertion(drtRequest, entry, 3, 3));
	}

	@Test
	public void noDetourForPickup_noDuplicatedInsertions() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 1); // 1 pax
		Waypoint.Stop stop0 = stop(0, fromLink, 0);//dropoff 1 pax
		VehicleEntry entry = entry(start, stop0);
		assertInsertionsOnly(drtRequest, entry,
				//no pickup after start (pickup is exactly at stop0)
				//pickup after stop 0
				new Insertion(drtRequest, entry, 1, 1));
	}

	@Test
	public void noDetourForDropoff_noDuplicatedInsertions() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 1); // 1 pax
		Waypoint.Stop stop0 = stop(0, toLink, 0);//dropoff 1 pax
		VehicleEntry entry = entry(start, stop0);
		assertInsertionsOnly(drtRequest, entry,
				//pickup after start: insertion(0, 0) is a duplicate of insertion(0, 1)
				new Insertion(drtRequest, entry, 0, 1),
				//pickup after stop 0
				new Insertion(drtRequest, entry, 1, 1));
	}

	@Test
	public void noDetourForDropoff_vehicleOutgoingFullAfterDropoff_insertionPossible() {
		// a special case where we allow inserting the dropoff after a stop despite outgoingOccupancy == maxCapacity
		// this is only because the the dropoff happens exactly at (not after) the stop
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 1); // 1 pax
		Waypoint.Stop stop0 = stop(0, toLink, CAPACITY);//dropoff 1 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), 0);//dropoff 1 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertionsOnly(drtRequest, entry,
				//pickup after start: insertion(0, 0) is a duplicate of insertion(0, 1)
				new Insertion(drtRequest, entry, 0, 1),
				//pickup after stop 0
				new Insertion(drtRequest, entry, 2, 2));
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private InsertionWithDetourData insertion(Insertion insertion, PickupDetourInfo pickupDetourInfo,
			DropoffDetourInfo dropoffDetourInfo) {
		return new InsertionWithDetourData(insertion, null, new DetourTimeInfo(pickupDetourInfo, dropoffDetourInfo));
	}

	private void assertInsertionsWithDetour(DrtRequest drtRequest, VehicleEntry entry,
			List<InsertionWithDetourData> expectedInsertions) {
		int stopCount = entry.stops.size();
		int endOccupancy = stopCount > 0 ? entry.stops.get(stopCount - 1).outgoingOccupancy : entry.start.occupancy;
		Preconditions.checkArgument(endOccupancy == 0);//make sure the input is valid

		DetourTimeEstimator timeEstimator = (from, to, departureTime) -> {
			if (from == to) {
				return 0;
			} else if (to.equals(drtRequest.getFromLink())) {
				return TIME_TO_PICKUP;
			} else if (from.equals(drtRequest.getFromLink())) {
				return TIME_FROM_PICKUP;
			} else if (to.equals(drtRequest.getToLink())) {
				return TIME_TO_DROPOFF;
			} else if (from.equals(drtRequest.getToLink())) {
				return TIME_FROM_DROPOFF;
			}
			return TIME_REPLACED_DRIVE;
		};

		var actualInsertions = new InsertionGenerator(new DefaultIncrementalStopDurationEstimator(STOP_DURATION), timeEstimator).generateInsertions(drtRequest,
				entry);
		assertThat(actualInsertions).usingRecursiveFieldByFieldElementComparator()
				.containsExactlyElementsOf(expectedInsertions);
	}

	private void assertInsertionsOnly(DrtRequest drtRequest, VehicleEntry entry, Insertion... expectedInsertions) {
		int stopCount = entry.stops.size();
		int endOccupancy = stopCount > 0 ? entry.stops.get(stopCount - 1).outgoingOccupancy : entry.start.occupancy;
		Preconditions.checkArgument(endOccupancy == 0);//make sure the input is valid

		DetourTimeEstimator timeEstimator = (from, to, departureTime) -> 0;

		var actualInsertions = new InsertionGenerator(new DefaultIncrementalStopDurationEstimator(STOP_DURATION), timeEstimator).generateInsertions(drtRequest,
				entry);
		assertThat(actualInsertions.stream().map(i -> i.insertion)).usingRecursiveFieldByFieldElementComparator()
				.containsExactly(expectedInsertions);
	}

	private Waypoint.Stop stop(double beginTime, Link link, int outgoingOccupancy) {
		return new Waypoint.Stop(new DefaultDrtStopTask(beginTime, beginTime + STOP_DURATION, link), outgoingOccupancy);
	}

	private VehicleEntry entry(Waypoint.Start start, Waypoint.Stop... stops) {
		var slackTimes = new double[stops.length + 1];
		Arrays.fill(slackTimes, Double.POSITIVE_INFINITY);
		return new VehicleEntry(vehicle, start, ImmutableList.copyOf(stops), slackTimes);
	}
}
