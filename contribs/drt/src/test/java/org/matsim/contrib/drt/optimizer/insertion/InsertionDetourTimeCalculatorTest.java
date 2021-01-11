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
import org.matsim.contrib.drt.optimizer.VehicleData.Entry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;

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
		Waypoint.Start start = start(null, 0, link("start"));
		Entry entry = entry(start);
		var detour = new Detour(100., 15., null, 0.);
		var insertion = insertion(entry, 0, 0, detour);

		assertPickupDetourTimeLoss(insertion, detour.toPickup + STOP_DURATION + detour.fromPickup);
		assertDropoffDetourTimeLoss(insertion, STOP_DURATION);
	}

	@Test
	public void detourTimeLoss_ongoingStopAsStart_pickup_dropoff() {
		//similar to detourTmeLoss_start_pickup_dropoff(), but the pickup is appended to the ongoing STOP task
		Waypoint.Start start = start(new DrtStopTask(0, STOP_DURATION, fromLink), STOP_DURATION, fromLink);
		Entry entry = entry(start);
		var detour = new Detour(null, 15., null, 0.);//toPickup/Dropoff unused
		var insertion = insertion(entry, 0, 0, detour);

		assertPickupDetourTimeLoss(insertion, detour.fromPickup);
		assertDropoffDetourTimeLoss(insertion, STOP_DURATION);
	}

	@Test
	public void detourTimeLoss_start_pickup_dropoff_stop() {
		Waypoint.Start start = start(null, 0, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		Entry entry = entry(start, stop0);
		var detour = new Detour(10., 30., null, 300.);//toDropoff unused
		var insertion = insertion(entry, 0, 0, detour);

		assertPickupDetourTimeLoss(insertion,
				detour.toPickup + STOP_DURATION + detour.fromPickup - timeBetween(start, stop0));
		assertDropoffDetourTimeLoss(insertion, STOP_DURATION + detour.fromDropoff);
	}

	@Test
	public void calculatePickupDetourTimeLoss_start_pickup_stop_dropoff() {
		Waypoint.Start start = start(null, 0, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		Entry entry = entry(start, stop0);
		var detour = new Detour(10., 30., 100., 0.);
		var insertion = insertion(entry, 0, 1, detour);

		assertPickupDetourTimeLoss(insertion,
				detour.toPickup + STOP_DURATION + detour.fromPickup - timeBetween(start, stop0));
		assertDropoffDetourTimeLoss(insertion, detour.toDropoff + STOP_DURATION);
	}

	@Test
	public void calculatePickupDetourTimeLoss_start_pickup_stop_dropoff_stop() {
		Waypoint.Start start = start(null, 0, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		Waypoint.Stop stop1 = stop(200, link("stop1"));
		Entry entry = entry(start, stop0, stop1);
		var detour = new Detour(10., 30., 100., 150.);
		var insertion = insertion(entry, 0, 1, detour);

		assertPickupDetourTimeLoss(insertion,
				detour.toPickup + STOP_DURATION + detour.fromPickup - timeBetween(start, stop0));
		assertDropoffDetourTimeLoss(insertion,
				detour.toDropoff + STOP_DURATION + detour.fromDropoff - timeBetween(stop0, stop1));
	}

	@Test
	public void calculatePickupDetourTimeLoss_start_pickupNotAppended_stop_dropoffAppended_stop() {
		Waypoint.Start start = start(null, 0, fromLink);//not a STOP -> pickup cannot be appended
		Waypoint.Stop stop0 = stop(10, toLink);
		Waypoint.Stop stop1 = stop(200, link("stop1"));
		Entry entry = entry(start, stop0, stop1);
		var detour = new Detour(null, null, null, null);//all unused
		var insertion = insertion(entry, 0, 1, detour);

		assertPickupDetourTimeLoss(insertion, STOP_DURATION);
		assertDropoffDetourTimeLoss(insertion, 0);
	}

	@Test
	public void calculatePickupDetourTimeLoss_start_stop_pickupAppended_stop_dropoffAppended() {
		Waypoint.Start start = start(null, 0, link("start"));
		Waypoint.Stop stop0 = stop(10, fromLink);
		Waypoint.Stop stop1 = stop(200, toLink);
		Entry entry = entry(start, stop0, stop1);
		var detour = new Detour(null, null, null, null);//all unused
		var insertion = insertion(entry, 1, 2, detour);

		assertPickupDetourTimeLoss(insertion, 0);
		assertDropoffDetourTimeLoss(insertion, 0);
	}

	@Test
	public void replacedDriveTimeEstimator() {
		Waypoint.Start start = start(null, 0, link("start"));
		Waypoint.Stop stop0 = stop(10, link("stop0"));
		Waypoint.Stop stop1 = stop(200, link("stop1"));
		Entry entry = entry(start, stop0, stop1);
		var detour = new Detour(10., 30., 100., 150.);
		var insertion = insertion(entry, 0, 1, detour);

		double replacedDriveTimePickup = 33;
		var detourTimeCalculatorPickup = new InsertionDetourTimeCalculator<>(STOP_DURATION, Double::doubleValue,
				(from, to) -> replacedDriveTimePickup);
		assertThat(detourTimeCalculatorPickup.calculatePickupDetourTimeLoss(insertion)).isEqualTo(
				detour.toPickup + STOP_DURATION + detour.fromPickup - replacedDriveTimePickup);

		double replacedDriveTimeDropoff = 111;
		var detourTimeCalculatorDropoff = new InsertionDetourTimeCalculator<>(STOP_DURATION, Double::doubleValue,
				(from, to) -> replacedDriveTimeDropoff);
		assertThat(detourTimeCalculatorDropoff.calculateDropoffDetourTimeLoss(insertion)).isEqualTo(
				detour.toDropoff + STOP_DURATION + detour.fromDropoff - replacedDriveTimeDropoff);
	}

	private void assertPickupDetourTimeLoss(InsertionWithDetourData<Double> insertion, double expected) {
		var detourTimeCalculator = new InsertionDetourTimeCalculator<>(STOP_DURATION, Double::doubleValue, null);
		assertThat(detourTimeCalculator.calculatePickupDetourTimeLoss(insertion)).isEqualTo(expected);
	}

	private void assertDropoffDetourTimeLoss(InsertionWithDetourData<Double> insertion, double expected) {
		var detourTimeCalculator = new InsertionDetourTimeCalculator<>(STOP_DURATION, Double::doubleValue, null);
		assertThat(detourTimeCalculator.calculateDropoffDetourTimeLoss(insertion)).isEqualTo(expected);
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private Waypoint.Start start(Task task, double time, Link link) {
		return new Waypoint.Start(task, link, time, 0);
	}

	private Waypoint.Stop stop(double beginTime, Link link) {
		return new Waypoint.Stop(new DrtStopTask(beginTime, beginTime + STOP_DURATION, link), 0);
	}

	private Entry entry(Waypoint.Start start, Waypoint.Stop... stops) {
		return new Entry(null, start, ImmutableList.copyOf(stops));
	}

	private InsertionWithDetourData<Double> insertion(Entry entry, int pickupIdx, int dropoffIdx, Detour detour) {
		return new InsertionWithDetourData<>(new Insertion(drtRequest, entry, pickupIdx, dropoffIdx), detour.toPickup,
				detour.fromPickup, detour.toDropoff, detour.fromDropoff);
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
