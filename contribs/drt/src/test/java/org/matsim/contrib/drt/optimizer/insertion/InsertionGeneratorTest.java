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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DropoffDetourInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.PickupDetourInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.stops.DefaultStopTimeCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.DvrpLoad;
import org.matsim.contrib.dvrp.load.IntegerLoad;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionGeneratorTest {
	private static final IntegerLoadType LOAD_TYPE = new IntegerLoadType("passengers");
	private static final DvrpLoad CAPACITY = LOAD_TYPE.fromInt(4);

	private static final int STOP_DURATION = 10;

	private static final double TIME_TO_PICKUP = 100;
	private static final double TIME_FROM_PICKUP = 200;//also pickup -> dropoff
	private static final double TIME_TO_DROPOFF = 300;
	private static final double TIME_FROM_DROPOFF = 400;
	private static final double TIME_REPLACED_DRIVE = 100;

	private final Link fromLink = link("from");
	private final Link toLink = link("to");
	private final DrtRequest drtRequest = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).passengerIds(List.of(Id.createPersonId("person"))).load(LOAD_TYPE.fromInt(1)).build();

	private final DrtRequest drtRequest2Pax = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).passengerIds(
			List.of(
					Id.createPersonId("person1"),
					Id.createPersonId("person2")
			)).load(LOAD_TYPE.fromInt(2)).build();

	private final DrtRequest drtRequest5Pax = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).passengerIds(
			List.of(
					Id.createPersonId("person1"),
					Id.createPersonId("person2"),
					Id.createPersonId("person3"),
					Id.createPersonId("person4"),
					Id.createPersonId("person5")
			)).load(LOAD_TYPE.fromInt(5)).build();
	private final DrtRequest prebookedRequest = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).earliestStartTime(100).load(LOAD_TYPE.getEmptyLoad()).build();

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
	void startEmpty_noStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad()); //no stops => must be empty
		VehicleEntry entry = entry(start);

		var insertions = new ArrayList<InsertionWithDetourData>();
		{//00
			var insertion = new Insertion(drtRequest, entry, 0, 0);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP);
			var dropoff = new DropoffDetourInfo(start.time + pickup.pickupTimeLoss,
				start.time + pickup.pickupTimeLoss, STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		assertInsertionsWithDetour(drtRequest, entry, insertions);
	}

	@Test
	void startNotFull_oneStop() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.fromInt(1)); // 1 pax aboard
		Waypoint.Stop stop0 = stop(start.time + TIME_REPLACED_DRIVE, link("stop0"), LOAD_TYPE.getEmptyLoad());//drop off 1 pax
		VehicleEntry entry = entry(start, stop0);

		var insertions = new ArrayList<InsertionWithDetourData>();
		{//00
			var insertion = new Insertion(drtRequest, entry, 0, 0);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(pickup.vehicleDepartureTime + TIME_FROM_PICKUP,
					pickup.vehicleDepartureTime + TIME_FROM_PICKUP,
					STOP_DURATION + TIME_FROM_DROPOFF);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//01
			var insertion = new Insertion(drtRequest, entry, 0, 1);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(stop0.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					stop0.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					TIME_TO_DROPOFF + STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//11
			var insertion = new Insertion(drtRequest, entry, 1, 1);
			var pickup = new PickupDetourInfo(stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP);
			var dropoff = new DropoffDetourInfo(stop0.getDepartureTime() + pickup.pickupTimeLoss, 
					stop0.getDepartureTime() + pickup.pickupTimeLoss, STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		assertInsertionsWithDetour(drtRequest, entry, insertions);
	}

	@Test
	void startFull_oneStop() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(start.time + TIME_REPLACED_DRIVE, link("stop0"), LOAD_TYPE.getEmptyLoad());//drop off 4 pax
		VehicleEntry entry = entry(start, stop0);

		var insertions = new ArrayList<InsertionWithDetourData>();
		{//11
			var insertion = new Insertion(drtRequest, entry, 1, 1);
			var pickup = new PickupDetourInfo(stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP);
			var dropoff = new DropoffDetourInfo(stop0.getDepartureTime() + pickup.pickupTimeLoss, 
					stop0.getDepartureTime() + pickup.pickupTimeLoss, STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		assertInsertionsWithDetour(drtRequest, entry, insertions);
	}

	@Test
	void startEmpty_twoStops_notFullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad()); //empty
		Waypoint.Stop stop0 = stop(start.time + TIME_REPLACED_DRIVE, link("stop0"), LOAD_TYPE.fromInt(1));//pick up 1 pax
		Waypoint.Stop stop1 = stop(stop0.getDepartureTime() + TIME_REPLACED_DRIVE, link("stop1"), LOAD_TYPE.getEmptyLoad());//drop off 1 pax
		VehicleEntry entry = entry(start, stop0, stop1);

		var insertions = new ArrayList<InsertionWithDetourData>();
		{//00
			var insertion = new Insertion(drtRequest, entry, 0, 0);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(pickup.vehicleDepartureTime + TIME_FROM_PICKUP,
					pickup.vehicleDepartureTime + TIME_FROM_PICKUP,
					STOP_DURATION + TIME_FROM_DROPOFF);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//01
			var insertion = new Insertion(drtRequest, entry, 0, 1);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(stop0.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					stop0.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					TIME_TO_DROPOFF + STOP_DURATION + TIME_FROM_DROPOFF - TIME_REPLACED_DRIVE);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//02
			var insertion = new Insertion(drtRequest, entry, 0, 2);
			var pickup = new PickupDetourInfo(start.time + TIME_TO_PICKUP + STOP_DURATION,
					start.time + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(stop1.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					stop1.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					TIME_TO_DROPOFF + STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//11
			var insertion = new Insertion(drtRequest, entry, 1, 1);
			var pickup = new PickupDetourInfo(stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(pickup.vehicleDepartureTime + TIME_FROM_PICKUP,
					pickup.vehicleDepartureTime + TIME_FROM_PICKUP,
					STOP_DURATION + TIME_FROM_DROPOFF);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//12
			var insertion = new Insertion(drtRequest, entry, 1, 2);
			var pickup = new PickupDetourInfo(stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(stop1.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					stop1.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					TIME_TO_DROPOFF + STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//22
			var insertion = new Insertion(drtRequest, entry, 2, 2);
			var pickup = new PickupDetourInfo(stop1.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					stop1.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP);
			var dropoff = new DropoffDetourInfo(stop1.getDepartureTime() + pickup.pickupTimeLoss, 
					stop1.getDepartureTime() + pickup.pickupTimeLoss, STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		assertInsertionsWithDetour(drtRequest, entry, insertions);
	}

	@Test
	void startEmpty_twoStops_notFullBetweenStops_tightSlackTimes() {
		//same as startEmpty_twoStops_notFullBetweenStops() but with different slack times
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad()); //empty
		Waypoint.Stop stop0 = stop(start.time + TIME_REPLACED_DRIVE, link("stop0"), LOAD_TYPE.fromInt(1));//pick up 1 pax
		Waypoint.Stop stop1 = stop(stop0.getDepartureTime() + TIME_REPLACED_DRIVE, link("stop1"), LOAD_TYPE.getEmptyLoad());//drop off 1 pax

		double[] slackTimes = { 0, 0, // impossible insertions: 00, 01, 02 (pickup at 0 is not possible)
				500, // additional impossible insertions: 11 (too long total detour); however 12 is possible
				1000 }; // 22 is possible

		List<Double> precedingStayTimes = Arrays.asList(0.0, 0.0);

		VehicleEntry entry = new VehicleEntry(vehicle, start, ImmutableList.of(stop0, stop1), slackTimes, precedingStayTimes, 0);

		var insertions = new ArrayList<InsertionWithDetourData>();
		{//12
			var insertion = new Insertion(drtRequest, entry, 1, 2);
			var pickup = new PickupDetourInfo(stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					stop0.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP - TIME_REPLACED_DRIVE);
			var dropoff = new DropoffDetourInfo(stop1.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					stop1.getDepartureTime() + pickup.pickupTimeLoss + TIME_TO_DROPOFF,
					TIME_TO_DROPOFF + STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		{//22
			var insertion = new Insertion(drtRequest, entry, 2, 2);
			var pickup = new PickupDetourInfo(stop1.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					stop1.getDepartureTime() + TIME_TO_PICKUP + STOP_DURATION,
					TIME_TO_PICKUP + STOP_DURATION + TIME_FROM_PICKUP);
			var dropoff = new DropoffDetourInfo(stop1.getDepartureTime() + pickup.pickupTimeLoss, 
					stop1.getDepartureTime() + pickup.pickupTimeLoss, STOP_DURATION);
			insertions.add(insertion(insertion, pickup, dropoff));
		}
		assertInsertionsWithDetour(drtRequest, entry, insertions);
	}

	@Test
	void startEmpty_twoStops_fullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad()); //empty
		Waypoint.Stop stop0 = stop(0, link("stop0"), CAPACITY);//pick up 4 pax (full)
		Waypoint.Stop stop1 = stop(0, link("stop1"), LOAD_TYPE.getEmptyLoad());//drop off 4 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertionsOnly(drtRequest, entry,
				//pickup after start
				new Insertion(drtRequest, entry, 0, 0),
				//no pickup after stop 0
				//pickup after stop 1
				new Insertion(drtRequest, entry, 2, 2));
	}

	@Test
	void startFull_twoStops_notFullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(0, link("stop0"), LOAD_TYPE.fromInt(2));//drop off 2 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), LOAD_TYPE.getEmptyLoad());//drop off 2 pax
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
	void startFull_twoStops_fullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(0, link("stop0"), CAPACITY);//drop off 1 pax, pickup 1 pax (full)
		Waypoint.Stop stop1 = stop(0, link("stop1"), LOAD_TYPE.getEmptyLoad());//drop off 4 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertionsOnly(drtRequest, entry,
				//no pickup after start
				//no pickup after stop 0
				//pickup after stop 1
				new Insertion(drtRequest, entry, 2, 2));
	}

	@Test
	void startNotFull_threeStops_emptyBetweenStops01_fullBetweenStops12() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.fromInt(1)); //empty
		Waypoint.Stop stop0 = stop(0, link("stop0"), LOAD_TYPE.getEmptyLoad());// dropoff 1 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), CAPACITY);// pickup 4 pax
		Waypoint.Stop stop2 = stop(0, link("stop2"), LOAD_TYPE.getEmptyLoad());// dropoff 4 pax
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
	void startFull_threeStops_emptyBetweenStops01_fullBetweenStops12() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(0, link("stop0"), LOAD_TYPE.getEmptyLoad());// dropoff 4 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), CAPACITY);// pickup 4 pax
		Waypoint.Stop stop2 = stop(0, link("stop2"), LOAD_TYPE.getEmptyLoad());// dropoff 4 pax
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
	void noDetourForPickup_noDuplicatedInsertions() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.fromInt(1)); // 1 pax
		Waypoint.Stop stop0 = stop(0, fromLink, LOAD_TYPE.getEmptyLoad());//dropoff 1 pax
		VehicleEntry entry = entry(start, stop0);
		assertInsertionsOnly(drtRequest, entry,
				//no pickup after start (pickup is exactly at stop0)
				//pickup after stop 0
				new Insertion(drtRequest, entry, 1, 1));
	}

	@Test
	void noDetourForDropoff_noDuplicatedInsertions() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.fromInt(1)); // 1 pax
		Waypoint.Stop stop0 = stop(0, toLink, LOAD_TYPE.getEmptyLoad());//dropoff 1 pax
		VehicleEntry entry = entry(start, stop0);
		assertInsertionsOnly(drtRequest, entry,
				//pickup after start: insertion(0, 0) is a duplicate of insertion(0, 1)
				new Insertion(drtRequest, entry, 0, 1),
				//pickup after stop 0
				new Insertion(drtRequest, entry, 1, 1));
	}

	@Test
	void noDetourForDropoff_vehicleOutgoingFullAfterDropoff_insertionPossible() {
		// a special case where we allow inserting the dropoff after a stop despite outgoingOccupancy == maxCapacity
		// this is only because the the dropoff happens exactly at (not after) the stop
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.fromInt(1)); // 1 pax
		Waypoint.Stop stop0 = stop(0, toLink, CAPACITY);//dropoff 1 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), LOAD_TYPE.getEmptyLoad());//dropoff 1 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertionsOnly(drtRequest, entry,
				//pickup after start: insertion(0, 0) is a duplicate of insertion(0, 1)
				new Insertion(drtRequest, entry, 0, 1),
				//pickup after stop 0
				new Insertion(drtRequest, entry, 2, 2));
	}

	@Test
	void startEmpty_prebookedRequest() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad());
		VehicleEntry entry = entry(start);
		assertInsertionsOnly(prebookedRequest, entry,
			new Insertion(prebookedRequest, entry, 0, 0));
	}

	@Test
	void startEmpty_onlineRequest_beforeAlreadyPrebookedOtherRequest() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad());
		Waypoint.Stop stop0 = stop(200, fromLink, LOAD_TYPE.fromInt(1));
		Waypoint.Stop stop1 = stop(400, link("stop"), LOAD_TYPE.getEmptyLoad());
		List<Double> precedingStayTimes = Arrays.asList(100.0, 0.0);
		VehicleEntry entry = entry(start, precedingStayTimes, stop0, stop1);
		assertInsertionsOnly(drtRequest, entry,
			new Insertion(drtRequest, entry, 0, 0),
			new Insertion(drtRequest, entry, 0, 1),
			new Insertion(drtRequest, entry, 0, 2),
			new Insertion(drtRequest, entry, 1, 1),
			new Insertion(drtRequest, entry, 1, 2),
			new Insertion(drtRequest, entry, 2, 2)
		);
	}

	@Test
	void startEmpty_prebookedRequest_inMiddleOfAlreadyPrebookedOtherRequest() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad());
		Waypoint.Stop stop0 = stop(50, fromLink, LOAD_TYPE.fromInt(1));
		Waypoint.Stop stop1 = stop(300, link("stop"), LOAD_TYPE.getEmptyLoad());
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertionsOnly(prebookedRequest, entry,
			new Insertion(prebookedRequest, entry, 1, 1),
			new Insertion(prebookedRequest, entry, 1, 2),
			new Insertion(prebookedRequest, entry, 2, 2));
	}

	@Test
	void startEmpty_prebookedRequest_afterAlreadyPrebookedOtherRequest() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad());
		Waypoint.Stop stop0 = stop(20, fromLink, LOAD_TYPE.fromInt(1));
		Waypoint.Stop stop1 = stop(70, link("stop"), LOAD_TYPE.getEmptyLoad());
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertionsOnly(prebookedRequest, entry,
			new Insertion(prebookedRequest, entry, 2, 2));
	}


	@Test
	void startEmpty_smallGroup() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad()); //empty
		VehicleEntry entry = entry(start);
		assertInsertionsOnly(drtRequest2Pax, entry,
				//pickup after start
				new Insertion(drtRequest2Pax, entry, 0, 0));
	}

	@Test
	void startEmpty_groupExceedsCapacity() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad()); //empty
		VehicleEntry entry = entry(start);
		assertInsertionsOnly(drtRequest5Pax, entry
				//no insertion possible
		);
	}

	@Test
	void startEmpty_twoStops_groupExceedsCapacityAtFirstStop() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, LOAD_TYPE.getEmptyLoad()); //empty
		Waypoint.Stop stop0 = stop(0, toLink, LOAD_TYPE.fromInt(3));//dropoff 1 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), LOAD_TYPE.getEmptyLoad());//dropoff 1 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertionsOnly(drtRequest2Pax, entry,
				//pickup after start:
				new Insertion(drtRequest2Pax, entry, 0, 1),
				//pickup after stop 1
				new Insertion(drtRequest2Pax, entry, 2, 2)
				);
	}

	@Test
	void testWaypointOccupancyChange() {
		IntegerLoad occupancy = LOAD_TYPE.getEmptyLoad();
		AcceptedDrtRequest acceptedReq5Pax = AcceptedDrtRequest.createFromOriginalRequest(drtRequest5Pax);
		AcceptedDrtRequest acceptedReq2Pax = AcceptedDrtRequest.createFromOriginalRequest(drtRequest2Pax);

		Waypoint.Stop stop2 = stop(0, link("stop2"), occupancy);
		//dropoff 5 pax
		stop2.task.addDropoffRequest(acceptedReq5Pax);
		occupancy = occupancy.subtract(stop2.getOccupancyChange());
		Assertions.assertEquals(5, occupancy.getValue());

		Waypoint.Stop stop1 = stop(0, link("stop1"), occupancy);
		//dropoff 2 pax, pickup 5
		stop1.task.addDropoffRequest(acceptedReq2Pax);
		stop1.task.addPickupRequest(acceptedReq5Pax);
		occupancy = occupancy.subtract(stop1.getOccupancyChange());
		Assertions.assertEquals(2, occupancy.getValue());


		Waypoint.Stop stop0 = stop(0, link("stop0"), occupancy);
		stop0.task.addPickupRequest(acceptedReq2Pax);
		occupancy = occupancy.subtract(stop0.getOccupancyChange());
		Assertions.assertEquals(0, occupancy.getValue());
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
		DvrpLoad endOccupancy = stopCount > 0 ? entry.stops.get(stopCount - 1).outgoingOccupancy : entry.start.occupancy;
		Preconditions.checkArgument(endOccupancy.isEmpty());//make sure the input is valid

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

		var actualInsertions = new InsertionGenerator(new DefaultStopTimeCalculator(STOP_DURATION), timeEstimator).generateInsertions(drtRequest,
				entry);
		assertThat(actualInsertions).usingRecursiveFieldByFieldElementComparator()
				.containsExactlyElementsOf(expectedInsertions);
	}

	private void assertInsertionsOnly(DrtRequest drtRequest, VehicleEntry entry, Insertion... expectedInsertions) {
		int stopCount = entry.stops.size();
		DvrpLoad endOccupancy = stopCount > 0 ? entry.stops.get(stopCount - 1).outgoingOccupancy : entry.start.occupancy;
		Preconditions.checkArgument(endOccupancy.isEmpty());//make sure the input is valid

		DetourTimeEstimator timeEstimator = (from, to, departureTime) -> 0;

		var actualInsertions = new InsertionGenerator(new DefaultStopTimeCalculator(STOP_DURATION), timeEstimator).generateInsertions(drtRequest,
				entry);
		assertThat(actualInsertions.stream().map(i -> i.insertion)).usingRecursiveFieldByFieldElementComparator()
				.containsExactly(expectedInsertions);
	}

	private Waypoint.Stop stop(double beginTime, Link link, DvrpLoad outgoingOccupancy) {
		return new Waypoint.Stop(new DefaultDrtStopTask(beginTime, beginTime + STOP_DURATION, link), outgoingOccupancy, LOAD_TYPE);
	}

	private VehicleEntry entry(Waypoint.Start start, Waypoint.Stop... stops) {
		List<Double> precedingStayTimes = Collections.nCopies(stops.length, 0.0);
		return entry(start, precedingStayTimes, stops);
	}

	private VehicleEntry entry(Waypoint.Start start, List<Double> precedingStayTimes, Waypoint.Stop... stops) {
		var slackTimes = new double[stops.length + 2];
		Arrays.fill(slackTimes, Double.POSITIVE_INFINITY);
		return new VehicleEntry(vehicle, start, ImmutableList.copyOf(stops), slackTimes, precedingStayTimes, 0);
	}
}
