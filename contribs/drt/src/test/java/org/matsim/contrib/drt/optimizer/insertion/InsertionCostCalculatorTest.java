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
import static org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.INFEASIBLE_SOLUTION_COST;
import static org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.*;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.testcases.fakes.FakeLink;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionCostCalculatorTest {
	private final Link fromLink = link("from");
	private final Link toLink = link("to");
	private final DrtRequest drtRequest = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).build();

	@Test
	public void testCalculate() {
		VehicleEntry entry = entry(new double[] { 20, 50 }, null, null);
		var insertion = insertion(entry, 0, 1);

		//feasible solution
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 11), new DropoffDetourInfo(0, 22)),
				11 + 22, new DrtConfigGroup(), drtRequest);

		//feasible solution - longest possible pickup and dropoff time losses
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 20), new DropoffDetourInfo(0, 30)),
				20 + 30, new DrtConfigGroup(), drtRequest);

		//infeasible solution - time constraints at stop 0
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 21), new DropoffDetourInfo(0, 29)),
				INFEASIBLE_SOLUTION_COST, new DrtConfigGroup(), drtRequest);

		//infeasible solution - vehicle time constraints
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 20), new DropoffDetourInfo(0, 31)),
				INFEASIBLE_SOLUTION_COST, new DrtConfigGroup(), drtRequest);
	}


	@Test
	public void testAllowDetourBeforeArrivalThreshold() {


		// start (0s) -----> new PU (60s) -----> existing DO (120s) -----> new DO (300s)

		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, 0);

		DrtStopTask existingDropoffTask = new DefaultDrtStopTask(120, 150, link("boardedDO"));
		DrtRequest boardedRequest = DrtRequest.newBuilder().fromLink(link("boardedFrom")).toLink(link("boardedTo")).build();

		AcceptedDrtRequest existingRequest = AcceptedDrtRequest.createFromOriginalRequest(boardedRequest);
		existingDropoffTask.addDropoffRequest(existingRequest);

		Waypoint.Stop[] stops = new Waypoint.Stop[1];
		stops[0] = new Waypoint.Stop(existingDropoffTask, 0);

		VehicleEntry entry = entry(new double[] {60, 300}, ImmutableList.copyOf(stops), start);
		var insertion = insertion(entry, 0, 1);

		DrtRequest drtRequest = DrtRequest.newBuilder()
				.fromLink(fromLink)
				.toLink(toLink)
				.latestStartTime(120)
				.latestArrivalTime(300)
				.build();

		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();

		// new insertion before dropoff of boarded passenger within threshold - infeasible solution
		drtConfigGroup.allowDetourBeforeArrivalThreshold = 180;
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(60, 30), new DropoffDetourInfo(300, 30)),
				INFEASIBLE_SOLUTION_COST, drtConfigGroup, drtRequest);

		// new insertion before dropoff of boarded passenger, inside of threshold but no additional delay - feasible solution
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(120, 0), new DropoffDetourInfo(300, 30)),
				30, drtConfigGroup, drtRequest);

		// new insertion before dropoff of boarded passenger, but outside of threshold - feasible solution
		drtConfigGroup.allowDetourBeforeArrivalThreshold = 120;
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(60, 30), new DropoffDetourInfo(300, 30)),
				60, drtConfigGroup, drtRequest);


		// new insertion after dropoff of boarded passenger - feasible solution
		insertion = insertion(entry, 1, 1);
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(60, 30), new DropoffDetourInfo(300, 30)),
				60, drtConfigGroup, drtRequest);
	}

	private void assertCalculate(Insertion insertion, DetourTimeInfo detourTimeInfo, double expectedCost, DrtConfigGroup drtConfigGroup, DrtRequest drtRequest) {
		var insertionCostCalculator = new DefaultInsertionCostCalculator(
				new CostCalculationStrategy.RejectSoftConstraintViolations(), drtConfigGroup);
		var insertionWithDetourData = new InsertionWithDetourData(insertion, null, detourTimeInfo);
		assertThat(insertionCostCalculator.calculate(drtRequest, insertionWithDetourData.insertion,
				insertionWithDetourData.detourTimeInfo)).isEqualTo(expectedCost);
	}

	private VehicleEntry entry(double[] slackTimes, ImmutableList<Waypoint.Stop> stops, Waypoint.Start start) {
		return new VehicleEntry(null, start, stops, slackTimes);
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private Insertion insertion(VehicleEntry entry, int pickupIdx, int dropoffIdx) {
		return new Insertion(entry, new InsertionGenerator.InsertionPoint(pickupIdx, null, null, null),
				new InsertionGenerator.InsertionPoint(dropoffIdx, null, null, null));
	}
}
