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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.FilteringAccessEgressFacilityFinder;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;

/**
 * Decorates an {@link AccessEgressFacilityFinder} and refuses trips whose access (pickup) and egress (dropoff)
 * facilities violate the {@link DrtPickupDropoffRestriction}. As with
 * {@link MinimumTripDistanceAccessEgressFacilityFinder}, the trip is then not routed, which is the same signal as for a
 * trip without a stop within reach. Stop candidates are filtered in the same way (see
 * {@link FilteringAccessEgressFacilityFinder}).
 */
public class PickupDropoffRestrictionAccessEgressFacilityFinder extends FilteringAccessEgressFacilityFinder {

	private final DrtPickupDropoffRestriction restriction;
	private final Network network;

	public PickupDropoffRestrictionAccessEgressFacilityFinder(AccessEgressFacilityFinder delegate,
			DrtPickupDropoffRestriction restriction, Network network) {
		super(delegate);
		this.restriction = restriction;
		this.network = network;
	}

	@Override
	protected boolean isAllowed(RoutingRequest request, Facility access, Facility egress) {
		return restriction.isAllowed(network.getLinks().get(access.getLinkId()),
				network.getLinks().get(egress.getLinkId()));
	}
}
