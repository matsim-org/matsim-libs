/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.depot;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;

import com.google.common.collect.ImmutableSet;

/**
 * @author michalm
 */
public class NearestStartLinkAsDepot implements DepotFinder {
	private final ImmutableSet<Link> startLinks;

	public NearestStartLinkAsDepot(Fleet fleet) {
		startLinks = fleet.getVehicles()
				.values()
				.stream()
				.map(DvrpVehicle::getStartLink)
				.collect(ImmutableSet.toImmutableSet());

	}

	// TODO a simple straight-line search (for the time being)... MultiNodeDijkstra should be the ultimate solution
	@Override
	public Link findDepot(DvrpVehicle vehicle) {
		return Depots.findStraightLineNearestDepot(vehicle, startLinks);
	}
}
