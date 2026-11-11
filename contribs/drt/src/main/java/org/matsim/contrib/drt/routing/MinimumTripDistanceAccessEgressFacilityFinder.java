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

package org.matsim.contrib.drt.routing;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.AccessEgressFacilityCandidates;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

/**
 * Decorates an {@link AccessEgressFacilityFinder} and refuses trips whose access and egress links are closer to each
 * other than a minimum distance. The trip is then not routed, and the routing module returns {@code null}. This is the
 * same signal as for a trip without a stop within reach, so a mode choice that only offers routable modes drops the
 * mode for that trip.
 * <p>
 * The distance is the beeline between the coordinates of the access and the egress link (see
 * {@link #calcTripDistance(Link, Link)}). Links are used rather than the facility coordinates so that the
 * {@link org.matsim.contrib.drt.passenger.DrtMinimumTripDistanceRequestValidator}, which only sees links, decides in
 * the same way.
 *
 * @author nkuehnel / MOIA
 */
public class MinimumTripDistanceAccessEgressFacilityFinder implements AccessEgressFacilityFinder {

	/**
	 * Provides the minimum trip distance that applies to a single routing request. It is queried after the access and
	 * egress facilities have been found, so that implementations can take them into account (e.g. to choose the
	 * constraints set by the access and egress links).
	 */
	public interface MinimumTripDistance {
		double get(RoutingRequest request, Facility accessFacility, Facility egressFacility);
	}

	private final AccessEgressFacilityFinder delegate;
	private final MinimumTripDistance minimumTripDistance;
	private final Network network;

	public MinimumTripDistanceAccessEgressFacilityFinder(AccessEgressFacilityFinder delegate,
			MinimumTripDistance minimumTripDistance, Network network) {
		this.delegate = delegate;
		this.minimumTripDistance = minimumTripDistance;
		this.network = network;
	}

	@Override
	public Optional<Pair<Facility, Facility>> findFacilities(RoutingRequest request) {
		return delegate.findFacilities(request).filter(facilities -> {
			Facility access = facilities.getLeft();
			Facility egress = facilities.getRight();
			double tripDistance = calcTripDistance(network.getLinks().get(access.getLinkId()),
					network.getLinks().get(egress.getLinkId()));
			return tripDistance >= minimumTripDistance.get(request, access, egress);
		});
	}

	/**
	 * Passes on the stop candidates of the delegate. As in {@link #findFacilities}, there are none if the preferred trip
	 * is too short. Alternative stops are kept only if every combination of the remaining access and egress candidates
	 * respects the minimum, so that the insertion search cannot choose a too short trip.
	 */
	@Override
	public Optional<AccessEgressFacilityCandidates> findCandidates(RoutingRequest request) {
		return delegate.findCandidates(request).flatMap(candidates -> {
			Facility preferredAccess = candidates.preferredAccess();
			Facility preferredEgress = candidates.preferredEgress();
			if (!isLongEnough(request, preferredAccess, preferredEgress)) {
				return Optional.empty();
			}
			// egress candidates that are long enough from the preferred access ...
			List<Facility> egress = candidates.egress().stream()
					.filter(e -> e == preferredEgress || isLongEnough(request, preferredAccess, e))
					.toList();
			// ... access candidates that are long enough to all of them ...
			List<Facility> access = candidates.access().stream()
					.filter(a -> a == preferredAccess || egress.stream().allMatch(e -> isLongEnough(request, a, e)))
					.toList();
			// ... and egress candidates that are long enough from all remaining access candidates
			List<Facility> finalEgress = egress.stream()
					.filter(e -> access.stream().allMatch(a -> isLongEnough(request, a, e)))
					.toList();
			return Optional.of(new AccessEgressFacilityCandidates(access, finalEgress));
		});
	}

	private boolean isLongEnough(RoutingRequest request, Facility access, Facility egress) {
		double tripDistance = calcTripDistance(network.getLinks().get(access.getLinkId()),
				network.getLinks().get(egress.getLinkId()));
		return tripDistance >= minimumTripDistance.get(request, access, egress);
	}

	/**
	 * @return the beeline distance between the coordinates of the two links, which is the distance compared against
	 * the minimum trip distance
	 */
	public static double calcTripDistance(Link fromLink, Link toLink) {
		return CoordUtils.calcEuclideanDistance(fromLink.getCoord(), toLink.getCoord());
	}
}
