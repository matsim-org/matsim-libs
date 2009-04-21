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

package org.matsim.core.population.routes;

import java.util.Map;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Route;
import org.matsim.core.network.algorithms.SubsequentLinksAnalyzer;

public class CompressedNetworkRouteFactory implements RouteFactory {

	private Map<Link, Link> subsequentLinks = null;
	private Network network;
	/**
	 * Uses {@link SubsequentLinksAnalyzer} to get the map of subsequent links,
	 * used to compress the route information stored.
	 *
	 * @param network
	 */
	public CompressedNetworkRouteFactory(final Network network) {
		this.network = network;
	}


	public Route createRoute(Link startLink, Link endLink) {
		if (this.subsequentLinks == null) {
			this.subsequentLinks = new SubsequentLinksAnalyzer(this.network).getSubsequentLinks();
		}
		return new CompressedNetworkRoute(startLink, endLink, this.subsequentLinks);
	}

}
