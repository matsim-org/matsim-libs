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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.optimizer.VehicleEntry;
import org.matsim.contrib.drt.optimizer.Waypoint;
import org.matsim.contrib.drt.optimizer.insertion.DefaultDrtInsertionSearch.InsertionProvider;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.drt.optimizer.insertion.InsertionGenerator.InsertionPoint;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DefaultDrtInsertionSearchTest {
	@Test
	public void findBestInsertion_noInsertionsProvided() {
		var insertionSearch = new DefaultDrtInsertionSearch(mock(InsertionProvider.class), null, null,
				new DrtConfigGroup(), new MobsimTimer());
		assertThat(insertionSearch.findBestInsertion(null, List.of())).isEmpty();
	}

	@Test
	public void findBestInsertion_twoInsertionsProvided() {
		var beforePickupLink = mock(Link.class);
		var afterPickupLink = mock(Link.class);
		var beforeDropoffLink = mock(Link.class);
		var afterDropoffLink = mock(Link.class);

		var request = DrtRequest.newBuilder().build();
		var vehicleEntry = mock(VehicleEntry.class);

		//mock insertionProvider
		var selectedInsertion = new Insertion(vehicleEntry, insertionPoint(beforePickupLink, afterPickupLink),
				insertionPoint(beforeDropoffLink, afterDropoffLink));
		var discardedInsertion = new Insertion(vehicleEntry, insertionPoint(beforePickupLink, afterPickupLink),
				insertionPoint(beforeDropoffLink, afterDropoffLink));
		var filteredInsertions = List.of(selectedInsertion, discardedInsertion);
		var insertionProvider = mock(InsertionProvider.class);
		when(insertionProvider.getInsertions(eq(request), eq(List.of(vehicleEntry)))).thenReturn(filteredInsertions);

		//mock detourPathCalculator
		var detourData = new DetourData<>(Map.of(beforePickupLink, mock(PathData.class)),
				Map.of(afterPickupLink, mock(PathData.class)), Map.of(beforeDropoffLink, mock(PathData.class)),
				Map.of(afterDropoffLink, mock(PathData.class)), PathData.EMPTY);
		var detourPathCalculator = mock(DetourPathCalculator.class);
		when(detourPathCalculator.calculatePaths(eq(request), eq(filteredInsertions))).thenReturn(detourData);

		//mock bestInsertionFinder
		var selectedInsertionWithPathData = detourData.createInsertionWithDetourData(selectedInsertion);
		@SuppressWarnings("unchecked")
		var bestInsertionFinder = (BestInsertionFinder<PathData>)mock(BestInsertionFinder.class);
		when(bestInsertionFinder.findBestInsertion(eq(request),
				argThat(argument -> argument.map(InsertionWithDetourData::getInsertion)
						.collect(toSet())
						.equals(Set.of(selectedInsertion, discardedInsertion))))).thenReturn(
				Optional.of(selectedInsertionWithPathData));

		//test insertion search
		var insertionSearch = new DefaultDrtInsertionSearch(insertionProvider, detourPathCalculator,
				bestInsertionFinder);
		assertThat(insertionSearch.findBestInsertion(request, List.of(vehicleEntry))).hasValue(
				selectedInsertionWithPathData);
	}

	private InsertionPoint insertionPoint(Link previousLink, Link nextLink) {
		var previousWaypoint = mock(Waypoint.class);
		when(previousWaypoint.getLink()).thenReturn(previousLink);
		var nextWaypoint = mock(Waypoint.class);
		when(nextWaypoint.getLink()).thenReturn(nextLink);
		return new InsertionPoint(-1, previousWaypoint, null, nextWaypoint);
	}
}
