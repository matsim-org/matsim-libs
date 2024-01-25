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

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.stops.CumulativeStopTimeCalculator;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;

/**
 * @author Michal Maciejewski (michalm)
 * @author Sebastian Hörl (sebhoerl)
 * @author nkuehnel
 */
public class InsertionDetourTimeCalculatorWithVariableDurationTest {
	private final static Link fromLink = link("from");
	private final static Link toLink = link("to");

	private static final DrtRequest drtRequestInitial = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).build();
	private static final DrtRequest drtRequestAdded = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).build();

	private static final int STOP_DURATION_INITIAL = 10;
	private static final int STOP_DURATION_ADDED = 5;
	
	public static final PassengerStopDurationProvider STOP_DURATION_PROVIDER = new PassengerStopDurationProvider() {
		@Override
		public double calcPickupDuration(DvrpVehicle vehicle, DrtRequest request) {
			if (request.equals(drtRequestInitial)) {
				return STOP_DURATION_INITIAL;
			} else if (request.equals(drtRequestAdded)) {
				return STOP_DURATION_ADDED;
			}
			
			throw new IllegalStateException();
		}

		@Override
		public double calcDropoffDuration(DvrpVehicle vehicle, DrtRequest request) {
			if (request.equals(drtRequestInitial)) {
				return STOP_DURATION_INITIAL;
			} else if (request.equals(drtRequestAdded)) {
				return STOP_DURATION_ADDED;
			}
			
			throw new IllegalStateException();
		}
	};
	
	public static final StopTimeCalculator STOP_TIME_CALCULATOR = 
			new CumulativeStopTimeCalculator(STOP_DURATION_PROVIDER);

	@Test
	void detourTimeLoss_start_pickup_dropoff() {
		Waypoint.Start start = start(null, 10, link("start"));
		VehicleEntry entry = entry(start);
		var detour = new Detour(100., 15., 0., 0.);
		var insertion = insertion(entry, 0, 0, detour);

		double departureTime = start.getDepartureTime() + detour.toPickup + STOP_DURATION_ADDED;
		double pickupTimeLoss = detour.toPickup + STOP_DURATION_ADDED + detour.fromPickup;
		double arrivalTime = departureTime + detour.fromPickup;
		double dropoffTimeLoss = STOP_DURATION_ADDED;


		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(new InsertionDetourTimeCalculator.PickupDetourInfo(departureTime, pickupTimeLoss),
						new InsertionDetourTimeCalculator.DropoffDetourInfo(arrivalTime, dropoffTimeLoss)));
	}

	@Test
	void detourTimeLoss_ongoingStopAsStart_pickup_dropoff() {
		//similar to detourTmeLoss_start_pickup_dropoff(), but the pickup is appended to the ongoing STOP task
		DrtStopTask stopTask = new DefaultDrtStopTask(20, 20 + STOP_DURATION_INITIAL, fromLink);
		stopTask.addDropoffRequest(AcceptedDrtRequest.createFromOriginalRequest(drtRequestInitial));
		// sh 03/08/23: Updated this test, according to VehicleDataEntryFactoryImpl start time should be task end time
		Waypoint.Start start = start(stopTask, 20 + STOP_DURATION_INITIAL, fromLink);
		VehicleEntry entry = entry(start);
		var detour = new Detour(0., 15., 0., 0.);//toPickup/Dropoff unused
		var insertion = insertion(entry, 0, 0, detour);

		double departureTime = start.getDepartureTime() + STOP_DURATION_ADDED;
		double pickupTimeLoss = detour.fromPickup + STOP_DURATION_ADDED;
		double arrivalTime = departureTime + detour.fromPickup;
		double dropoffTimeLoss = STOP_DURATION_ADDED;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(new InsertionDetourTimeCalculator.PickupDetourInfo(departureTime, pickupTimeLoss),
						new InsertionDetourTimeCalculator.DropoffDetourInfo(arrivalTime, dropoffTimeLoss)));
	}

	@Test
	void detourTimeLoss_start_pickup_dropoff_stop() {
		Waypoint.Start start = start(null, 5, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		VehicleEntry entry = entry(start, stop0);
		var detour = new Detour(10., 30., 0., 300.);//toDropoff unused
		var insertion = insertion(entry, 0, 0, detour);

		double departureTime = start.getDepartureTime() + detour.toPickup + STOP_DURATION_ADDED;
		double pickupTimeLoss = detour.toPickup + STOP_DURATION_ADDED + detour.fromPickup - timeBetween(start, stop0);
		double arrivalTime = departureTime + detour.fromPickup;
		double dropoffTimeLoss = STOP_DURATION_ADDED + detour.fromDropoff;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(new InsertionDetourTimeCalculator.PickupDetourInfo(departureTime, pickupTimeLoss),
						new InsertionDetourTimeCalculator.DropoffDetourInfo(arrivalTime, dropoffTimeLoss)));
	}

	@Test
	void calculatePickupDetourTimeLoss_start_pickup_stop_dropoff() {
		Waypoint.Start start = start(null, 5, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		VehicleEntry entry = entry(start, stop0);
		var detour = new Detour(10., 30., 100., 0.);
		var insertion = insertion(entry, 0, 1, detour);

		double departureTime = start.getDepartureTime() + detour.toPickup + STOP_DURATION_ADDED;
		double pickupTimeLoss = detour.toPickup + STOP_DURATION_ADDED + detour.fromPickup - timeBetween(start, stop0);
		double arrivalTime = stop0.getDepartureTime() + pickupTimeLoss + detour.toDropoff;
		double dropoffTimeLoss = detour.toDropoff + STOP_DURATION_ADDED;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(new InsertionDetourTimeCalculator.PickupDetourInfo(departureTime, pickupTimeLoss),
						new InsertionDetourTimeCalculator.DropoffDetourInfo(arrivalTime, dropoffTimeLoss)));
	}

	@Test
	void calculatePickupDetourTimeLoss_start_pickup_stop_dropoff_stop() {
		Waypoint.Start start = start(null, 5, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		Waypoint.Stop stop1 = stop(200, link("stop1"));
		VehicleEntry entry = entry(start, stop0, stop1);
		var detour = new Detour(10., 30., 100., 150.);
		var insertion = insertion(entry, 0, 1, detour);

		double departureTime = start.getDepartureTime() + detour.toPickup + STOP_DURATION_ADDED;
		double pickupTimeLoss = detour.toPickup + STOP_DURATION_ADDED + detour.fromPickup - timeBetween(start, stop0);
		double arrivalTime = stop0.getDepartureTime() + pickupTimeLoss + detour.toDropoff;
		double dropoffTimeLoss = detour.toDropoff + STOP_DURATION_ADDED + detour.fromDropoff - timeBetween(stop0, stop1);
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(new InsertionDetourTimeCalculator.PickupDetourInfo(departureTime, pickupTimeLoss),
						new InsertionDetourTimeCalculator.DropoffDetourInfo(arrivalTime, dropoffTimeLoss)));
	}

	@Test
	void calculatePickupDetourTimeLoss_start_pickupNotAppended_stop_dropoffAppended_stop() {
		Waypoint.Start start = start(null, 5, fromLink);//not a STOP -> pickup cannot be appended
		Waypoint.Stop stop0 = stop(10, toLink);
		Waypoint.Stop stop1 = stop(200, link("stop1"));
		VehicleEntry entry = entry(start, stop0, stop1);
		var detour = new Detour(0., 0., 0., 0.);//all unused
		var insertion = insertion(entry, 0, 1, detour);

		double departureTime = start.getDepartureTime() + STOP_DURATION_ADDED;
		double pickupTimeLoss = STOP_DURATION_ADDED;
		double arrivalTime = stop0.getArrivalTime() + pickupTimeLoss;
		double dropoffTimeLoss = STOP_DURATION_ADDED;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(new InsertionDetourTimeCalculator.PickupDetourInfo(departureTime, pickupTimeLoss),
						new InsertionDetourTimeCalculator.DropoffDetourInfo(arrivalTime, dropoffTimeLoss)));
	}

	@Test
	void calculatePickupDetourTimeLoss_start_stop_pickupAppended_stop_dropoffAppended() {
		Waypoint.Start start = start(null, 5, link("start"));
		Waypoint.Stop stop0 = stop(10, fromLink);
		Waypoint.Stop stop1 = stop(200, toLink);
		VehicleEntry entry = entry(start, stop0, stop1);
		var detour = new Detour(0., 0., 0., 0.);//all unused
		var insertion = insertion(entry, 1, 2, detour);

		double departureTime = stop0.getDepartureTime() + STOP_DURATION_ADDED;
		double pickupTimeLoss = STOP_DURATION_ADDED;
		double arrivalTime = stop1.getArrivalTime() + STOP_DURATION_ADDED;
		double dropoffTimeLoss = STOP_DURATION_ADDED;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(new InsertionDetourTimeCalculator.PickupDetourInfo(departureTime, pickupTimeLoss),
						new InsertionDetourTimeCalculator.DropoffDetourInfo(arrivalTime, dropoffTimeLoss)));
	}

	@Test
	void replacedDriveTimeEstimator() {
		Waypoint.Start start = start(null, 0, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		Waypoint.Stop stop1 = stop(200, link("stop1"));
		VehicleEntry entry = entry(start, stop0, stop1);
		var detour = new Detour(10., 30., 100., 150.);
		var insertion = insertion(entry, 0, 1, detour);

		double pickupDetourReplacedDriveEstimate = 33;
		double dropoffDetourReplacedDriveEstimate = 111;
		var replacedDriveTimeEstimates = ImmutableTable.<Link, Link, Double>builder()//
				.put(start.getLink(), stop0.getLink(), pickupDetourReplacedDriveEstimate)
				.put(stop0.getLink(), stop1.getLink(), dropoffDetourReplacedDriveEstimate)
				.build();

		var detourTimeCalculator = new InsertionDetourTimeCalculator(STOP_TIME_CALCULATOR,
				new DetourTimeEstimator() {
					@Override
					public double estimateTime(Link from, Link to, double departureTime) {
						return replacedDriveTimeEstimates.get(from, to);
					}
				});
		var actualDetourTimeInfo = detourTimeCalculator.calculateDetourTimeInfo(insertion.insertion, insertion.detourData, drtRequestInitial);

		double departureTime = start.getDepartureTime() + detour.toPickup + STOP_DURATION_INITIAL;
		double pickupTimeLoss = detour.toPickup + STOP_DURATION_INITIAL + detour.fromPickup - pickupDetourReplacedDriveEstimate;
		double arrivalTime = stop0.getDepartureTime() + pickupTimeLoss + detour.toDropoff;
		double dropoffTimeLoss = detour.toDropoff + STOP_DURATION_INITIAL + detour.fromDropoff
				- dropoffDetourReplacedDriveEstimate;
		DetourTimeInfo detourTimeInfo = new DetourTimeInfo(new InsertionDetourTimeCalculator.PickupDetourInfo(departureTime, pickupTimeLoss),
				new InsertionDetourTimeCalculator.DropoffDetourInfo(arrivalTime, dropoffTimeLoss));
		assertThat(actualDetourTimeInfo.pickupDetourInfo).isEqualToComparingFieldByField(
				detourTimeInfo.pickupDetourInfo);
		assertThat(actualDetourTimeInfo.dropoffDetourInfo).isEqualToComparingFieldByField(
				detourTimeInfo.dropoffDetourInfo);
	}

	private void assertDetourTimeInfo(InsertionWithDetourData insertion, DetourTimeInfo expected) {
		var detourTimeCalculator = new InsertionDetourTimeCalculator(STOP_TIME_CALCULATOR, null);
		DetourTimeInfo detourTimeInfo = detourTimeCalculator.calculateDetourTimeInfo(insertion.insertion, insertion.detourData, drtRequestAdded);
		assertThat(detourTimeInfo.pickupDetourInfo).isEqualToComparingFieldByField(expected.pickupDetourInfo);
		assertThat(detourTimeInfo.dropoffDetourInfo).isEqualToComparingFieldByField(expected.dropoffDetourInfo);
	}

	private static Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private Waypoint.Start start(Task task, double time, Link link) {
		return new Waypoint.Start(task, link, time, 0);
	}

	private Waypoint.Stop stop(double beginTime, Link link) {
		DrtStopTask stopTask = new DefaultDrtStopTask(beginTime, beginTime + STOP_DURATION_INITIAL, link);
		stopTask.addPickupRequest(AcceptedDrtRequest.createFromOriginalRequest(drtRequestInitial));
		return new Waypoint.Stop(stopTask, 0);
	}

	private VehicleEntry entry(Waypoint.Start start, Waypoint.Stop... stops) {
		List<Double> precedingStayTimes = Collections.nCopies(stops.length, 0.0);
		return new VehicleEntry(null, start, ImmutableList.copyOf(stops), null, precedingStayTimes, 0);
	}

	private InsertionWithDetourData insertion(VehicleEntry entry, int pickupIdx, int dropoffIdx,
													  Detour detour) {
		Insertion insertion = new Insertion(drtRequestInitial, entry, pickupIdx, dropoffIdx);
		InsertionWithDetourData.InsertionDetourData insertionDetourData = new InsertionWithDetourData.InsertionDetourData(getPath(detour.toPickup),
				getPath(detour.fromPickup), getPath(detour.toDropoff), getPath(detour.fromDropoff));

		DetourTimeInfo detourTimeInfo = new DetourTimeInfo(new InsertionDetourTimeCalculator.PickupDetourInfo(0, detour.fromPickup + detour.toPickup),
				new InsertionDetourTimeCalculator.DropoffDetourInfo(0, detour.fromDropoff + detour.toDropoff));
		return new InsertionWithDetourData(insertion, insertionDetourData, detourTimeInfo);
	}

	private OneToManyPathSearch.PathData getPath(double detour) {
		return new OneToManyPathSearch.PathData(new LeastCostPathCalculator.Path(null, Collections.EMPTY_LIST, detour, detour), 0);
	}

	private static class Detour {
		private final Double toPickup;
		private final Double fromPickup;
		private final Double toDropoff;
		private final Double fromDropoff;

		private Detour(Double toPickup, Double fromPickup, Double toDropoff, Double fromDropoff) {
			this.toPickup = toPickup;
			this.fromPickup = fromPickup;
			this.toDropoff = toDropoff;
			this.fromDropoff = fromDropoff;
		}
	}

	private double timeBetween(Waypoint from, Waypoint to) {
		return to.getArrivalTime() - from.getDepartureTime();
	}
}
