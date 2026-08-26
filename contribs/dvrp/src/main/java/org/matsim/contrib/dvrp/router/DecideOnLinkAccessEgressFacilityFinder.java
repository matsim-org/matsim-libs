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
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DecideOnLinkAccessEgressFacilityFinder implements AccessEgressFacilityFinder {
	private final Network network;

	public DecideOnLinkAccessEgressFacilityFinder(Network network) {
		this.network = network;
	}

	@Override
	public Optional<Pair<Facility, Facility>> findFacilities(RoutingRequest request) {
		LinkWrapperFacility accessFacility = new LinkWrapperFacility(
				FacilitiesUtils.decideOnLink(request.getFromFacility(), network));
		LinkWrapperFacility egressFacility = new LinkWrapperFacility(
				FacilitiesUtils.decideOnLink(request.getToFacility(), network));
		return Optional.of(ImmutablePair.of(accessFacility, egressFacility));
	}
}
