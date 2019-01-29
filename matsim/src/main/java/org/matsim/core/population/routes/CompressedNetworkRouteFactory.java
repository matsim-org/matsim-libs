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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.algorithms.SubsequentLinksAnalyzer;

public final class CompressedNetworkRouteFactory implements RouteFactory {

	private Map<Id<Link>, Id<Link>> subsequentLinks = null;
	private final Network network;
	/**
	 * Uses {@link SubsequentLinksAnalyzer} to get the map of subsequent links,
	 * used to compress the route information stored.
	 *
	 * @param network
	 */
	public CompressedNetworkRouteFactory(final Network network) {
		this.network = network;
	}

	@Override
	public Route createRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		if ( network==null ) {
			throw new RuntimeException( "need to set Network in Population in order to be able to create compressed routes") ;
		}
		if (this.subsequentLinks == null) {
			this.subsequentLinks = new SubsequentLinksAnalyzer(this.network).getSubsequentLinks();
		}
		return new CompressedNetworkRouteImpl(startLinkId, endLinkId, this.network, this.subsequentLinks);
	}
	
	@Override
	public String getCreatedRouteType() {
		return "links";
	}

}
