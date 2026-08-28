/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.router;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.TimeDependentAccessEgressFacilityFinder.TimeWindow;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;

/**
 * @author nkuehnel / MOIA
 */
public class TimeDependentAccessEgressFacilityFinderTest {

	private final Facility fromFacility = facility("from");
	private final Facility toFacility = facility("to");

	private final Facility accessStop = facility("accessStop");
	private final Facility egressStop = facility("egressStop");

	@Test
	void testDepartureWithinTheWindowIsDelegated() {
		AccessEgressFacilityFinder delegate = delegateReturningStops();
		var finder = new TimeDependentAccessEgressFacilityFinder(
				List.of(new TimeWindow(6 * 3600, 10 * 3600, delegate)));

		Optional<Pair<Facility, Facility>> facilities = finder.findFacilities(request(8 * 3600));

		assertThat(facilities).isPresent();
		assertThat(facilities.get().getLeft()).isSameAs(accessStop);
		assertThat(facilities.get().getRight()).isSameAs(egressStop);
	}

	@Test
	void testDepartureOutsideTheWindowDoesNotEvenAskTheDelegate() {
		AccessEgressFacilityFinder delegate = delegateReturningStops();
		var finder = new TimeDependentAccessEgressFacilityFinder(
				List.of(new TimeWindow(6 * 3600, 10 * 3600, delegate)));

		assertThat(finder.findFacilities(request(11 * 3600))).isEmpty();
		verifyNoInteractions(delegate);
	}

	@Test
	void testWindowIsHalfOpen() {
		AccessEgressFacilityFinder delegate = delegateReturningStops();
		var finder = new TimeDependentAccessEgressFacilityFinder(
				List.of(new TimeWindow(6 * 3600, 10 * 3600, delegate)));

		// startTime is inclusive, endTime is exclusive
		assertThat(finder.findFacilities(request(6 * 3600))).isPresent();
		assertThat(finder.findFacilities(request(10 * 3600 - 1))).isPresent();
		assertThat(finder.findFacilities(request(10 * 3600))).isEmpty();
		assertThat(finder.findFacilities(request(6 * 3600 - 1))).isEmpty();
	}

	@Test
	void testEachWindowUsesItsOwnDelegate() {
		AccessEgressFacilityFinder morningDelegate = delegateReturningStops();
		AccessEgressFacilityFinder eveningDelegate = delegateReturningStops();
		var finder = new TimeDependentAccessEgressFacilityFinder(
				List.of(new TimeWindow(6 * 3600, 10 * 3600, morningDelegate),
						new TimeWindow(16 * 3600, 20 * 3600, eveningDelegate)));

		assertThat(finder.findFacilities(request(8 * 3600))).isPresent();
		verify(morningDelegate).findFacilities(any());
		verifyNoInteractions(eveningDelegate);

		assertThat(finder.findFacilities(request(18 * 3600))).isPresent();
		verify(eveningDelegate).findFacilities(any());
	}

	@Test
	void testGapBetweenTwoWindows() {
		var finder = new TimeDependentAccessEgressFacilityFinder(
				List.of(new TimeWindow(6 * 3600, 10 * 3600, delegateReturningStops()),
						new TimeWindow(16 * 3600, 20 * 3600, delegateReturningStops())));

		assertThat(finder.findFacilities(request(13 * 3600))).isEmpty();
	}

	@Test
	void testOpenStartAndOpenEnd() {
		var finder = new TimeDependentAccessEgressFacilityFinder(
				List.of(new TimeWindow(Double.NEGATIVE_INFINITY, 10 * 3600, delegateReturningStops()),
						new TimeWindow(16 * 3600, Double.POSITIVE_INFINITY, delegateReturningStops())));

		assertThat(finder.findFacilities(request(0))).isPresent();
		assertThat(finder.findFacilities(request(-100))).isPresent();
		assertThat(finder.findFacilities(request(48 * 3600))).isPresent();
		assertThat(finder.findFacilities(request(12 * 3600))).isEmpty();
	}

	@Test
	void testRoutingRequestIsPassedOnUnchanged() {
		AccessEgressFacilityFinder delegate = delegateReturningStops();
		var finder = new TimeDependentAccessEgressFacilityFinder(
				List.of(new TimeWindow(6 * 3600, 10 * 3600, delegate)));

		RoutingRequest request = request(8 * 3600);
		finder.findFacilities(request);

		verify(delegate).findFacilities(request);
	}

	@Test
	void testTimesBeyond24hDoNotWrapAround() {
		var finder = new TimeDependentAccessEgressFacilityFinder(
				List.of(new TimeWindow(24 * 3600, 26 * 3600, delegateReturningStops())));

		assertThat(finder.findFacilities(request(25 * 3600))).isPresent();
		// 1:00 of the same day is not covered by a window starting at 24:00
		assertThat(finder.findFacilities(request(3600))).isEmpty();
	}

	@Test
	void testDelegateFindingNothingResultsInNothing() {
		AccessEgressFacilityFinder delegate = mock(AccessEgressFacilityFinder.class);
		when(delegate.findFacilities(any())).thenReturn(Optional.empty());
		var finder = new TimeDependentAccessEgressFacilityFinder(
				List.of(new TimeWindow(6 * 3600, 10 * 3600, delegate)));

		assertThat(finder.findFacilities(request(8 * 3600))).isEmpty();
	}

	private AccessEgressFacilityFinder delegateReturningStops() {
		AccessEgressFacilityFinder delegate = mock(AccessEgressFacilityFinder.class);
		when(delegate.findFacilities(any())).thenReturn(Optional.of(Pair.of(accessStop, egressStop)));
		return delegate;
	}

	private RoutingRequest request(double departureTime) {
		return DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, departureTime, null);
	}

	private static Facility facility(String linkId) {
		return new Facility() {
			@Override
			public Coord getCoord() {
				return new Coord(0, 0);
			}

			@Override
			public Id<Link> getLinkId() {
				return Id.createLinkId(linkId);
			}

			@Override
			public Map<String, Object> getCustomAttributes() {
				return Map.of();
			}
		};
	}
}
