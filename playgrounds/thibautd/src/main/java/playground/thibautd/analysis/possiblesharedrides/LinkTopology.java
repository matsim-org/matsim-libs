/* *********************************************************************** *
 * project: org.matsim.*
 * LinkTopology.java
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
package playground.thibautd.analysis.possiblesharedrides;

import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * Defines a topology on links.
 * A link (a,b) is neighbor of (c,d) if min[ d(x,y) ] &lt; dmax,
 * where d is the euclidean distance, x &isin; {a,b}, y &isin; {c,d},
 * dmax is an "acceptable distance".
 *
 * @author thibautd
 */
public class LinkTopology {

	public LinkTopology(
			final Network network,
			final double acceptableDistance) {
	}

	public List<Link> getNeighbors(Link link) {
		return null;
	}
}

