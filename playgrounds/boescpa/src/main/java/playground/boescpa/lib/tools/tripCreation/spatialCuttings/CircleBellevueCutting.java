/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** */

package playground.boescpa.lib.tools.tripCreation.spatialCuttings;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Spatial cutting strategy for trip processing.
 * 
 * Circle Bellevue returns TRUE for all trips with start and/or end link
 * inside the circle around Bellevue with radius "cuttingRadius".
 * 
 * @author pboesch
 */
public class CircleBellevueCutting implements SpatialCuttingStrategy {
	
	private final int radius;
	private final Coord center = new Coord(683518.0, 246836.0);

	/**
	 * @param cuttingRadius [meters]
	 */
	public CircleBellevueCutting(int cuttingRadius) {
		this.radius = cuttingRadius;
	}

	@Override
	public boolean spatiallyConsideringTrip(Network network, Id startLink, Id endLink) {
		Link sLink = network.getLinks().get(startLink);
		Link eLink = network.getLinks().get(endLink); // could be null!
		
		if (CoordUtils.calcDistance(sLink.getCoord(), center) <= radius) {
			return true;
		} else if (eLink != null && (CoordUtils.calcDistance(eLink.getCoord(), center) <= radius)) {
			return true;
		} else {
			return false;
		}
	}

}
