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
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.Attributes;

import com.google.common.base.Verify;

/**
 * @author michalm
 */
public class ClosestAccessEgressFacilityFinder implements AccessEgressFacilityFinder {
	private final Network network;
	private final QuadTree<? extends Facility> facilityQuadTree;
	private final double maxDistance;

	public ClosestAccessEgressFacilityFinder(double maxDistance, Network network,
			QuadTree<? extends Facility> facilityQuadTree) {
		this.network = network;
		this.facilityQuadTree = facilityQuadTree;
		this.maxDistance = maxDistance;
	}

	@Override
	public Optional<Pair<Facility, Facility>> findFacilities(Facility fromFacility, Facility toFacility, Attributes tripAttributes) {
		Facility accessFacility = findClosestStop(fromFacility);
		if (accessFacility == null) {
			return Optional.empty();
		}

		Facility egressFacility = findClosestStop(toFacility);
		return egressFacility == null ?
				Optional.empty() :
				Optional.of(new ImmutablePair<>(accessFacility, egressFacility));
	}

	private Facility findClosestStop(Facility facility) {
		Coord coord = getFacilityCoord(facility, network);
		Facility closestStop = facilityQuadTree.getClosest(coord.getX(), coord.getY());
		double closestStopDistance = CoordUtils.calcEuclideanDistance(coord, closestStop.getCoord());
		return closestStopDistance > maxDistance ? null : closestStop;
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
