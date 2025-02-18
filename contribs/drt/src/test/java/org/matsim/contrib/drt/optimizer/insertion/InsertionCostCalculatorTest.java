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

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DetourTimeInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.DropoffDetourInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionDetourTimeCalculator.PickupDetourInfo;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DefaultDrtStopTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.load.IntegerLoadType;
import org.matsim.testcases.fakes.FakeLink;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionCostCalculatorTest {
	private final Link fromLink = link("from");
	private final Link toLink = link("to");
	private final DrtRequest drtRequest = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).build();

	private final IntegerLoadType loadType = new IntegerLoadType("passengers");

	@Test
	void testCalculate() {
		VehicleEntry entry = entry(new double[] { 20, 20, 50 }, ImmutableList.<Waypoint.Stop>builder().build(), null);
		var insertion = insertion(entry, 0, 1);

		//feasible solution
		final DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 11), new DropoffDetourInfo(0, 22)),
				11 + 22, drtRequest, drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet());

		//feasible solution - longest possible pickup and dropoff time losses
		final DrtConfigGroup drtConfigGroup1 = new DrtConfigGroup();
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 20), new DropoffDetourInfo(0, 30)),
				20 + 30, drtRequest, drtConfigGroup1.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet());

		//infeasible solution - time constraints at stop 0
		final DrtConfigGroup drtConfigGroup2 = new DrtConfigGroup();
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 21), new DropoffDetourInfo(0, 29)),
				INFEASIBLE_SOLUTION_COST, drtRequest, drtConfigGroup2.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet());

		//infeasible solution - vehicle time constraints
		final DrtConfigGroup drtConfigGroup3 = new DrtConfigGroup();
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 20), new DropoffDetourInfo(0, 31)),
				INFEASIBLE_SOLUTION_COST, drtRequest, drtConfigGroup3.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet());
	}


	@Test
	public void testAllowDetourBeforeArrivalThreshold() {


		// start (0s) -----> new PU (60s) -----> existing DO (120s) -----> new DO (300s)

		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, loadType.fromInt(1));

		DrtStopTask existingDropoffTask = new DefaultDrtStopTask(120, 150, link("boardedDO"));
		DrtRequest boardedRequest = DrtRequest.newBuilder().fromLink(link("boardedFrom")).toLink(link("boardedTo")).build();

		AcceptedDrtRequest existingRequest = AcceptedDrtRequest.createFromOriginalRequest(boardedRequest);
		existingDropoffTask.addDropoffRequest(existingRequest);

		Waypoint.Stop[] stops = new Waypoint.Stop[1];
		stops[0] = new Waypoint.Stop(existingDropoffTask, loadType.fromInt(1), loadType);

		VehicleEntry entry = entry(new double[] {60, 60, 300}, ImmutableList.copyOf(stops), start);
		var insertion = insertion(entry, 0, 1);

		DrtRequest drtRequest = DrtRequest.newBuilder()
				.fromLink(fromLink)
				.toLink(toLink)
				.latestStartTime(120)
				.latestArrivalTime(300)
				.maxRideDuration(Double.MAX_VALUE)
				.build();

		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
		DrtOptimizationConstraintsSet drtOptimizationConstraintsSet = drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();

		// new insertion before dropoff of boarded passenger within threshold - infeasible solution
		drtOptimizationConstraintsSet.lateDiversionthreshold = 180;
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(60, 30), new DropoffDetourInfo(300, 30)),
				INFEASIBLE_SOLUTION_COST, drtRequest, drtOptimizationConstraintsSet);

		// new insertion before dropoff of boarded passenger, inside of threshold but no additional delay - feasible solution
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(120, 0), new DropoffDetourInfo(300, 30)),
				30, drtRequest, drtOptimizationConstraintsSet);

		// new insertion before dropoff of boarded passenger, but outside of threshold - feasible solution
		drtOptimizationConstraintsSet.lateDiversionthreshold = 120;
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(60, 30), new DropoffDetourInfo(300, 30)),
				60, drtRequest, drtOptimizationConstraintsSet);


		// new insertion after dropoff of boarded passenger - feasible solution
		insertion = insertion(entry, 1, 1);
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(60, 30), new DropoffDetourInfo(300, 30)),
				60, drtRequest, drtOptimizationConstraintsSet);
	}

	@Test
	public void testAllowDetourBeforeArrivalThreshold2() {

		// start (0s) -----> new PU (60s) -----> existing PU (120s) -----> existing DO (200s) -----> new DO (300s)

		Waypoint.Start start = new Waypoint.Start(null, link("start"), 0, loadType.fromInt(1));

		DrtStopTask existingPickupTask = new DefaultDrtStopTask(120, 150, link("scheduledPU"));
		DrtRequest scheduledRequest = DrtRequest.newBuilder().fromLink(link("scheduledFrom")).toLink(link("scheduledTo")).build();
		AcceptedDrtRequest acceptedScheduledRequest = AcceptedDrtRequest.createFromOriginalRequest(scheduledRequest);
		existingPickupTask.addPickupRequest(acceptedScheduledRequest);

		DrtStopTask existingDropoffTask = new DefaultDrtStopTask(200, 230, link("boardedDO"));
		DrtRequest boardedRequest = DrtRequest.newBuilder().fromLink(link("boardedFrom")).toLink(link("boardedTo")).build();
		AcceptedDrtRequest existingRequest = AcceptedDrtRequest.createFromOriginalRequest(boardedRequest);
		existingDropoffTask.addDropoffRequest(existingRequest);

		Waypoint.Stop[] stops = new Waypoint.Stop[2];
		stops[0] = new Waypoint.Stop(existingPickupTask, loadType.fromInt(2), loadType);
		stops[1] = new Waypoint.Stop(existingDropoffTask, loadType.fromInt(1), loadType);

		VehicleEntry entry = entry(new double[] {60, 60, 60, 300}, ImmutableList.copyOf(stops), start);


		var insertion = insertion(entry, 0, 2);

		DrtRequest drtRequest = DrtRequest.newBuilder()
				.fromLink(fromLink)
				.toLink(toLink)
				.latestStartTime(120)
				.latestArrivalTime(300)
				.maxRideDuration(Double.MAX_VALUE)
				.build();

		DrtConfigGroup drtConfigGroup = new DrtConfigGroup();

		// new insertion before dropoff of boarded passenger within threshold - infeasible solution
		DrtOptimizationConstraintsSet constraintsSet = drtConfigGroup.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();
		constraintsSet.lateDiversionthreshold = 300;
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(60, 60), new DropoffDetourInfo(300, 60)),
				INFEASIBLE_SOLUTION_COST, drtRequest, constraintsSet);

		// new insertion before dropoff of boarded passenger outside of threshold - feasible solution
		constraintsSet.lateDiversionthreshold = 200;
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(60, 60), new DropoffDetourInfo(300, 60)),
				120, drtRequest, constraintsSet);
	}

	private void assertCalculate(Insertion insertion, DetourTimeInfo detourTimeInfo, double expectedCost, DrtRequest drtRequest, DrtOptimizationConstraintsSet constraintsSet) {
		var insertionCostCalculator = new DefaultInsertionCostCalculator(
				new CostCalculationStrategy.RejectSoftConstraintViolations(), constraintsSet);
		var insertionWithDetourData = new InsertionWithDetourData(insertion, null, detourTimeInfo);
		assertThat(insertionCostCalculator.calculate(drtRequest, insertionWithDetourData.insertion,
				insertionWithDetourData.detourTimeInfo)).isEqualTo(expectedCost);
	}

	private VehicleEntry entry(double[] slackTimes, ImmutableList<Waypoint.Stop> stops, Waypoint.Start start) {
		return new VehicleEntry(null, start, stops, slackTimes, stops.stream().map(s -> 0.).toList(), 0);
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private Insertion insertion(VehicleEntry entry, int pickupIdx, int dropoffIdx) {
		return new Insertion(entry, new InsertionGenerator.InsertionPoint(pickupIdx, null, null, null),
				new InsertionGenerator.InsertionPoint(dropoffIdx, null, null, null), loadType.fromInt(1));
	}
}
