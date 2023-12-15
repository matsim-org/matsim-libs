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
import org.matsim.contrib.drt.optimizer.Waypoint.Stop;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.testcases.fakes.FakeLink;

/**
 * @author Michal Maciejewski (michalm)
 */
public class VehicleDataEntryFactoryImplTest {
	private final Link depot = new FakeLink(Id.createLinkId("depot"));

	//time slack: 20 (arrival is the constraint)
	private final Stop stop0 = stop(100, 120, 200, 230);

	//time slack: 30 (departure is the constraint)
	private final Stop stop1 = stop(300, 340, 400, 430);

	@Test
	void computeSlackTimes_withStops() {
		final List<Double> precedingStayTimes = Arrays.asList(0.0, 0.0);
		
		//final stay task not started - vehicle slack time is 50
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new Stop[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(20, 20, 30, 50);

		//final stay task not started - vehicle slack time is 25 and limits the slack times at stop1
		assertThat(computeSlackTimes(vehicle(500, 475), 100, new Stop[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(20, 20, 25, 25);

		//final stay task not started - vehicle slack time is 10 and limits the slack times at all stops
		assertThat(computeSlackTimes(vehicle(500, 490), 100, new Stop[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(10, 10, 10, 10);
	}

	@Test
	void computeSlackTimes_withoutStops() {
		final List<Double> precedingStayTimes = Arrays.asList();
		
		//final stay task not started yet - vehicle slack time is 10
		assertThat(computeSlackTimes(vehicle(500, 490), 485, new Stop[] {}, null, precedingStayTimes)).containsExactly(10, 10);

		//final stay task just started - vehicle slack time is 10
		assertThat(computeSlackTimes(vehicle(500, 490), 490, new Stop[] {}, null, precedingStayTimes)).containsExactly(10, 10);

		//final stay task half completed - vehicle slack time is 5
		assertThat(computeSlackTimes(vehicle(500, 490), 495, new Stop[] {}, null, precedingStayTimes)).containsExactly(5, 5);

		//final stay task just completed - vehicle slack time is 0
		assertThat(computeSlackTimes(vehicle(500, 490), 500, new Stop[] {}, null, precedingStayTimes)).containsExactly(0, 0);

		//final stay task started, but delayed - vehicle slack time is 0
		assertThat(computeSlackTimes(vehicle(500, 510), 510, new Stop[] {}, null, precedingStayTimes)).containsExactly(0, 0);

		//final stay task planned after vehicle end time - vehicle slack time is 0s
		assertThat(computeSlackTimes(vehicle(500, 510), 300, new Stop[] {}, null, precedingStayTimes)).containsExactly(0, 0);
	}

	@Test
	void computeSlackTimes_withStart() {
		final List<Double> noPrecedingStayTimes = Arrays.asList();
		final List<Double> onePrecedingStayTime = Arrays.asList(0.0);
		
		//start without stop
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new Stop[] {}, stop0, noPrecedingStayTimes)).containsExactly(30, 50);

		//start without stop
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new Stop[] {}, stop1, noPrecedingStayTimes)).containsExactly(30, 50);

		//start with stop
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new Stop[] { stop1 }, stop0, onePrecedingStayTime)).containsExactly(30, 30, 50);
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
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new Stop[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(20, 20, 63, 50);

		//final stay task not started - vehicle slack time is 25 and limits the slack times at stop1
		assertThat(computeSlackTimes(vehicle(500, 475), 100, new Stop[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(20, 20, 58, 25);

		//final stay task not started - vehicle slack time is 10 and limits the slack times at all stops
		assertThat(computeSlackTimes(vehicle(500, 490), 100, new Stop[] { stop0, stop1 }, null, precedingStayTimes)).containsExactly(20, 20, 43, 10);
	}

	private Stop stop(double beginTime, double latestArrivalTime, double endTime, double latestDepartureTime) {
		return new Stop(new DefaultDrtStopTask(beginTime, endTime, null), latestArrivalTime, latestDepartureTime, 0);
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
}
