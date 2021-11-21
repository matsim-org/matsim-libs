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
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionDetourTimeCalculatorTest {
	private static final int STOP_DURATION = 10;

	private final Link fromLink = link("from");
	private final Link toLink = link("to");
	private final DrtRequest drtRequest = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).build();

	@Test
	public void detourTimeLoss_start_pickup_dropoff() {
		Waypoint.Start start = start(null, 10, link("start"));
		VehicleEntry entry = entry(start);
		var detour = new InsertionDetourData<>(100., 15., null, 0.);
		var insertion = insertion(entry, 0, 0, detour);

		double departureTime = start.getDepartureTime() + detour.detourToPickup + STOP_DURATION;
		double pickupTimeLoss = detour.detourToPickup + STOP_DURATION + detour.detourFromPickup;
		double arrivalTime = departureTime + detour.detourFromPickup;
		double dropoffTimeLoss = STOP_DURATION;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss));
	}

	@Test
	public void detourTimeLoss_ongoingStopAsStart_pickup_dropoff() {
		//similar to detourTmeLoss_start_pickup_dropoff(), but the pickup is appended to the ongoing STOP task
		Waypoint.Start start = start(new DefaultDrtStopTask(20, 20 + STOP_DURATION, fromLink), STOP_DURATION, fromLink);
		VehicleEntry entry = entry(start);
		var detour = new InsertionDetourData<>(null, 15., null, 0.);//toPickup/Dropoff unused
		var insertion = insertion(entry, 0, 0, detour);

		double departureTime = start.getDepartureTime();
		double pickupTimeLoss = detour.detourFromPickup;
		double arrivalTime = departureTime + detour.detourFromPickup;
		double dropoffTimeLoss = STOP_DURATION;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss));
	}

	@Test
	public void detourTimeLoss_start_pickup_dropoff_stop() {
		Waypoint.Start start = start(null, 5, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		VehicleEntry entry = entry(start, stop0);
		var detour = new InsertionDetourData<>(10., 30., null, 300.);//toDropoff unused
		var insertion = insertion(entry, 0, 0, detour);

		double departureTime = start.getDepartureTime() + detour.detourToPickup + STOP_DURATION;
		double pickupTimeLoss = detour.detourToPickup + STOP_DURATION + detour.detourFromPickup - timeBetween(start,
				stop0);
		double arrivalTime = departureTime + detour.detourFromPickup;
		double dropoffTimeLoss = STOP_DURATION + detour.detourFromDropoff;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss));
	}

	@Test
	public void calculatePickupDetourTimeLoss_start_pickup_stop_dropoff() {
		Waypoint.Start start = start(null, 5, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		VehicleEntry entry = entry(start, stop0);
		var detour = new InsertionDetourData<>(10., 30., 100., 0.);
		var insertion = insertion(entry, 0, 1, detour);

		double departureTime = start.getDepartureTime() + detour.detourToPickup + STOP_DURATION;
		double pickupTimeLoss = detour.detourToPickup + STOP_DURATION + detour.detourFromPickup - timeBetween(start,
				stop0);
		double arrivalTime = stop0.getDepartureTime() + pickupTimeLoss + detour.detourToDropoff;
		double dropoffTimeLoss = detour.detourToDropoff + STOP_DURATION;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss));
	}

	@Test
	public void calculatePickupDetourTimeLoss_start_pickup_stop_dropoff_stop() {
		Waypoint.Start start = start(null, 5, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		Waypoint.Stop stop1 = stop(200, link("stop1"));
		VehicleEntry entry = entry(start, stop0, stop1);
		var detour = new InsertionDetourData<>(10., 30., 100., 150.);
		var insertion = insertion(entry, 0, 1, detour);

		double departureTime = start.getDepartureTime() + detour.detourToPickup + STOP_DURATION;
		double pickupTimeLoss = detour.detourToPickup + STOP_DURATION + detour.detourFromPickup - timeBetween(start,
				stop0);
		double arrivalTime = stop0.getDepartureTime() + pickupTimeLoss + detour.detourToDropoff;
		double dropoffTimeLoss = detour.detourToDropoff + STOP_DURATION + detour.detourFromDropoff - timeBetween(stop0,
				stop1);
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss));
	}

	@Test
	public void calculatePickupDetourTimeLoss_start_pickupNotAppended_stop_dropoffAppended_stop() {
		Waypoint.Start start = start(null, 5, fromLink);//not a STOP -> pickup cannot be appended
		Waypoint.Stop stop0 = stop(10, toLink);
		Waypoint.Stop stop1 = stop(200, link("stop1"));
		VehicleEntry entry = entry(start, stop0, stop1);
		var detour = new InsertionDetourData<Double>(null, null, null, null);//all unused
		var insertion = insertion(entry, 0, 1, detour);

		double departureTime = start.getDepartureTime() + STOP_DURATION;
		double pickupTimeLoss = STOP_DURATION;
		double arrivalTime = stop0.getArrivalTime() + pickupTimeLoss;
		double dropoffTimeLoss = 0;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss));
	}

	@Test
	public void calculatePickupDetourTimeLoss_start_stop_pickupAppended_stop_dropoffAppended() {
		Waypoint.Start start = start(null, 5, link("start"));
		Waypoint.Stop stop0 = stop(10, fromLink);
		Waypoint.Stop stop1 = stop(200, toLink);
		VehicleEntry entry = entry(start, stop0, stop1);
		var detour = new InsertionDetourData<Double>(null, null, null, null);//all unused
		var insertion = insertion(entry, 1, 2, detour);

		double departureTime = stop0.getDepartureTime();
		double pickupTimeLoss = 0;
		double arrivalTime = stop1.getArrivalTime();
		double dropoffTimeLoss = 0;
		assertDetourTimeInfo(insertion,
				new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss));
	}

	@Test
	public void replacedDriveTimeEstimator() {
		Waypoint.Start start = start(null, 0, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		Waypoint.Stop stop1 = stop(200, link("stop1"));
		VehicleEntry entry = entry(start, stop0, stop1);
		var detour = new InsertionDetourData<>(10., 30., 100., 150.);
		var insertion = insertion(entry, 0, 1, detour);

		double pickupDetourReplacedDriveEstimate = 33;
		double dropoffDetourReplacedDriveEstimate = 111;
		var replacedDriveTimeEstimates = ImmutableTable.<Link, Link, Double>builder()//
				.put(start.getLink(), stop0.getLink(), pickupDetourReplacedDriveEstimate)
				.put(stop0.getLink(), stop1.getLink(), dropoffDetourReplacedDriveEstimate)
				.build();

		var detourTimeCalculator = new InsertionDetourTimeCalculator<>(STOP_DURATION, Double::doubleValue,
				replacedDriveTimeEstimates::get);
		var actualDetourTimeInfo = detourTimeCalculator.calculateDetourTimeInfo(insertion);

		double departureTime = start.getDepartureTime() + detour.detourToPickup + STOP_DURATION;
		double pickupTimeLoss = detour.detourToPickup + STOP_DURATION + detour.detourFromPickup
				- pickupDetourReplacedDriveEstimate;
		double arrivalTime = stop0.getDepartureTime() + pickupTimeLoss + detour.detourToDropoff;
		double dropoffTimeLoss = detour.detourToDropoff + STOP_DURATION + detour.detourFromDropoff
				- dropoffDetourReplacedDriveEstimate;
		assertThat(actualDetourTimeInfo).usingRecursiveComparison()
				.isEqualTo(new DetourTimeInfo(departureTime, arrivalTime, pickupTimeLoss, dropoffTimeLoss));
	}

	private void assertDetourTimeInfo(InsertionWithDetourData<Double> insertion, DetourTimeInfo expected) {
		var detourTimeCalculator = new InsertionDetourTimeCalculator<>(STOP_DURATION, Double::doubleValue, null);
		var detourTimeInfo = detourTimeCalculator.calculateDetourTimeInfo(insertion);
		assertThat(detourTimeInfo).usingRecursiveComparison().isEqualTo(expected);
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private Waypoint.Start start(Task task, double time, Link link) {
		return new Waypoint.Start(task, link, time, 0);
	}

	private Waypoint.Stop stop(double beginTime, Link link) {
		return new Waypoint.Stop(new DefaultDrtStopTask(beginTime, beginTime + STOP_DURATION, link), 0);
	}

	private VehicleEntry entry(Waypoint.Start start, Waypoint.Stop... stops) {
		return new VehicleEntry(null, start, ImmutableList.copyOf(stops), null);
	}

	private InsertionWithDetourData<Double> insertion(VehicleEntry entry, int pickupIdx, int dropoffIdx,
			InsertionDetourData<Double> detour) {
		return new InsertionWithDetourData<Double>(new Insertion(drtRequest, entry, pickupIdx, dropoffIdx), detour);
	}

	private double timeBetween(Waypoint from, Waypoint to) {
		return to.getArrivalTime() - from.getDepartureTime();
	}
}
