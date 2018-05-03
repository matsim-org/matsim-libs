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

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;

import com.google.inject.Inject;

/**
 * @author michalm
 */
public class NearestStartLinkAsDepot implements DepotFinder {
	private final Set<Link> startLinks = new HashSet<>();

	@Inject
	public NearestStartLinkAsDepot(Fleet fleet) {
		for (Vehicle v : fleet.getVehicles().values()) {
			startLinks.add(v.getStartLink());
		}
	}

	// TODO a simple straight-line search (for the time being)... MultiNodeDijkstra should be the ultimate solution
	@Override
	public Link findDepot(Vehicle vehicle) {
		return Depots.findStraightLineNearestDepot(vehicle, startLinks);
	}
}
