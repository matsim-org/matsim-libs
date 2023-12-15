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

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.testcases.fakes.FakeLink;

/**
 * @author Michal Maciejewski (michalm)
 */
public class InsertionCostCalculatorTest {
	private final Link fromLink = link("from");
	private final Link toLink = link("to");
	private final DrtRequest drtRequest = DrtRequest.newBuilder().fromLink(fromLink).toLink(toLink).build();

	@Test
	void testCalculate() {
		VehicleEntry entry = entry(new double[] { 20, 20, 50 });
		var insertion = insertion(entry, 0, 1);

		//feasible solution
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 11), new DropoffDetourInfo(0, 22)),
				11 + 22);

		//feasible solution - longest possible pickup and dropoff time losses
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 20), new DropoffDetourInfo(0, 30)),
				20 + 30);

		//infeasible solution - time constraints at stop 0
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 21), new DropoffDetourInfo(0, 29)),
				INFEASIBLE_SOLUTION_COST);

		//infeasible solution - vehicle time constraints
		assertCalculate(insertion, new DetourTimeInfo(new PickupDetourInfo(0, 20), new DropoffDetourInfo(0, 31)),
				INFEASIBLE_SOLUTION_COST);
	}

	private void assertCalculate(Insertion insertion, DetourTimeInfo detourTimeInfo, double expectedCost) {
		var insertionCostCalculator = new DefaultInsertionCostCalculator(
				new CostCalculationStrategy.RejectSoftConstraintViolations());
		var insertionWithDetourData = new InsertionWithDetourData(insertion, null, detourTimeInfo);
		assertThat(insertionCostCalculator.calculate(drtRequest, insertionWithDetourData.insertion,
				insertionWithDetourData.detourTimeInfo)).isEqualTo(expectedCost);
	}

	private VehicleEntry entry(double[] slackTimes) {
		return new VehicleEntry(null, null, null, slackTimes, null, 0);
	}

	private Link link(String id) {
		return new FakeLink(Id.createLinkId(id));
	}

	private Insertion insertion(VehicleEntry entry, int pickupIdx, int dropoffIdx) {
		return new Insertion(entry, new InsertionGenerator.InsertionPoint(pickupIdx, null, null, null),
				new InsertionGenerator.InsertionPoint(dropoffIdx, null, null, null));
	}
}
