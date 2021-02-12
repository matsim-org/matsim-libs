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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SelectiveInsertionProviderTest {
	@Rule
	public final ForkJoinPoolTestRule rule = new ForkJoinPoolTestRule();

	@SuppressWarnings("unchecked")
	private final BestInsertionFinder<Double> initialInsertionFinder = (BestInsertionFinder<Double>)mock(
			BestInsertionFinder.class);

	@Test
	public void getInsertions_noInsertionsGenerated() {
		var insertionProvider = new SelectiveInsertionProvider(null, initialInsertionFinder, new InsertionGenerator(),
				rule.forkJoinPool);
		assertThat(insertionProvider.getInsertions(null, List.of())).isEmpty();
	}

	@Test
	public void getInsertions_twoInsertionsGenerated_zeroSelected() {
		getInsertions_twoInsertionsGenerated(false);
	}

	@Test
	public void getInsertions_twoInsertionsGenerated_oneSelected() {
		getInsertions_twoInsertionsGenerated(true);
	}

	private void getInsertions_twoInsertionsGenerated(boolean oneSelected) {
		var request = DrtRequest.newBuilder().build();
		var vehicleEntry = mock(VehicleEntry.class);

		// mock insertionGenerator
		var insertion1 = new Insertion(vehicleEntry, insertionPoint(), insertionPoint());
		var insertion2 = new Insertion(vehicleEntry, insertionPoint(), insertionPoint());
		var insertionGenerator = mock(InsertionGenerator.class);
		when(insertionGenerator.generateInsertions(eq(request), eq(vehicleEntry))).thenReturn(
				List.of(insertion1, insertion2));

		//init restrictiveDetourTimeEstimator
		DetourTimeEstimator restrictiveDetourTimeEstimator = (from, to) -> 987.;

		//mock initialInsertionFinder
		var selectedInsertion = oneSelected ? Optional.of(insertion1) : Optional.<Insertion>empty();
		var selectedInsertionWithDetourData = selectedInsertion.map(
				DetourData.create(restrictiveDetourTimeEstimator, request)::createInsertionWithDetourData);
		when(initialInsertionFinder.findBestInsertion(eq(request),
				argThat(argument -> argument.map(InsertionWithDetourData::getInsertion)
						.collect(toSet())
						.equals(Set.of(insertion1, insertion2)))))//
				.thenReturn(selectedInsertionWithDetourData);

		//test insertionProvider
		var insertionProvider = new SelectiveInsertionProvider(restrictiveDetourTimeEstimator, initialInsertionFinder,
				insertionGenerator, rule.forkJoinPool);
		assertThat(insertionProvider.getInsertions(request, List.of(vehicleEntry))).isEqualTo(
				selectedInsertion.stream().collect(toList()));
	}

	private InsertionGenerator.InsertionPoint insertionPoint() {
		return new InsertionGenerator.InsertionPoint(-1, mock(Waypoint.class), null, mock(Waypoint.class));
	}
}
