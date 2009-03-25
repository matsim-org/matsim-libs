/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkAdaptLength.java
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

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.utils.geometry.CoordUtils;

public class NetworkAdaptLength {

	public void run(final Network network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		for (Link l : network.getLinks().values()) {
			Coord fc = l.getFromNode().getCoord();
			Coord tc   = l.getToNode().getCoord();
			double length = l.getLength();

			double dist = CoordUtils.calcDistance(fc, tc);
			if (dist > length) {
				l.setLength(dist);
				System.out.println("      link id=" + l.getId() + ": length=" + length + " set to eucledian dist=" + dist + ".");
			}

			// the following is just temporary (remove it later)
			if (l.getType().equals("90")) {
				l.setFreespeed(1.0/3.6);
				System.out.println("      link id=" + l.getId() + ", type=" + l.getType() + ": freespeed set to " + (1.0/3.6) + ".");
			}
		}

		System.out.println("    done.");
	}

}
