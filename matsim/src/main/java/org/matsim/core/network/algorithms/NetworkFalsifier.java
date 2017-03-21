/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkFalsifier.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.network.algorithms;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Falsifies a network, so it can more legally be redistributed, by moving the nodes by a random amount
 * north or south and east or west from their original point, but at most <code>distance</code> away.
 * Additionally, the link length will be set to the euclidean distance between the from- and to-node.
 *
 * @author mrieser
 */
public final class NetworkFalsifier implements NetworkRunnable {

	private final double distance;

	public NetworkFalsifier(double distance) {
		this.distance = distance;
	}

	@Override
	public void run(Network network) {
		double maxDistance = this.distance * 2.0;
		for (Node node : network.getNodes().values()) {
			Coord coord = node.getCoord();
			node.setCoord( new Coord(coord.getX() + (MatsimRandom.getRandom().nextDouble() - 0.5) *  maxDistance,
					coord.getY() + (MatsimRandom.getRandom().nextDouble() - 0.5) * maxDistance) ) ;
		}

		for (Link link : network.getLinks().values()) {
			Coord fromCoord = link.getFromNode().getCoord();
			Coord toCoord = link.getToNode().getCoord();
			link.setLength(CoordUtils.calcEuclideanDistance(fromCoord, toCoord));
		}
	}

}
