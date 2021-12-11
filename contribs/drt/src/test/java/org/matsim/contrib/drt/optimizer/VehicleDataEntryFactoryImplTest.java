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
import static org.matsim.contrib.drt.optimizer.Waypoint.Stop;

import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
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
	public void computeSlackTimes_withStops() {
		//final stay task not started - vehicle slack time is 50
		assertThat(computeSlackTimes(vehicle(500, 450), 100, new Stop[] { stop0, stop1 })).containsExactly(20, 30, 50);

		//final stay task not started - vehicle slack time is 25 and limits the slack times at stop1
		assertThat(computeSlackTimes(vehicle(500, 475), 100, new Stop[] { stop0, stop1 })).containsExactly(20, 25, 25);

		//final stay task not started - vehicle slack time is 10 and limits the slack times at all stops
		assertThat(computeSlackTimes(vehicle(500, 490), 100, new Stop[] { stop0, stop1 })).containsExactly(10, 10, 10);
	}

	@Test
	public void computeSlackTimes_withoutStops() {
		//final stay task not started yet - vehicle slack time is 10
		assertThat(computeSlackTimes(vehicle(500, 490), 485, new Stop[] {})).containsExactly(10);

		//final stay task just started - vehicle slack time is 10
		assertThat(computeSlackTimes(vehicle(500, 490), 490, new Stop[] {})).containsExactly(10);

		//final stay task half completed - vehicle slack time is 5
		assertThat(computeSlackTimes(vehicle(500, 490), 495, new Stop[] {})).containsExactly(5);

		//final stay task just completed - vehicle slack time is 0
		assertThat(computeSlackTimes(vehicle(500, 490), 500, new Stop[] {})).containsExactly(0);

		//final stay task started, but delayed - vehicle slack time is 0
		assertThat(computeSlackTimes(vehicle(500, 510), 510, new Stop[] {})).containsExactly(0);

		//final stay task planned after vehicle end time - vehicle slack time is 0s
		assertThat(computeSlackTimes(vehicle(500, 510), 300, new Stop[] {})).containsExactly(0);
	}

	private Stop stop(double beginTime, double latestArrivalTime, double endTime, double latestDepartureTime) {
		return new Stop(new DrtStopTask(beginTime, endTime, null), latestArrivalTime, latestDepartureTime, 0);
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
