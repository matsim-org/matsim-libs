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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * @author Michal Maciejewski (michalm)
 */
public class BestInsertionFinderTest {
	//min cost is used
	// if min costs equal, use the Id/idx comparator

	private final DrtRequest request = mock(DrtRequest.class);
	private final InsertionCostCalculator insertionCostCalculator = mock(InsertionCostCalculator.class);
	private final BestInsertionFinder bestInsertionFinder = new BestInsertionFinder(insertionCostCalculator);

	@Test
	void noInsertions_empty() {
		assertThat(findBestInsertion()).isEmpty();
	}

	@Test
	void noFeasibleInsertions_empty() {
		when(insertionCostCalculator.calculate(eq(request), any(), any())).thenReturn(INFEASIBLE_SOLUTION_COST);

		assertThat(findBestInsertion(insertion("v1", 0, 0), insertion("v1", 0, 1))).isEmpty();
	}

	@Test
	void oneFeasibleInsertion_oneInfeasibleInsertion_chooseFeasible() {
		var feasibleInsertion = insertion("v1", 0, 0);
		whenInsertionThenCost(feasibleInsertion, 12345);

		var infeasibleInsertion = insertion("v1", 0, 1);
		whenInsertionThenCost(infeasibleInsertion, INFEASIBLE_SOLUTION_COST);

		assertThat(findBestInsertion(feasibleInsertion, infeasibleInsertion)).hasValue(feasibleInsertion);
		assertThat(findBestInsertion(infeasibleInsertion, feasibleInsertion)).hasValue(feasibleInsertion);
	}

	@Test
	void twoFeasibleInsertions_chooseBetter() {
		var insertion1 = insertion("v1", 0, 0);
		whenInsertionThenCost(insertion1, 123);

		var insertion2 = insertion("v1", 0, 1);
		whenInsertionThenCost(insertion2, 9999);

		assertThat(findBestInsertion(insertion1, insertion2)).hasValue(insertion1);
		assertThat(findBestInsertion(insertion2, insertion1)).hasValue(insertion1);
	}

	@Test
	void twoEqualInsertions_chooseLowerVehicleId() {
		var insertion1 = insertion("v1", 0, 0);
		whenInsertionThenCost(insertion1, 123);

		var insertion2 = insertion("v2", 0, 0);
		whenInsertionThenCost(insertion2, 123);

		assertThat(findBestInsertion(insertion1, insertion2)).hasValue(insertion1);
		assertThat(findBestInsertion(insertion2, insertion1)).hasValue(insertion1);
	}

	@Test
	void twoEqualInsertions_sameVehicle_chooseLowerPickupIdx() {
		var insertion1 = insertion("v1", 0, 1);
		whenInsertionThenCost(insertion1, 123);

		var insertion2 = insertion("v1", 1, 1);
		whenInsertionThenCost(insertion2, 123);

		assertThat(findBestInsertion(insertion1, insertion2)).hasValue(insertion1);
		assertThat(findBestInsertion(insertion2, insertion1)).hasValue(insertion1);
	}

	@Test
	void twoEqualInsertions_sameVehicle_samePickupIdx_chooseLowerDropoffIdx() {
		var insertion1 = insertion("v1", 0, 0);
		whenInsertionThenCost(insertion1, 123);

		var insertion2 = insertion("v1", 0, 1);
		whenInsertionThenCost(insertion2, 123);

		assertThat(findBestInsertion(insertion1, insertion2)).hasValue(insertion1);
		assertThat(findBestInsertion(insertion2, insertion1)).hasValue(insertion1);
	}

	@SafeVarargs
	private Optional<InsertionWithDetourData> findBestInsertion(InsertionWithDetourData... insertions) {
		return bestInsertionFinder.findBestInsertion(request, Arrays.stream(insertions));
	}

	private void whenInsertionThenCost(InsertionWithDetourData insertion, double cost) {
		when(insertionCostCalculator.calculate(eq(request), eq(insertion.insertion),
				eq(insertion.detourTimeInfo))).thenReturn(cost);
	}

	private InsertionWithDetourData insertion(String vehicleId, int pickupIdx, int dropoffIdx) {
		var vehicle = mock(DvrpVehicle.class);
		when(vehicle.getId()).thenReturn(Id.create(vehicleId, DvrpVehicle.class));
		var vehicleEntry = new VehicleEntry(vehicle, null, null, null, null, 0);

		var pickupInsertion = new InsertionGenerator.InsertionPoint(pickupIdx, null, null, null);
		var dropoffInsertion = new InsertionGenerator.InsertionPoint(dropoffIdx, null, null, null);

		return new InsertionWithDetourData(
				new InsertionGenerator.Insertion(vehicleEntry, pickupInsertion, dropoffInsertion), null, null);
	}
}
