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

import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.algorithms.SubsequentLinksAnalyzer;

public class CompressedNetworkRouteFactory implements RouteFactory {

	private Map<LinkImpl, LinkImpl> subsequentLinks = null;
	private NetworkLayer network;
	/**
	 * Uses {@link SubsequentLinksAnalyzer} to get the map of subsequent links,
	 * used to compress the route information stored.
	 *
	 * @param network
	 */
	public CompressedNetworkRouteFactory(final NetworkLayer network) {
		this.network = network;
	}


	public RouteWRefs createRoute(LinkImpl startLink, LinkImpl endLink) {
		if (this.subsequentLinks == null) {
			this.subsequentLinks = new SubsequentLinksAnalyzer(this.network).getSubsequentLinks();
		}
		return new CompressedNetworkRoute(startLink, endLink, this.subsequentLinks);
	}

}
