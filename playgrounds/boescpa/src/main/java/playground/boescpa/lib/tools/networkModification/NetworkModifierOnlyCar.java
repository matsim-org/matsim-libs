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

package playground.boescpa.lib.tools.networkModification;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * WHAT IS IT FOR?
 * WHAT DOES IT?
 *
 * @author boescpa
 */
public class NetworkModifierOnlyCar extends AbstractNetworkModifier {
	public NetworkModifierOnlyCar(Coord center) {
		super(center);
	}

	public static void main(String[] args) {
		AbstractNetworkModifier networkModifier = new NetworkModifierOnlyCar(new Coord(682952.0, 247797.0)); // A bit south of HB Zurich...);
		networkModifier.run(args);
	}

	@Override
	public boolean isLinkAffected(Link link) {
		// Area:
		boolean isAffected = CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), center) <= radius ||
				CoordUtils.calcEuclideanDistance(link.getToNode().getCoord(), center) <= radius ||
				CoordUtils.calcEuclideanDistance(link.getCoord(), center) <= radius;
		// Mode:
		if (isAffected) {
			isAffected = link.getAllowedModes().contains("car");
		}
		return isAffected;
	}
}
