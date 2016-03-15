/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.polettif.boescpa.analysis.spatialCutters;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Circle Point returns TRUE for all links
 * inside the circle around the specified point (xCoord, yCoord)
 * with radius cuttingRadius (FALSE else).
 *
 * @author boescpa
 */
public class CirclePointCutter implements SpatialCutter {

	private final int radius;
	private final Coord center;
	private final Map<Id, Boolean> linkCache;

	public CirclePointCutter(int cuttingRadius, double xCoord, double yCoord) {
		this.radius = cuttingRadius;
		this.center = new Coord(xCoord, yCoord);
		this.linkCache = new HashMap<>();
	}

	@Override
	public boolean spatiallyConsideringLink(Link link) {
		Boolean isConsidered = linkCache.get(link.getId());
		if (isConsidered == null) {
			isConsidered = false;
			if (CoordUtils.calcEuclideanDistance(link.getCoord(), center) <= radius) {
				isConsidered = true;
			}
			linkCache.put(link.getId(),isConsidered);
		}
		return isConsidered;
	}

	@Override
	public String toString() {
		return "Area: Circle around x = " + center.getX() + " and y = " + center.getY()
				+ " with radius = " + radius;
	}
}
