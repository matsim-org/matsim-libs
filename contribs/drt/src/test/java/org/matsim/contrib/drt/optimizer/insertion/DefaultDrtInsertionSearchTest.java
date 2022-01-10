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

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.insertion.DefaultDrtInsertionSearch.InsertionProvider;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourData.InsertionDetourData;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DefaultDrtInsertionSearchTest {
	@Test
	public void findBestInsertion_noInsertionsProvided() {
		var insertionSearch = new DefaultDrtInsertionSearch(mock(InsertionProvider.class), null, null, null);
		assertThat(insertionSearch.findBestInsertion(null, List.of())).isEmpty();
	}

	@Test
	public void findBestInsertion_oneInsertionsProvided() {
		var request = DrtRequest.newBuilder().build();
		var vehicleEntry = mock(VehicleEntry.class);

		//mock insertionProvider
		var insertion = mock(Insertion.class);
		var allInsertions = List.of(insertion);
		var insertionProvider = mock(InsertionProvider.class);
		when(insertionProvider.getInsertions(eq(request), eq(List.of(vehicleEntry)))).thenReturn(allInsertions);

		//mock detourPathCalculator
		var pathData = mock(DetourPathDataCache.class);
		var detourPathCalculator = mock(DetourPathCalculator.class);
		when(detourPathCalculator.calculatePaths(eq(request), eq(allInsertions))).thenReturn(pathData);
		var insertionPathData = new InsertionDetourData<PathData>(null, null, null, null);
		when(pathData.createInsertionDetourData(eq(insertion))).thenReturn(insertionPathData);

		//mock detourTimeCalculator
		var detourTimeCalculator = (InsertionDetourTimeCalculator<PathData>)mock(InsertionDetourTimeCalculator.class);
		var detourTimeInfo = new InsertionDetourTimeCalculator.DetourTimeInfo(null, null);
		when(detourTimeCalculator.calculateDetourTimeInfo(eq(insertion), eq(insertionPathData))).thenReturn(
				detourTimeInfo);

		//mock bestInsertionFinder
		var bestInsertionFinder = (BestInsertionFinder<PathData>)mock(BestInsertionFinder.class);
		var insertionWithPathData = new InsertionWithDetourData<>(insertion, insertionPathData, detourTimeInfo);
		when(bestInsertionFinder.findBestInsertion(eq(request),
				argThat(argument -> argument.map(insertionWithDetourData -> insertionWithDetourData.insertion)
						.collect(toSet())
						.equals(Set.of(insertion))))).thenReturn(Optional.of(insertionWithPathData));

		//test insertion search
		var insertionSearch = new DefaultDrtInsertionSearch(insertionProvider, detourPathCalculator,
				bestInsertionFinder, detourTimeCalculator);
		assertThat(insertionSearch.findBestInsertion(request, List.of(vehicleEntry))).hasValue(insertionWithPathData);
	}
}
