/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;

/**
 * @author nkuehnel / MOIA
 */
class FilteringAccessEgressFacilityFinderTest {

	private final Facility a = facility("a");
	private final Facility b = facility("b");
	private final Facility c = facility("c");
	private final Facility x = facility("x");
	private final Facility y = facility("y");

	@Test
	void forbiddenPreferredPairRefusesTheTripAndItsCandidates() {
		var finder = filter(delegate(List.of(a, b), List.of(x, y)), Set.of("a-x"));

		assertThat(finder.findFacilities(null)).isEmpty();
		assertThat(finder.findCandidates(null)).isEmpty();
	}

	@Test
	void conditionOnOneSideIsAppliedExactly() {
		// every pair with access c is forbidden: c is dropped, all egress candidates are kept
		var finder = filter(delegate(List.of(a, b, c), List.of(x, y)), Set.of("c-x", "c-y"));

		assertThat(finder.findFacilities(null)).contains(new ImmutablePair<>(a, x));
		assertThat(finder.findCandidates(null)).hasValueSatisfying(candidates -> {
			assertThat(candidates.access()).containsExactly(a, b);
			assertThat(candidates.egress()).containsExactly(x, y);
		});
	}

	@Test
	void forbiddenPairOfAlternativesDropsOneOfTheirStops() {
		// only b-y is forbidden: every remaining combination must be allowed, so b (access side first) is dropped
		var finder = filter(delegate(List.of(a, b), List.of(x, y)), Set.of("b-y"));

		assertThat(finder.findCandidates(null)).hasValueSatisfying(candidates -> {
			assertThat(candidates.access()).containsExactly(a);
			assertThat(candidates.egress()).containsExactly(x, y);
		});
	}

	private static FilteringAccessEgressFacilityFinder filter(AccessEgressFacilityFinder delegate,
			Set<String> forbiddenPairs) {
		return new FilteringAccessEgressFacilityFinder(delegate) {
			@Override
			protected boolean isAllowed(RoutingRequest request, Facility access, Facility egress) {
				return !forbiddenPairs.contains(access.getLinkId() + "-" + egress.getLinkId());
			}
		};
	}

	private static AccessEgressFacilityFinder delegate(List<Facility> access, List<Facility> egress) {
		return new AccessEgressFacilityFinder() {
			@Override
			public Optional<Pair<Facility, Facility>> findFacilities(RoutingRequest request) {
				return Optional.of(new ImmutablePair<>(access.getFirst(), egress.getFirst()));
			}

			@Override
			public Optional<AccessEgressFacilityCandidates> findCandidates(RoutingRequest request) {
				return Optional.of(new AccessEgressFacilityCandidates(access, egress));
			}
		};
	}

	private static Facility facility(String linkId) {
		Coord coord = new Coord(0, 0);
		return new Facility() {
			@Override
			public Coord getCoord() {
				return coord;
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
