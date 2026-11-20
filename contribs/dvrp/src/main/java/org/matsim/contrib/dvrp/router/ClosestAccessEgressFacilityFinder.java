/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

import com.google.common.base.Verify;

/**
 * @author michalm
 */
public class ClosestAccessEgressFacilityFinder implements AccessEgressFacilityFinder {

	/**
	 * Provides the maximum access/egress distance that applies to a single routing request. This allows the maximum
	 * distance to depend on the trip, e.g. on the person or on the time of day, instead of being a global constant.
	 * <p>
	 * It is queried after the closest access and egress facilities have been determined, so that implementations may
	 * also take the found facilities (e.g. their links) into account.
	 */
	public interface MaxAccessEgressDistance {
		double get(RoutingRequest request, Facility accessFacility, Facility egressFacility);
	}

	/**
	 * Provides the maximum number of access and of egress stop candidates (including the closest stop) for a single
	 * routing request, see {@link #findCandidates}. Like {@link MaxAccessEgressDistance}, it is queried after the
	 * closest facilities have been determined.
	 */
	public interface MaxStopCandidates {
		int get(RoutingRequest request, Facility accessFacility, Facility egressFacility);
	}

	private final Network network;
	private final QuadTree<? extends Facility> facilityQuadTree;
	private final MaxAccessEgressDistance maxAccessEgressDistance;
	private final MaxStopCandidates maxStopCandidates;

	/**
	 * The maximum distance applied by {@link #findClosestStop(Facility)}, which has no routing request at hand.
	 */
	private final double defaultMaxDistance;

	public ClosestAccessEgressFacilityFinder(double maxDistance, Network network,
			QuadTree<? extends Facility> facilityQuadTree) {
		this((request, accessFacility, egressFacility) -> maxDistance, maxDistance, network, facilityQuadTree);
	}

	public ClosestAccessEgressFacilityFinder(MaxAccessEgressDistance maxAccessEgressDistance,
			double defaultMaxDistance, Network network, QuadTree<? extends Facility> facilityQuadTree) {
		this(maxAccessEgressDistance, (request, accessFacility, egressFacility) -> 1, defaultMaxDistance, network,
				facilityQuadTree);
	}

	/**
	 * @param maxStopCandidates maximum number of access and of egress candidates per trip; with 1 (the default of the
	 *                          other constructors), {@link #findCandidates} returns only the closest stops
	 */
	public ClosestAccessEgressFacilityFinder(MaxAccessEgressDistance maxAccessEgressDistance,
			MaxStopCandidates maxStopCandidates, double defaultMaxDistance, Network network,
			QuadTree<? extends Facility> facilityQuadTree) {
		this.maxStopCandidates = maxStopCandidates;
		this.network = network;
		this.facilityQuadTree = facilityQuadTree;
		this.maxAccessEgressDistance = maxAccessEgressDistance;
		this.defaultMaxDistance = defaultMaxDistance;
	}

	@Override
	public Optional<Pair<Facility, Facility>> findFacilities(RoutingRequest request) {
		Facility fromFacility = request.getFromFacility();
		Facility toFacility = request.getToFacility();

		// the closest facilities do not depend on the maximum distance, so they can be determined first and passed on
		// to the MaxAccessEgressDistance
		ClosestStop access = findClosestStopWithDistance(fromFacility);
		ClosestStop egress = findClosestStopWithDistance(toFacility);
		if (access == null || egress == null) {
			return Optional.empty();
		}

		double maxDistance = maxAccessEgressDistance.get(request, access.facility(), egress.facility());
		if (access.distance() > maxDistance || egress.distance() > maxDistance) {
			return Optional.empty();
		}

		return Optional.of(new ImmutablePair<>(access.facility(), egress.facility()));
	}

	/**
	 * Returns up to k access and k egress stops within the maximum distance, ordered by beeline distance (ties by
	 * facility id). The first entries are the stops returned by {@link #findFacilities}.
	 */
	@Override
	public Optional<AccessEgressFacilityCandidates> findCandidates(RoutingRequest request) {
		Optional<Pair<Facility, Facility>> closest = findFacilities(request);
		if (closest.isEmpty()) {
			return Optional.empty();
		}
		Facility accessFacility = closest.get().getLeft();
		Facility egressFacility = closest.get().getRight();

		int k = maxStopCandidates.get(request, accessFacility, egressFacility);
		Verify.verify(k >= 1, "maxStopCandidates must be at least 1: %s", k);
		if (k == 1) {
			return Optional.of(AccessEgressFacilityCandidates.of(accessFacility, egressFacility));
		}

		double maxDistance = maxAccessEgressDistance.get(request, accessFacility, egressFacility);
		return Optional.of(new AccessEgressFacilityCandidates(
				findStopsWithin(request.getFromFacility(), accessFacility, maxDistance, k),
				findStopsWithin(request.getToFacility(), egressFacility, maxDistance, k)));
	}

	private List<Facility> findStopsWithin(Facility facility, Facility closestStop, double maxDistance, int k) {
		Coord coord = getFacilityCoord(facility, network);
		List<Facility> stops = new ArrayList<>();
		stops.add(closestStop);
		facilityQuadTree.getDisk(coord.getX(), coord.getY(), maxDistance)
				.stream()
				.filter(stop -> stop != closestStop)
				.map(stop -> (Facility)stop)
				.sorted(Comparator.<Facility>comparingDouble(
								stop -> CoordUtils.calcEuclideanDistance(coord, stop.getCoord()))
						.thenComparing(ClosestAccessEgressFacilityFinder::facilityKey))
				.limit(k - 1)
				.forEach(stops::add);
		return stops;
	}

	private static String facilityKey(Facility facility) {
		return facility instanceof Identifiable<?> identifiable ?
				identifiable.getId().toString() :
				facility.getLinkId().toString();
	}

	public Facility findClosestStop(Facility facility) {
		return findClosestStop(facility, defaultMaxDistance);
	}

	/**
	 * @return the facility closest to the given facility, or {@code null} if it is farther away than {@code maxDistance}
	 */
	public Facility findClosestStop(Facility facility, double maxDistance) {
		ClosestStop closestStop = findClosestStopWithDistance(facility);
		return closestStop == null || closestStop.distance() > maxDistance ? null : closestStop.facility();
	}

	/**
	 * @return the facility closest to the given facility together with its distance, or {@code null} if there is no
	 * facility at all
	 */
	private ClosestStop findClosestStopWithDistance(Facility facility) {
		Coord coord = getFacilityCoord(facility, network);
		Facility closestStop = facilityQuadTree.getClosest(coord.getX(), coord.getY());
		return closestStop == null ?
				null :
				new ClosestStop(closestStop, CoordUtils.calcEuclideanDistance(coord, closestStop.getCoord()));
	}

	private record ClosestStop(Facility facility, double distance) {
	}

	static Coord getFacilityCoord(Facility facility, Network network) {
		Coord coord = facility.getCoord();
		if (coord == null) {
			coord = network.getLinks().get(facility.getLinkId()).getCoord();
			Verify.verify(coord != null, "From facility has neither coordinates nor link Id. Should not happen.");
		}
		return coord;
	}
}
