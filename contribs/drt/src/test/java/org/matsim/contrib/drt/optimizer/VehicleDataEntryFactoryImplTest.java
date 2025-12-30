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

package org.matsim.contrib.drt.optimizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl.computeSlackTimes;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.testcases.fakes.FakeLink;

/**
 * @author Michal Maciejewski (michalm)
 */
public class VehicleDataEntryFactoryImplTest {
	private final Link depot = new FakeLink(Id.createLinkId("depot"));

	//time slack: 20 (arrival is the constraint)
	private final StopWaypoint stop0 = stop(100, 120, 200, 230);

	//time slack: 30 (departure is the constraint)
	private final StopWaypoint stop1 = stop(300, 340, 400, 430);
	
	private static final IntegerLoadType loadType = new IntegerLoadType("passengers");

	@Test
	void computeSlackTimes_withStops() {
		final List<Double> precedingStayTimes = Arrays.asList(0.0, 0.0);

		//final stay task not started - vehicle slack time is 50
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new StopWaypoint[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(20, 20, 30, 50);

		//final stay task not started - vehicle slack time is 25 and limits the slack times at stop1
		assertThat(computeSlackTimes(vehicle(500, 475), 100, new StopWaypoint[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(20, 20, 25, 25);

		//final stay task not started - vehicle slack time is 10 and limits the slack times at all stops
		assertThat(computeSlackTimes(vehicle(500, 490), 100, new StopWaypoint[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(10, 10, 10, 10);
	}

	@Test
	void computeSlackTimes_withRideDurationConstraints_a() {

		//time slack: 20 (arrival is the constraint)
		StopWaypoint pickupStop =  stop(100, 120, 200, 230);
		//time slack: 30 (departure is the constraint)
		StopWaypoint dropoffStop = stop(300, 340, 400, 430);

		AcceptedDrtRequest mockRequest = AcceptedDrtRequest.newBuilder()
				.request(DrtRequest.newBuilder().id(Id.create("mock", Request.class)).build())
				.latestArrivalTime(340)
				.maxRideDuration(200)
				.earliestStartTime(100)
				.latestStartTime(230)
				.plannedPickupTime(100.)
				.plannedDropoffTime(300.)
				.build();

		// stop duration == 0 --> pickup time == stop begin time
		// ---> ride duration == 200 == max ride duration --> slack should be == 0

		pickupStop.getTask().addPickupRequest(mockRequest);
		dropoffStop.getTask().addDropoffRequest(mockRequest);

		StopWaypoint[] stops = new StopWaypoint[] {pickupStop, dropoffStop};

		var precedingStayTimes = List.of(0.0, 0.0);

		// initial vehicle slack == 1000
		DvrpVehicle vehicle = vehicle(2000, 1000);

		double[] slackTimes = computeSlackTimes(vehicle, 0, stops, null, precedingStayTimes);
		assertThat(slackTimes).containsExactly(0, 0, 0, 1000);
	}

	@Test
	void computeSlackTimes_withRideDurationConstraints_b() {

		//time slack: 20 (arrival is the constraint)
		StopWaypoint pickupStop =  stop(100, 120, 200, 230);
		//time slack: 30 (departure is the constraint)
		StopWaypoint dropoffStop = stop(300, 340, 400, 430);

		AcceptedDrtRequest mockRequest = AcceptedDrtRequest.newBuilder()
				.request(DrtRequest.newBuilder().id(Id.create("mock", Request.class)).build())
				.latestArrivalTime(340)
				.maxRideDuration(210)
				.earliestStartTime(100)
				.latestStartTime(230)
				.plannedPickupTime(100.)
				.plannedDropoffTime(300.)
				.build();

		// stop duration == 0 --> pickup time == stop begin time
		// ---> ride duration == 200 == max ride duration --> slack should be == 0

		pickupStop.getTask().addPickupRequest(mockRequest);
		dropoffStop.getTask().addDropoffRequest(mockRequest);

		StopWaypoint[] stops = new StopWaypoint[] {pickupStop, dropoffStop};

		var precedingStayTimes = List.of(0.0, 0.0);

		// initial vehicle slack == 1000
		DvrpVehicle vehicle = vehicle(2000, 1000);

		double[] slackTimes = computeSlackTimes(vehicle, 0, stops, null, precedingStayTimes);
		assertThat(slackTimes).containsExactly(10, 10, 10, 1000);
	}


	@Test
	void computeSlackTimes_withoutStops() {
		final List<Double> precedingStayTimes = Arrays.asList();

		//final stay task not started yet - vehicle slack time is 10
		assertThat(computeSlackTimes(vehicle(500, 490), 485, new StopWaypoint[] {}, null, precedingStayTimes)).containsExactly(10, 10);

		//final stay task just started - vehicle slack time is 10
		assertThat(computeSlackTimes(vehicle(500, 490), 490, new StopWaypoint[] {}, null, precedingStayTimes)).containsExactly(10, 10);

		//final stay task half completed - vehicle slack time is 5
		assertThat(computeSlackTimes(vehicle(500, 490), 495, new StopWaypoint[] {}, null, precedingStayTimes)).containsExactly(5, 5);

		//final stay task just completed - vehicle slack time is 0
		assertThat(computeSlackTimes(vehicle(500, 490), 500, new StopWaypoint[] {}, null, precedingStayTimes)).containsExactly(0, 0);

		//final stay task started, but delayed - vehicle slack time is 0
		assertThat(computeSlackTimes(vehicle(500, 510), 510, new StopWaypoint[] {}, null, precedingStayTimes)).containsExactly(0, 0);

		//final stay task planned after vehicle end time - vehicle slack time is 0s
		assertThat(computeSlackTimes(vehicle(500, 510), 300, new StopWaypoint[] {}, null, precedingStayTimes)).containsExactly(0, 0);
	}

	@Test
	void computeSlackTimes_withStart() {
		final List<Double> noPrecedingStayTimes = Arrays.asList();
		final List<Double> onePrecedingStayTime = Arrays.asList(0.0);

		//start without stop
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new StopWaypoint[] {}, stop0, noPrecedingStayTimes)).containsExactly(30, 50);

		//start without stop
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new StopWaypoint[] {}, stop1, noPrecedingStayTimes)).containsExactly(30, 50);

		//start with stop
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new StopWaypoint[] { stop1 }, stop0, onePrecedingStayTime)).containsExactly(30, 30, 50);
	}

	@Test
	void computeSlackTimes_withPrecedingStayTimes() {
		final List<Double> precedingStayTimes = Arrays.asList( //
				0.0, //
				33.0 // second stop is a prebooked pickup, so slack for insertion after first stop is longer
				);

		// note that these examples are naively adapted from computeSlackTimes_withStops
		// in practice the slack would never pass the service end time slack (ie the
		// last value in the list) if the preceding insertions were done correctly and
		// there was no congestion

		//final stay task not started - vehicle slack time is 50
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new StopWaypoint[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(20, 20, 63, 50);

		//final stay task not started - vehicle slack time is 25 and limits the slack times at stop1
		assertThat(computeSlackTimes(vehicle(500, 475), 100, new StopWaypoint[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(20, 20, 58, 25);

		//final stay task not started - vehicle slack time is 10 and limits the slack times at all stops
		assertThat(computeSlackTimes(vehicle(500, 490), 100, new StopWaypoint[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(20, 20, 43, 10);
	}

	private StopWaypoint stop(double beginTime, double latestArrivalTime, double endTime, double latestDepartureTime) {
		return new StopWaypointImpl(new DefaultDrtStopTask(beginTime, endTime, null), latestArrivalTime, latestDepartureTime, loadType.getEmptyLoad(), loadType);
	}

	private DvrpVehicle vehicle(double vehicleEndTime, double lastStayTaskBeginTime) {
		var vehicle = new DvrpVehicleImpl(ImmutableDvrpVehicleSpecification.newBuilder()
				.id(Id.create("a", DvrpVehicle.class))
				.startLinkId(depot.getId())
				.capacity(0)
				.serviceBeginTime(0)
				.serviceEndTime(vehicleEndTime)
				.build(), depot);
		vehicle.getSchedule()
				.addTask(
						new DrtStayTask(lastStayTaskBeginTime, Math.max(lastStayTaskBeginTime, vehicleEndTime), depot));
		return vehicle;
	}

	/**
	 * Test that slack calculation correctly handles multiple passengers with different ride duration constraints.
	 * Verifies that the simplified algorithm (checking only dropoffs) produces correct slack propagation.
	 */
	@Test
	void computeSlackTimes_multiplePassengers_tightestConstraintPropagates() {
		// Scenario:
		// Stop 0 (t=100): Pickup passenger A (maxRideDuration=300, loose constraint)
		// Stop 1 (t=200): Pickup passenger B (maxRideDuration=100, tight constraint!)
		// Stop 2 (t=300): Dropoff passenger A
		// Stop 3 (t=350): Dropoff passenger B
		//
		// Current ride durations:
		// - A: 300-100 = 200s (slack = 300-200 = 100s)
		// - B: 350-200 = 150s (slack = 100-150 = -50s → 0s, violated!)
		//
		// Time windows are set very loose (1000s slack) so they don't interfere
		// Expected: B's 0 slack should dominate at all stops after B's pickup

		// stop(beginTime, latestArrivalTime, endTime, latestDepartureTime)
		// Set time windows very loose: latestArrival = begin + 1000, latestDeparture = end + 1000
		StopWaypoint pickupStopA = stop(100, 1100, 110, 1110);
		StopWaypoint pickupStopB = stop(200, 1200, 210, 1210);
		StopWaypoint dropoffStopA = stop(300, 1300, 310, 1310);
		StopWaypoint dropoffStopB = stop(350, 1350, 360, 1360);

		AcceptedDrtRequest requestA = AcceptedDrtRequest.newBuilder()
				.request(DrtRequest.newBuilder().id(Id.create("reqA", Request.class)).build())
				.maxRideDuration(300)
				.plannedPickupTime(100.)
				.plannedDropoffTime(300.)
				.build();

		AcceptedDrtRequest requestB = AcceptedDrtRequest.newBuilder()
				.request(DrtRequest.newBuilder().id(Id.create("reqB", Request.class)).build())
				.maxRideDuration(100)
				.plannedPickupTime(200.)
				.plannedDropoffTime(350.)
				.build();

		pickupStopA.getTask().addPickupRequest(requestA);
		pickupStopB.getTask().addPickupRequest(requestB);
		dropoffStopA.getTask().addDropoffRequest(requestA);
		dropoffStopB.getTask().addDropoffRequest(requestB);

		StopWaypoint[] stops = new StopWaypoint[] {pickupStopA, pickupStopB, dropoffStopA, dropoffStopB};
		var precedingStayTimes = List.of(0.0, 0.0, 0.0, 0.0);

		DvrpVehicle vehicle = vehicle(2000, 1000);

		double[] slackTimes = computeSlackTimes(vehicle, 0, stops, null, precedingStayTimes);

		// Expected slackTimes: [start, afterStop0, afterStop1, afterStop2, afterStop3, vehicle]
		// At stop 3 (dropoff B): min(time_window_slack=1000, B_ride_slack=0) = 0
		// At stop 2 (dropoff A): min(time_window_slack=1000, A_ride_slack=100, propagated=0) = 0
		// At stop 1 (pickup B): min(time_window_slack=1000, propagated=0) = 0
		// At stop 0 (pickup A): min(time_window_slack=1000, propagated=0) = 0
		// Start: propagated = 0
		// Vehicle: 1000
		assertThat(slackTimes).containsExactly(0, 0, 0, 0, 0, 1000);
	}

	/**
	 * Test that when checking only dropoffs, we correctly capture constraints from passengers
	 * picked up at intermediate stops.
	 */
	@Test
	void computeSlackTimes_multiplePassengers_intermediatePickupConstraint() {
		// Scenario:
		// Stop 0 (t=100): Pickup A (maxRideDuration=400, loose)
		// Stop 1 (t=200): Pickup B (maxRideDuration=200, moderate)
		// Stop 2 (t=300): Pickup C (maxRideDuration=100, tight)
		// Stop 3 (t=400): Dropoff A (slack = 400-300 = 100)
		// Stop 4 (t=450): Dropoff B (slack = 200-250 = -50 → 0)
		// Stop 5 (t=500): Dropoff C (slack = 100-200 = -100 → 0)
		//
		// Time windows are set very loose (1000s slack) so they don't interfere

		StopWaypoint pickupStopA = stop(100, 1100, 110, 1110);
		StopWaypoint pickupStopB = stop(200, 1200, 210, 1210);
		StopWaypoint pickupStopC = stop(300, 1300, 310, 1310);
		StopWaypoint dropoffStopA = stop(400, 1400, 410, 1410);
		StopWaypoint dropoffStopB = stop(450, 1450, 460, 1460);
		StopWaypoint dropoffStopC = stop(500, 1500, 510, 1510);

		AcceptedDrtRequest requestA = AcceptedDrtRequest.newBuilder()
				.request(DrtRequest.newBuilder().id(Id.create("reqA", Request.class)).build())
				.maxRideDuration(400)
				.plannedPickupTime(100.)
				.plannedDropoffTime(400.)
				.build();

		AcceptedDrtRequest requestB = AcceptedDrtRequest.newBuilder()
				.request(DrtRequest.newBuilder().id(Id.create("reqB", Request.class)).build())
				.maxRideDuration(200)
				.plannedPickupTime(200.)
				.plannedDropoffTime(450.)
				.build();

		AcceptedDrtRequest requestC = AcceptedDrtRequest.newBuilder()
				.request(DrtRequest.newBuilder().id(Id.create("reqC", Request.class)).build())
				.maxRideDuration(100)
				.plannedPickupTime(300.)
				.plannedDropoffTime(500.)
				.build();

		pickupStopA.getTask().addPickupRequest(requestA);
		pickupStopB.getTask().addPickupRequest(requestB);
		pickupStopC.getTask().addPickupRequest(requestC);
		dropoffStopA.getTask().addDropoffRequest(requestA);
		dropoffStopB.getTask().addDropoffRequest(requestB);
		dropoffStopC.getTask().addDropoffRequest(requestC);

		StopWaypoint[] stops = new StopWaypoint[] {
				pickupStopA, pickupStopB, pickupStopC, dropoffStopA, dropoffStopB, dropoffStopC
		};
		var precedingStayTimes = List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

		DvrpVehicle vehicle = vehicle(2000, 1000);

		double[] slackTimes = computeSlackTimes(vehicle, 0, stops, null, precedingStayTimes);

		// Working backward:
		// At stop 5 (dropoff C): min(time_window=1000, C_slack=max(0,100-200)) = 0
		// At stop 4 (dropoff B): min(time_window=1000, B_slack=max(0,200-250), propagated=0) = 0
		// At stop 3 (dropoff A): min(time_window=1000, A_slack=100, propagated=0) = 0
		// All earlier stops: 0 propagates backward
		// Vehicle: 1000
		assertThat(slackTimes).containsExactly(0, 0, 0, 0, 0, 0, 0, 1000);
	}

	/**
	 * Test interaction between ride duration constraints and prebooked stay time buffers.
	 * Stay time buffers should absorb delays without affecting the ride duration constraint.
	 */
	@Test
	void computeSlackTimes_withRideDurationAndStayTimeBuffer() {
		// Scenario:
		// Stop 0 (t=100): Pickup A (maxRideDuration=300)
		// Stay time: 50s before stop 1
		// Stop 1 (t=200): Dropoff A (prebooked with buffer)
		//
		// Ride duration: 200-100 = 100s, slack = 300-100 = 200s
		// Time windows set very loose (1000s) so they don't interfere

		StopWaypoint pickupStop = stop(100, 1100, 110, 1110);
		StopWaypoint dropoffStop = stop(200, 1200, 210, 1210);

		AcceptedDrtRequest request = AcceptedDrtRequest.newBuilder()
				.request(DrtRequest.newBuilder().id(Id.create("req", Request.class)).build())
				.maxRideDuration(300)
				.plannedPickupTime(100.)
				.plannedDropoffTime(200.)
				.build();

		pickupStop.getTask().addPickupRequest(request);
		dropoffStop.getTask().addDropoffRequest(request);

		StopWaypoint[] stops = new StopWaypoint[] {pickupStop, dropoffStop};
		var precedingStayTimes = List.of(0.0, 50.0);

		DvrpVehicle vehicle = vehicle(2000, 1000);

		double[] slackTimes = computeSlackTimes(vehicle, 0, stops, null, precedingStayTimes);

		// Expected: [start, afterStop0, afterStop1, vehicle]
		// Backward calculation:
		// At stop 1: min(time_window=1000, ride_slack=200) = 200, then += 50 = 250
		// At stop 0: min(time_window=1000, propagated=250) = 250
		// Start: 250
		// Vehicle: 1000
		assertThat(slackTimes).containsExactly(250, 250, 250, 1000);
	}
}
