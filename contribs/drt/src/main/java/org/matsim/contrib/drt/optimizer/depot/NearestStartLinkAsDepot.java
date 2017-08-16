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

import java.util.*;

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.util.distance.DistanceUtils;

/**
 * @author michalm
 */
public class NearestStartLinkAsDepot implements DepotFinder {
	private final Set<Link> startLinks = new HashSet<>();

	public NearestStartLinkAsDepot(Fleet fleet) {
		for (Vehicle v : fleet.getVehicles().values()) {
			startLinks.add(v.getStartLink());
		}
	}

	// TODO a simple straight-line search (for the time being)... MultiNodeDijkstra should be the ultimate solution
	@Override
	public Link findDepot(Vehicle vehicle) {
		DrtStayTask currentTask = (DrtStayTask)vehicle.getSchedule().getCurrentTask();
		Link currentLink = currentTask.getLink();
		if (startLinks.contains(currentLink)) {
			return null;// stay where it is
		}

		Link bestLink = null;
		double bestDistance = Double.MAX_VALUE;
		for (Link l : startLinks) {
			double currentDistance = DistanceUtils.calculateSquaredDistance(currentLink.getCoord(), l.getCoord());
			if (currentDistance < bestDistance) {
				bestDistance = currentDistance;
				bestLink = l;
			}
		}

		return bestLink;
	}
}
