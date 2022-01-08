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

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
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

	private static final int STOP_DURATION = 10;

	private static final double TIME_TO_PICKUP = 100;
	private static final double TIME_FROM_PICKUP = 200;
	private static final double TIME_TO_DROPOFF = 300;
	private static final double TIME_FROM_DROPOFF = 400;

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
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 0); //no stops => must be empty
		VehicleEntry entry = entry(start);
		assertInsertions(drtRequest, entry,
				//pickup after start
				insertion(new Insertion(drtRequest, entry, 0, 0),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	@Test
	public void generateInsertions_startNotFull_oneStop() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 1); // 1 pax aboard
		Waypoint.Stop stop0 = stop(99, link("stop0"), 0);//drop off 1 pax
		VehicleEntry entry = entry(start, stop0);
		assertInsertions(drtRequest, entry,
				//pickup after start
				insertion(new Insertion(drtRequest, entry, 0, 0),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY,
								TIME_FROM_DROPOFF), null),//
				insertion(new Insertion(drtRequest, entry, 0, 1),//
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, TIME_TO_DROPOFF, 0.), null),
				//pickup after stop 0
				insertion(new Insertion(drtRequest, entry, 1, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	@Test
	public void generateInsertions_startFull_oneStop() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(0, link("stop0"), 0);//drop off 4 pax
		VehicleEntry entry = entry(start, stop0);
		assertInsertions(drtRequest, entry,
				//no pickup after stop
				//pickup after stop 0
				insertion(new Insertion(drtRequest, entry, 1, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	@Test
	public void generateInsertions_startEmpty_twoStops_notFullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 0); //empty
		Waypoint.Stop stop0 = stop(0, link("stop0"), 1);//pick up 1 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), 0);//drop off 1 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertions(drtRequest, entry,
				//pickup after start
				insertion(new Insertion(drtRequest, entry, 0, 0),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY,
								TIME_FROM_DROPOFF), null),//
				insertion(new Insertion(drtRequest, entry, 0, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, TIME_TO_DROPOFF, TIME_FROM_DROPOFF),
						null),//
				insertion(new Insertion(drtRequest, entry, 0, 2),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, TIME_TO_DROPOFF, 0.), null),
				//pickup after stop 0
				insertion(new Insertion(drtRequest, entry, 1, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY,
								TIME_FROM_DROPOFF), null),//
				insertion(new Insertion(drtRequest, entry, 1, 2),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, TIME_TO_DROPOFF, 0.), null),
				//pickup after stop 1
				insertion(new Insertion(drtRequest, entry, 2, 2),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	@Test
	public void generateInsertions_startEmpty_twoStops_fullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 0); //empty
		Waypoint.Stop stop0 = stop(0, link("stop0"), CAPACITY);//pick up 4 pax (full)
		Waypoint.Stop stop1 = stop(0, link("stop1"), 0);//drop off 4 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertions(drtRequest, entry,
				//pickup after start
				insertion(new Insertion(drtRequest, entry, 0, 0),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY,
								TIME_FROM_DROPOFF), null),
				//no pickup after stop 0
				//pickup after stop 1
				insertion(new Insertion(drtRequest, entry, 2, 2),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	@Test
	public void generateInsertions_startFull_twoStops_notFullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(0, link("stop0"), 2);//drop off 2 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), 0);//drop off 2 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertions(drtRequest, entry,
				//no pickup after start
				//pickup after stop 0
				insertion(new Insertion(drtRequest, entry, 1, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY,
								TIME_FROM_DROPOFF), null),//
				insertion(new Insertion(drtRequest, entry, 1, 2),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, TIME_TO_DROPOFF, 0.), null),
				//pickup after stop 1
				insertion(new Insertion(drtRequest, entry, 2, 2),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	@Test
	public void generateInsertions_startFull_twoStops_fullBetweenStops() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(0, link("stop0"), CAPACITY);//drop off 1 pax, pickup 1 pax (full)
		Waypoint.Stop stop1 = stop(0, link("stop1"), 0);//drop off 4 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertions(drtRequest, entry,
				//no pickup after start
				//no pickup after stop 0
				//pickup after stop 1
				insertion(new Insertion(drtRequest, entry, 2, 2),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	@Test
	public void generateInsertions_startNotFull_threeStops_emptyBetweenStops01_fullBetweenStops12() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 1); //empty
		Waypoint.Stop stop0 = stop(0, link("stop0"), 0);// dropoff 1 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), CAPACITY);// pickup 4 pax
		Waypoint.Stop stop2 = stop(0, link("stop2"), 0);// dropoff 4 pax
		VehicleEntry entry = entry(start, stop0, stop1, stop2);
		assertInsertions(drtRequest, entry,
				//pickup after start
				insertion(new Insertion(drtRequest, entry, 0, 0),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY,
								TIME_FROM_DROPOFF), null),//
				insertion(new Insertion(drtRequest, entry, 0, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, TIME_TO_DROPOFF, TIME_FROM_DROPOFF),
						null),
				//pickup after stop 0
				insertion(new Insertion(drtRequest, entry, 1, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY,
								TIME_FROM_DROPOFF), null),
				//pickup after stop 1
				//pickup after stop 2
				insertion(new Insertion(drtRequest, entry, 3, 3),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	@Test
	public void generateInsertions_startFull_threeStops_emptyBetweenStops01_fullBetweenStops12() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, CAPACITY); //full
		Waypoint.Stop stop0 = stop(0, link("stop0"), 0);// dropoff 4 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), CAPACITY);// pickup 4 pax
		Waypoint.Stop stop2 = stop(0, link("stop2"), 0);// dropoff 4 pax
		VehicleEntry entry = entry(start, stop0, stop1, stop2);
		assertInsertions(drtRequest, entry,
				//no pickup after start
				//pickup after stop 0
				insertion(new Insertion(drtRequest, entry, 1, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY,
								TIME_FROM_DROPOFF), null),
				//pickup after stop 1
				//pickup after stop 2
				insertion(new Insertion(drtRequest, entry, 3, 3),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	@Test
	public void generateInsertions_noDetourForPickup_noDuplicatedInsertions() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 1); // 1 pax
		Waypoint.Stop stop0 = stop(0, fromLink, 0);//dropoff 1 pax
		VehicleEntry entry = entry(start, stop0);
		assertInsertions(drtRequest, entry,
				//no pickup after start (pickup is exactly at stop0)
				//pickup after stop 0
				insertion(new Insertion(drtRequest, entry, 1, 1),
						new InsertionDetourData<>(0., TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.), null));
	}

	@Test
	public void generateInsertions_noDetourForDropoff_noDuplicatedInsertions() {
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 1); // 1 pax
		Waypoint.Stop stop0 = stop(0, toLink, 0);//dropoff 1 pax
		VehicleEntry entry = entry(start, stop0);
		assertInsertions(drtRequest, entry,
				//pickup after start: insertion(0, 0) is a duplicate of insertion(0, 1)
				insertion(new Insertion(drtRequest, entry, 0, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, 0., 0.), null),
				//pickup after stop 0
				insertion(new Insertion(drtRequest, entry, 1, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	@Test
	public void generateInsertions_noDetourForDropoff_vehicleOutgoingFullAfterDropoff_insertionPossible() {
		// a special case where we allow inserting the dropoff after a stop despite outgoingOccupancy == maxCapacity
		// this is only because the the dropoff happens exactly at (not after) the stop
		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 1); // 1 pax
		Waypoint.Stop stop0 = stop(0, toLink, CAPACITY);//dropoff 1 pax
		Waypoint.Stop stop1 = stop(0, link("stop1"), 0);//dropoff 1 pax
		VehicleEntry entry = entry(start, stop0, stop1);
		assertInsertions(drtRequest, entry,
				//pickup after start: insertion(0, 0) is a duplicate of insertion(0, 1)
				insertion(new Insertion(drtRequest, entry, 0, 1),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, 0., TIME_FROM_DROPOFF), null),
				//pickup after stop 0
				insertion(new Insertion(drtRequest, entry, 2, 2),
						new InsertionDetourData<>(TIME_TO_PICKUP, TIME_FROM_PICKUP, Double.POSITIVE_INFINITY, 0.),
						null));
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private void assertInsertions(DrtRequest drtRequest, VehicleEntry entry,
			InsertionWithDetourData<Double>... expectedInsertions) {
		int stopCount = entry.stops.size();
		int endOccupancy = stopCount > 0 ? entry.stops.get(stopCount - 1).outgoingOccupancy : entry.start.occupancy;
		Preconditions.checkArgument(endOccupancy == 0);//make sure the input is valid

		DetourTimeEstimator timeEstimator = (from, to) -> {
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
			return 100;
		};

		var actualInsertions = new InsertionGenerator(STOP_DURATION, timeEstimator).generateInsertions(drtRequest,
				entry);
		assertThat(actualInsertions).hasSize(expectedInsertions.length);

		for (int i = 0; i < expectedInsertions.length; i++) {
			//TODO we do not compare insertion.detourTimeInfo
			assertThat(actualInsertions.get(i).getInsertion()).usingRecursiveComparison()
					.isEqualTo(expectedInsertions[i].getInsertion());
			assertThat(actualInsertions.get(i).getDetourData()).usingRecursiveComparison()
					.isEqualTo(expectedInsertions[i].getDetourData());
		}
	}

	private InsertionWithDetourData<Double> insertion(Insertion insertion, InsertionDetourData<Double> detourData,
			DetourTimeInfo detourTimeInfo) {
		return new InsertionWithDetourData<>(insertion, detourData, detourTimeInfo);
	}

	private Waypoint.Stop stop(double beginTime, Link link, int outgoingOccupancy) {
		return new Waypoint.Stop(new DefaultDrtStopTask(beginTime, beginTime + STOP_DURATION, link), outgoingOccupancy);
	}

	private VehicleEntry entry(Waypoint.Start start, Waypoint.Stop... stops) {
		return new VehicleEntry(vehicle, start, ImmutableList.copyOf(stops), null);
	}
}
