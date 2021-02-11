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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.passenger.DrtRequest;

import com.google.common.collect.ImmutableTable;

/**
 * @author Michal Maciejewski (michalm)
 */
public class ExtensiveInsertionProviderTest {
	@Rule
	public final ForkJoinPoolTestRule rule = new ForkJoinPoolTestRule();

	@Test
	public void getInsertions_noInsertionsGenerated() {
		var insertionProvider = new ExtensiveInsertionProvider(null, null, null, new InsertionGenerator(),
				rule.forkJoinPool);
		assertThat(insertionProvider.getInsertions(null, List.of())).isEmpty();
	}

	@Test
	public void findBestInsertion_twoAtEndInsertionsGenerated_zeroNearestInsertionsAtEndLimit() {
		//the infeasible solution gets discarded in the first stage
		//the feasible solution gets discarded in the second stage (KNearestInsertionsAtEndFilter)
		findBestInsertion_twoInsertionsGenerated(0);
	}

	@Test
	public void findBestInsertion_twoAtEndInsertionsGenerated_tenNearestInsertionsAtEndLimit() {
		//the infeasible solution gets discarded in the first stage
		//the feasible solution is NOT discarded in the second stage (KNearestInsertionsAtEndFilter)
		findBestInsertion_twoInsertionsGenerated(10);
	}

	private void findBestInsertion_twoInsertionsGenerated(int nearestInsertionsAtEndLimit) {
		var beforePickupLink = mock(Link.class);
		var pickupLink = mock(Link.class);
		var afterPickupLink = mock(Link.class);
		var beforeDropoffLink = mock(Link.class);
		var dropoffLink = mock(Link.class);
		var afterDropoffLink = mock(Link.class);

		var request = DrtRequest.newBuilder().fromLink(pickupLink).toLink(dropoffLink).build();
		var vehicleEntry = mock(VehicleEntry.class);

		// mock insertionGenerator
		var feasibleInsertion = new InsertionGenerator.Insertion(vehicleEntry,
				insertionPoint(beforePickupLink, afterPickupLink), insertionPoint(beforeDropoffLink, afterDropoffLink));
		var infeasibleInsertion = new InsertionGenerator.Insertion(vehicleEntry,
				insertionPoint(beforePickupLink, afterPickupLink), insertionPoint(beforeDropoffLink, afterDropoffLink));
		var generatedInsertions = List.of(feasibleInsertion, infeasibleInsertion);
		var insertionGenerator = mock(InsertionGenerator.class);
		when(insertionGenerator.generateInsertions(eq(request), eq(vehicleEntry))).thenReturn(generatedInsertions);

		//init admissibleDetourTimeEstimator
		var ttMatrix = ImmutableTable.<Link, Link, Double>builder()//
				.put(beforePickupLink, pickupLink, 1.)
				.put(pickupLink, afterPickupLink, 2.)
				.put(beforeDropoffLink, dropoffLink, 3.)
				.put(dropoffLink, afterDropoffLink, 4.)
				.build();
		var admissibleDetourTimeEstimator = (DetourTimeEstimator)ttMatrix::get;

		//mock admissibleCostCalculator
		@SuppressWarnings("unchecked")
		var admissibleCostCalculator = (InsertionCostCalculator<Double>)mock(InsertionCostCalculator.class);
		when(admissibleCostCalculator.calculate(eq(request),
				argThat(argument -> argument.getInsertion() == feasibleInsertion))).thenReturn(1.);
		when(admissibleCostCalculator.calculate(eq(request),
				argThat(argument -> argument.getInsertion() == infeasibleInsertion))).thenReturn(
				InsertionCostCalculator.INFEASIBLE_SOLUTION_COST);

		//test insertion search
		var params = new ExtensiveInsertionSearchParams().setNearestInsertionsAtEndLimit(nearestInsertionsAtEndLimit);
		//pretend all insertions are at end to check KNearestInsertionsAtEndFilter
		when(vehicleEntry.isAfterLastStop(anyInt())).thenReturn(true);
		var insertionProvider = new ExtensiveInsertionProvider(params, admissibleCostCalculator,
				admissibleDetourTimeEstimator, insertionGenerator, rule.forkJoinPool);
		assertThat(insertionProvider.getInsertions(request, List.of(vehicleEntry))).isEqualTo(
				nearestInsertionsAtEndLimit == 0 ? List.of() : List.of(feasibleInsertion));
	}

	private InsertionGenerator.InsertionPoint insertionPoint(Link previousLink, Link nextLink) {
		var previousWaypoint = mock(Waypoint.class);
		when(previousWaypoint.getLink()).thenReturn(previousLink);
		var nextWaypoint = mock(Waypoint.class);
		when(nextWaypoint.getLink()).thenReturn(nextLink);
		return new InsertionGenerator.InsertionPoint(-1, previousWaypoint, null, nextWaypoint);
	}
}
