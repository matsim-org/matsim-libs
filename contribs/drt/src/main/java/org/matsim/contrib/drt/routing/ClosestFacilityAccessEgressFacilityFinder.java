/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.drt.routing;

import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

/**
 * @author michalm
 */
public class ClosestFacilityAccessEgressFacilityFinder implements AccessEgressFacilityFinder {
	private final Network network;
	private final QuadTree<? extends Facility> facilityQT;
	private final double maxDistance;

	public ClosestFacilityAccessEgressFacilityFinder(double maxDistance, Network network,
			QuadTree<? extends Facility> facilityQT) {
		if (facilityQT.size() == 0) {
			throw new IllegalArgumentException("Empty facility QuadTree");
		}

		this.network = network;
		this.facilityQT = facilityQT;
		this.maxDistance = maxDistance;
	}

	@Override
	public Optional<Pair<Facility, Facility>> findFacilities(Facility fromFacility, Facility toFacility) {
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
		Coord coord = StopBasedDrtRoutingModule.getFacilityCoord(facility, network);
		Facility closestStop = facilityQT.getClosest(coord.getX(), coord.getY());
		double closestStopDistance = CoordUtils.calcEuclideanDistance(coord, closestStop.getCoord());
		return closestStopDistance > maxDistance ? null : closestStop;
	}
}
