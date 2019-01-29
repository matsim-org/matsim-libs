/* *********************************************************************** *
 * project: org.matsim.*
 * LinkCarRouteFactory.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;

/**
 * @author mrieser
 */
public final class LinkNetworkRouteFactory implements RouteFactory {

	@Override
	public Route createRoute(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		return RouteUtils.createLinkNetworkRouteImpl(startLinkId, endLinkId);
	}

	@Override
	public String getCreatedRouteType() {
		return LinkNetworkRouteImpl.ROUTE_TYPE;
	}
	
}
