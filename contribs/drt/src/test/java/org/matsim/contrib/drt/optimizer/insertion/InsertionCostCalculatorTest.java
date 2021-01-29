/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.DiscourageSoftConstraintViolations.MAX_TRAVEL_TIME_VIOLATION_PENALTY;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.DiscourageSoftConstraintViolations.MAX_WAIT_TIME_VIOLATION_PENALTY;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.*;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.*;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionCostCalculatorTest {
	private final Link fromLink = link("from");
	private final Link toLink = link("to");
	private final DrtRequest drtRequest = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).build();

	private final Waypoint.Start start = start(null, 0, link("start"));

	//time slack: 20 (arrival is the constraint)
	private final Waypoint.Stop stop0 = stop(100, 120, 200, 230);

	//time slack: 30 (departure is the constraint)
	private final Waypoint.Stop stop1 = stop(300, 340, 400, 430);

	@Test
	public void checkTimeConstraintsForScheduledRequests_start_pickup_stop_dropoff_stop() {
		VehicleData.Entry entry = entry(start, stop0, stop1);
		var insertion = insertion(entry, 0, 1);

		//almost too late
		assertThat(checkTimeConstraintsForScheduledRequests(insertion, 20, 30)).isTrue();

		//pickup too late
		assertThat(checkTimeConstraintsForScheduledRequests(insertion, 21, 30)).isFalse();

		//dropoff too late
		assertThat(checkTimeConstraintsForScheduledRequests(insertion, 20, 31)).isFalse();
	}

	@Test
	public void checkTimeConstraintsForScheduledRequests_start_pickup_dropoff_stop_stop() {
		VehicleData.Entry entry = entry(start, stop0, stop1);
		var insertion = insertion(entry, 0, 0);

		//almost too late
		assertThat(checkTimeConstraintsForScheduledRequests(insertion, 19, 20)).isTrue();

		//pickup & dropoff too late
		assertThat(checkTimeConstraintsForScheduledRequests(insertion, 21, 22)).isFalse();

		//dropoff too late
		assertThat(checkTimeConstraintsForScheduledRequests(insertion, 20, 21)).isFalse();
	}

	@Test
	public void checkTimeConstraintsForScheduledRequests_start_stop_stop_pickup_dropoff() {
		VehicleData.Entry entry = entry(start, stop0, stop1);
		var insertion = insertion(entry, 2, 2);

		//appended at the end -> never too late
		assertThat(checkTimeConstraintsForScheduledRequests(insertion, 9999, 9999)).isTrue();
	}

	@Test
	public void calcVehicleSlackTime_all_cases() {
		//we are ahead of time
		assertThat(calcVehicleSlackTime(entry(1000, 600), 0)).isEqualTo(400);

		//at least 150 s of delay, optimistically 250 s of slack
		assertThat(calcVehicleSlackTime(entry(1000, 600), 750)).isEqualTo(250);

		//behind the vehicle end time - due to current time
		assertThat(calcVehicleSlackTime(entry(1000, 990), 1111)).isEqualTo(-111);

		//behind the vehicle end time - due to predicted last task end time
		assertThat(calcVehicleSlackTime(entry(1000, 1234), 888)).isEqualTo(-234);

	}

	private VehicleData.Entry entry(double vehicleEndTime, double lastStayTaskBeginTime) {
		var vehicle = new DvrpVehicleImpl(ImmutableDvrpVehicleSpecification.newBuilder()
				.id(Id.create("a", DvrpVehicle.class))
				.startLinkId(Id.createLinkId("depot"))
				.capacity(0)
				.serviceBeginTime(0)
				.serviceEndTime(vehicleEndTime)
				.build(), link("depot"));
		vehicle.getSchedule()
				.addTask(new DrtStayTask(lastStayTaskBeginTime, Math.max(lastStayTaskBeginTime, vehicleEndTime),
						link("depot")));
		return new VehicleData.Entry(vehicle, start, ImmutableList.of());
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private Waypoint.Start start(Task task, double time, Link link) {
		return new Waypoint.Start(task, link, time, 0);
	}

	private Waypoint.Stop stop(double beginTime, double latestArrivalTime, double endTime, double latestDepartureTime) {
		return new Waypoint.Stop(new DrtStopTask(beginTime, endTime, null), latestArrivalTime, latestDepartureTime, 0);
	}

	private VehicleData.Entry entry(Waypoint.Start start, Waypoint.Stop... stops) {
		return new VehicleData.Entry(null, start, ImmutableList.copyOf(stops));
	}

	private InsertionGenerator.Insertion insertion(VehicleData.Entry entry, int pickupIdx, int dropoffIdx) {
		return new InsertionGenerator.Insertion(drtRequest, entry, pickupIdx, dropoffIdx);
	}

	@Test
	public void RejectSoftConstraintViolations_tooLittleSlackTime() {
		assertRejectSoftConstraintViolations(9999, 9999, 10, new DetourTimeInfo(0, 0, 5, 5.01),
				INFEASIBLE_SOLUTION_COST);
	}

	@Test
	public void RejectSoftConstraintViolations_tooLongWaitTime() {
		assertRejectSoftConstraintViolations(10, 9999, 9999, new DetourTimeInfo(11, 22, 0, 0),
				INFEASIBLE_SOLUTION_COST);
	}

	@Test
	public void RejectSoftConstraintViolations_tooLongTravelTime() {
		assertRejectSoftConstraintViolations(9999, 10, 9999, new DetourTimeInfo(0, 11, 0, 0), INFEASIBLE_SOLUTION_COST);
	}

	@Test
	public void RejectSoftConstraintViolations_allConstraintSatisfied() {
		assertRejectSoftConstraintViolations(9999, 9999, 9999, new DetourTimeInfo(11, 22, 33, 44), 33 + 44);
	}

	private void assertRejectSoftConstraintViolations(double latestStartTime, double latestArrivalTime,
			double vehicleSlackTime, DetourTimeInfo detourTimeInfo, double expectedCost) {
		var drtRequest = DrtRequest.newBuilder()
				.latestStartTime(latestStartTime)
				.latestArrivalTime(latestArrivalTime)
				.build();
		assertThat(new InsertionCostCalculator.RejectSoftConstraintViolations().calcCost(drtRequest, null,
				vehicleSlackTime, detourTimeInfo)).isEqualTo(expectedCost);
	}

	@Test
	public void DiscourageSoftConstraintViolations_tooLittleSlackTime() {
		assertDiscourageSoftConstraintViolations(9999, 9999, 10, new DetourTimeInfo(0, 0, 5, 5.01),
				INFEASIBLE_SOLUTION_COST);
	}

	@Test
	public void DiscourageSoftConstraintViolations_tooLongWaitTime() {
		assertDiscourageSoftConstraintViolations(10, 9999, 9999, new DetourTimeInfo(11, 22, 0, 0),
				MAX_WAIT_TIME_VIOLATION_PENALTY);
	}

	@Test
	public void DiscourageSoftConstraintViolations_tooLongTravelTime() {
		assertDiscourageSoftConstraintViolations(9999, 10, 9999, new DetourTimeInfo(0, 11, 0, 0),
				MAX_TRAVEL_TIME_VIOLATION_PENALTY);
	}

	@Test
	public void DiscourageSoftConstraintViolations_allConstraintSatisfied() {
		assertDiscourageSoftConstraintViolations(9999, 9999, 9999, new DetourTimeInfo(11, 22, 33, 44), 33 + 44);
	}

	private void assertDiscourageSoftConstraintViolations(double latestStartTime, double latestArrivalTime,
			double vehicleSlackTime, DetourTimeInfo detourTimeInfo, double expectedCost) {
		var drtRequest = DrtRequest.newBuilder()
				.latestStartTime(latestStartTime)
				.latestArrivalTime(latestArrivalTime)
				.build();
		assertThat(new InsertionCostCalculator.DiscourageSoftConstraintViolations().calcCost(drtRequest, null,
				vehicleSlackTime, detourTimeInfo)).isEqualTo(expectedCost);
	}

}
