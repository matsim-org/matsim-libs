/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkAdaptLength.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.internal.NetworkRunnable;
import org.matsim.core.utils.geometry.CoordUtils;

public class NetworkAdaptLength implements NetworkRunnable {

	private static final double overLengthFactor = 1.001; // link length is at least 1 permil longer than euclidean distance

	private static final Logger log = Logger.getLogger(NetworkAdaptLength.class);

	@Override
	public void run(Network network) {
		log.info("running " + this.getClass().getName() + " module...");
		log.info("  adapting link length to at least 'overLengthFactor * euclidean distance' (works properly only for eucledian coord systems)");
		log.info("  also ceil link length to meters");
		log.info("  overLengthFactor: "+overLengthFactor);

		for (Link l : network.getLinks().values()) {
			double dist = overLengthFactor*CoordUtils.calcEuclideanDistance(l.getFromNode().getCoord(),l.getToNode().getCoord());
			if (dist > l.getLength()) { l.setLength(dist); }
			double len = Math.ceil(l.getLength());
			l.setLength(len);
		}

		log.info("done.");
	}
}
