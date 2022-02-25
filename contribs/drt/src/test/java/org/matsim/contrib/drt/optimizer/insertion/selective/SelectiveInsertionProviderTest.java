/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.insertion.selective;

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
import org.matsim.contrib.drt.optimizer.insertion.BestInsertionFinder;
import org.matsim.contrib.drt.optimizer.insertion.ForkJoinPoolTestRule;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SelectiveInsertionProviderTest {
	@Rule
	public final ForkJoinPoolTestRule rule = new ForkJoinPoolTestRule();

	private final BestInsertionFinder initialInsertionFinder = mock(BestInsertionFinder.class);

	@Test
	public void getInsertions_noInsertionsGenerated() {
		var insertionProvider = new SelectiveInsertionProvider(initialInsertionFinder,
				new InsertionGenerator(120, null), rule.forkJoinPool);
		assertThat(insertionProvider.getInsertion(null, List.of())).isEmpty();
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
		var insertionWithDetourData = new InsertionWithDetourData(new Insertion(vehicleEntry, null, null),
				new InsertionDetourData(null, null, null, null), null);
		var insertionGenerator = mock(InsertionGenerator.class);
		when(insertionGenerator.generateInsertions(eq(request), eq(vehicleEntry))).thenReturn(
				List.of(insertionWithDetourData));

		//mock initialInsertionFinder
		var selectedInsertion = oneSelected ?
				Optional.of(insertionWithDetourData) :
				Optional.<InsertionWithDetourData>empty();
		when(initialInsertionFinder.findBestInsertion(eq(request),
				argThat(argument -> argument.collect(toSet()).equals(Set.of(insertionWithDetourData)))))//
				.thenReturn(selectedInsertion);

		//test insertionProvider
		var insertionProvider = new SelectiveInsertionProvider(initialInsertionFinder, insertionGenerator,
				rule.forkJoinPool);
		assertThat(insertionProvider.getInsertion(request, List.of(vehicleEntry))).isEqualTo(selectedInsertion);
	}
}
