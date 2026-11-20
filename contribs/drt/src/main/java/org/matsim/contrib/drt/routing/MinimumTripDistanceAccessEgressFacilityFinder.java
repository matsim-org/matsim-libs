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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.FilteringAccessEgressFacilityFinder;
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
public class MinimumTripDistanceAccessEgressFacilityFinder extends FilteringAccessEgressFacilityFinder {

	/**
	 * Provides the minimum trip distance that applies to a single routing request. It is queried after the access and
	 * egress facilities have been found, so that implementations can take them into account (e.g. to choose the
	 * constraints set by the access and egress links).
	 */
	public interface MinimumTripDistance {
		double get(RoutingRequest request, Facility accessFacility, Facility egressFacility);
	}

	private final MinimumTripDistance minimumTripDistance;
	private final Network network;

	public MinimumTripDistanceAccessEgressFacilityFinder(AccessEgressFacilityFinder delegate,
			MinimumTripDistance minimumTripDistance, Network network) {
		super(delegate);
		this.minimumTripDistance = minimumTripDistance;
		this.network = network;
	}

	@Override
	protected boolean isAllowed(RoutingRequest request, Facility access, Facility egress) {
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
