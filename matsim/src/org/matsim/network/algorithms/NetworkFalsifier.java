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

package org.matsim.network.algorithms;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.geometry.shared.Coord;

/**
 * Falsifies a network, so it can more legally be redistributed, by moving the nodes by a random amount
 * north or south and east or west from their original point, but at most <code>distance</code> away.
 * Additionally, the link length will be set to the euclidean distance between the from- and to-node.
 *
 * @author mrieser
 */
public class NetworkFalsifier extends NetworkAlgorithm {

	private final double distance;

	public NetworkFalsifier(double distance) {
		this.distance = distance;
	}

	@Override
	public void run(NetworkLayer network) {
		double maxDistance = this.distance * 2.0;
		for (Node node : network.getNodes().values()) {
			Coord coord = node.getCoord();
			coord.setXY(coord.getX() + (Gbl.random.nextDouble() - 0.5) *  maxDistance,
					coord.getY() + (Gbl.random.nextDouble() - 0.5) * maxDistance);
		}

		for (Link link : network.getLinks().values()) {
			Coord fromCoord = link.getFromNode().getCoord();
			Coord toCoord = link.getToNode().getCoord();
			link.setLength(fromCoord.calcDistance(toCoord));
		}
	}

}
