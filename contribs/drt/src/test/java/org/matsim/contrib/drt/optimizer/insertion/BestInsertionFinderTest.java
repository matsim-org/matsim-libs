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

import org.junit.Test;
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
	@SuppressWarnings("unchecked")
	private final InsertionCostCalculator<Double> insertionCostCalculator = mock(InsertionCostCalculator.class);
	private final BestInsertionFinder<Double> bestInsertionFinder = new BestInsertionFinder<>(insertionCostCalculator);

	@Test
	public void noInsertions_empty() {
		assertThat(findBestInsertion()).isEmpty();
	}

	@Test
	public void noFeasibleInsertions_empty() {
		when(insertionCostCalculator.calculate(eq(request), any())).thenReturn(INFEASIBLE_SOLUTION_COST);

		assertThat(findBestInsertion(insertion(), insertion())).isEmpty();
	}

	@Test
	public void oneFeasibleInsertion_oneInfeasibleInsertion_chooseFeasible() {
		var feasibleInsertion = insertion();
		whenInsertionThenCost(feasibleInsertion, 12345);

		var infeasibleInsertion = insertion();
		whenInsertionThenCost(infeasibleInsertion, INFEASIBLE_SOLUTION_COST);

		assertThat(findBestInsertion(feasibleInsertion, infeasibleInsertion)).hasValue(feasibleInsertion);
		assertThat(findBestInsertion(infeasibleInsertion, feasibleInsertion)).hasValue(feasibleInsertion);
	}

	@Test
	public void twoFeasibleInsertions_chooseBetter() {
		var insertion1 = insertion();
		whenInsertionThenCost(insertion1, 123);

		var insertion2 = insertion();
		whenInsertionThenCost(insertion2, 9999);

		assertThat(findBestInsertion(insertion1, insertion2)).hasValue(insertion1);
		assertThat(findBestInsertion(insertion2, insertion1)).hasValue(insertion1);
	}

	@Test
	public void twoEqualInsertions_chooseLowerVehicleId() {
		var insertion1 = insertion("v1", 0, 0);
		whenInsertionThenCost(insertion1, 123);

		var insertion2 = insertion("v2", 0, 0);
		whenInsertionThenCost(insertion2, 123);

		assertThat(findBestInsertion(insertion1, insertion2)).hasValue(insertion1);
		assertThat(findBestInsertion(insertion2, insertion1)).hasValue(insertion1);
	}

	@Test
	public void twoEqualInsertions_sameVehicle_chooseLowerPickupIdx() {
		var insertion1 = insertion("v1", 0, 1);
		whenInsertionThenCost(insertion1, 123);

		var insertion2 = insertion("v1", 1, 1);
		whenInsertionThenCost(insertion2, 123);

		assertThat(findBestInsertion(insertion1, insertion2)).hasValue(insertion1);
		assertThat(findBestInsertion(insertion2, insertion1)).hasValue(insertion1);
	}

	@Test
	public void twoEqualInsertions_sameVehicle_samePickupIdx_chooseLowerDropoffIdx() {
		var insertion1 = insertion("v1", 0, 0);
		whenInsertionThenCost(insertion1, 123);

		var insertion2 = insertion("v1", 0, 1);
		whenInsertionThenCost(insertion2, 123);

		assertThat(findBestInsertion(insertion1, insertion2)).hasValue(insertion1);
		assertThat(findBestInsertion(insertion2, insertion1)).hasValue(insertion1);
	}

	@SafeVarargs
	private Optional<InsertionWithDetourData<Double>> findBestInsertion(InsertionWithDetourData<Double>... insertions) {
		return bestInsertionFinder.findBestInsertion(request, Arrays.stream(insertions));
	}

	private void whenInsertionThenCost(InsertionWithDetourData<Double> insertion, double cost) {
		when(insertionCostCalculator.calculate(eq(request), eq(insertion))).thenReturn(cost);
	}

	@SuppressWarnings("unchecked")
	private InsertionWithDetourData<Double> insertion() {
		return (InsertionWithDetourData<Double>)mock(InsertionWithDetourData.class);
	}

	private InsertionWithDetourData<Double> insertion(String vehicleId, int pickupIdx, int dropoffIdx) {
		var vehicle = mock(DvrpVehicle.class);
		when(vehicle.getId()).thenReturn(Id.create(vehicleId, DvrpVehicle.class));
		var vehicleEntry = new VehicleEntry(vehicle, null, null);

		var pickupInsertion = new InsertionGenerator.InsertionPoint(pickupIdx, null, null, null);
		var dropoffInsertion = new InsertionGenerator.InsertionPoint(dropoffIdx, null, null, null);

		return new InsertionWithDetourData<Double>(
				new InsertionGenerator.Insertion(vehicleEntry, pickupInsertion, dropoffInsertion), null, null, null,
				null);
	}
}
