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

package org.matsim.network.algorithms;

import java.util.Iterator;

import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Link;
import org.matsim.network.NetworkLayer;

public class NetworkAdaptLength {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public NetworkAdaptLength() {
		super();
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	public void run(final NetworkLayer network) {
		System.out.println("    running " + this.getClass().getName() + " algorithm...");

		Iterator<? extends Link> l_it = network.getLinks().values().iterator();
		while (l_it.hasNext()) {
			Link l = l_it.next();
			Coord fc = l.getFromNode().getCoord();
			Coord tc   = l.getToNode().getCoord();
			double length = l.getLength();

			double dist = fc.calcDistance(tc);
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

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////
}
