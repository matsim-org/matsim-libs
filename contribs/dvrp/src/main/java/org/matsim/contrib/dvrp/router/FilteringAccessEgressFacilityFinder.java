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

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;

/**
 * Base class for decorators that refuse some combinations of access and egress facility (e.g. too short trips, or
 * pickups outside an area). Subclasses only decide whether a pair is allowed; this class applies the decision to the
 * facilities and to the stop candidates of the delegate in the same way:
 * <ul>
 * <li>{@link #findFacilities}: the trip is refused (empty) if its pair is not allowed;</li>
 * <li>{@link #findCandidates}: no candidates if the preferred pair is not allowed; otherwise alternatives are kept only
 * if every remaining combination of access and egress candidates is allowed, so that the insertion search cannot choose
 * a forbidden pair. For conditions on one side only this is exact; for conditions on the pair it is conservative (a
 * stop may be dropped that would be allowed with some, but not all, candidates of the other side).</li>
 * </ul>
 * Deriving new filters from this class keeps them compatible with stop candidates.
 *
 * @author nkuehnel / MOIA
 */
public abstract class FilteringAccessEgressFacilityFinder implements AccessEgressFacilityFinder {

	private final AccessEgressFacilityFinder delegate;

	protected FilteringAccessEgressFacilityFinder(AccessEgressFacilityFinder delegate) {
		this.delegate = delegate;
	}

	/**
	 * @return true if a trip from the given access to the given egress facility is allowed
	 */
	protected abstract boolean isAllowed(RoutingRequest request, Facility access, Facility egress);

	@Override
	public final Optional<Pair<Facility, Facility>> findFacilities(RoutingRequest request) {
		return delegate.findFacilities(request)
				.filter(facilities -> isAllowed(request, facilities.getLeft(), facilities.getRight()));
	}

	@Override
	public final Optional<AccessEgressFacilityCandidates> findCandidates(RoutingRequest request) {
		return delegate.findCandidates(request).flatMap(candidates -> {
			Facility preferredAccess = candidates.preferredAccess();
			Facility preferredEgress = candidates.preferredEgress();
			if (!isAllowed(request, preferredAccess, preferredEgress)) {
				return Optional.empty();
			}
			// egress candidates allowed with the preferred access ...
			List<Facility> egress = candidates.egress().stream()
					.filter(e -> e == preferredEgress || isAllowed(request, preferredAccess, e))
					.toList();
			// ... access candidates allowed with all of them ...
			List<Facility> access = candidates.access().stream()
					.filter(a -> a == preferredAccess || egress.stream().allMatch(e -> isAllowed(request, a, e)))
					.toList();
			// ... and egress candidates allowed with all remaining access candidates
			List<Facility> finalEgress = egress.stream()
					.filter(e -> access.stream().allMatch(a -> isAllowed(request, a, e)))
					.toList();
			return Optional.of(new AccessEgressFacilityCandidates(access, finalEgress));
		});
	}
}
