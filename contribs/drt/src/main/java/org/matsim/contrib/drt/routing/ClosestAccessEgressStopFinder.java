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

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule.AccessEgressStopFinder;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author michalm
 */
public class ClosestAccessEgressStopFinder implements AccessEgressStopFinder {

	private final Network network;
	private final QuadTree<TransitStopFacility> stopsQT;
	private final double maxDistance;

	public ClosestAccessEgressStopFinder(double maxDistance, Network network, QuadTree<TransitStopFacility> stopsQT) {
		this.network = network;
		this.stopsQT = stopsQT;
		this.maxDistance = maxDistance;
	}

	@Override
	public Pair<TransitStopFacility, TransitStopFacility> findStops(Facility fromFacility, Facility toFacility) {
		TransitStopFacility accessFacility = findClosestStop(fromFacility);
		if (accessFacility == null) {
			return new ImmutablePair<>(null, null);
		}

		TransitStopFacility egressFacility = findClosestStop(toFacility);
		return new ImmutablePair<>(accessFacility, egressFacility);
	}

	private TransitStopFacility findClosestStop(Facility facility) {
		Coord coord = StopBasedDrtRoutingModule.getFacilityCoord(facility, network);
		TransitStopFacility closestStop = stopsQT.getClosest(coord.getX(), coord.getY());
		double closestStopDistance = CoordUtils.calcEuclideanDistance(coord, closestStop.getCoord());
		if (closestStopDistance > maxDistance) {
			return null;
		}
		return stopsQT.getClosest(coord.getX(), coord.getY());
	}
}
