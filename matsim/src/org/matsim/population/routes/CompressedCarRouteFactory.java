/* *********************************************************************** *
 * project: org.matsim.*
 * CompressedCarRouteFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.population.routes;

import java.util.Map;

import org.matsim.interfaces.core.v01.Route;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.algorithms.SubsequentLinksAnalyzer;

public class CompressedCarRouteFactory implements RouteFactory {

	private final Map<Link, Link> subsequentLinks;

	/**
	 * Uses {@link SubsequentLinksAnalyzer} to get the map of subsequent links,
	 * used to compress the route information stored.
	 *
	 * @param network
	 */
	public CompressedCarRouteFactory(final NetworkLayer network) {
		this(new SubsequentLinksAnalyzer(network).getSubsequentLinks());
	}

	public CompressedCarRouteFactory(final Map<Link, Link> subsequentLinks) {
		this.subsequentLinks = subsequentLinks;
	}

	public Route createRoute(Link startLink, Link endLink) {
		return new CompressedCarRoute(startLink, endLink, this.subsequentLinks);
	}

}
