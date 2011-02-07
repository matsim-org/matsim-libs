/* *********************************************************************** *
 * project: org.matsim.*
 * PathSizeFrom2Routes.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.choiceSetGeneration;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * use Path-Size concept to compare 2 Routes, Path-size == 1, if without
 * overlapping links; Path-size == 0.5, if the currentRoute comprises of
 * duplicating links from referenceRoute
 * 
 * @author yu
 * 
 */
public class PathSizeFrom2Routes {
	private NetworkRoute currentRoute, referenceRoute;
	private static Network network;
	private double pathSize;

	public static void setNetwork(Network network) {
		PathSizeFrom2Routes.network = network;
	}

	/**
	 * 
	 */
	public PathSizeFrom2Routes(NetworkRoute currentRoute,
			NetworkRoute referenceRoute) {
		this.currentRoute = currentRoute;
		this.referenceRoute = referenceRoute;
		pathSize = 0d;
		compareRoute();
	}

	public double getPathSize() {
		return pathSize;
	}

	protected void compareRoute() {
		List<Id> currentLinkIds = currentRoute.getLinkIds(), referenceLinkIds = referenceRoute
				.getLinkIds();
		if (currentLinkIds == null) {
			throw new RuntimeException("currentRoute of vehicle (ID:\t"
					+ currentRoute.getVehicleId() + ") has NULL linkIds!!");
		}
		if (referenceLinkIds == null) {
			throw new RuntimeException("referenceRoute of vehicle (ID:\t"
					+ referenceRoute.getVehicleId() + ") has NULL linkIds!!");
		}
		if (currentLinkIds.size() > 0) {
			double linkLengthSum = 0d;
			for (Id linkId : currentLinkIds) {
				double linkLength = network.getLinks().get(linkId).getLength();
				linkLengthSum += linkLength;
				if (referenceLinkIds.contains(linkId)/* commen Link */) {
					pathSize += 0.5/* 1d/2d */* linkLength;

				} else {
					pathSize += linkLength/*        *1d (unique/distinct) */;
				}
			}
			pathSize /= linkLengthSum;

		} else if (currentLinkIds.size() == 0) {
			if (referenceLinkIds.size() > 0) {
				pathSize = 1d;
				return;
			} else if (referenceLinkIds.size() == 0) {
				pathSize = 0.5;
				return;
			}
		}
	}

}
