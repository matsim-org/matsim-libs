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

import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Coord;
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

	private final Network network;
	private final QuadTree<? extends Facility> facilityQuadTree;
	private final MaxAccessEgressDistance maxAccessEgressDistance;

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
