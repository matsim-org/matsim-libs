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

import org.matsim.core.network.LinkImpl;


/**
 * Creates new instances of {@link NodeNetworkRoute}.
 * 
 * @author mrieser
 */
public class NodeNetworkRouteFactory implements RouteFactory {


	public RouteWRefs createRoute(LinkImpl startLink, LinkImpl endLink) {
		return new NodeNetworkRoute(startLink, endLink);
	}

	/**
	 * Method only available for backward compatibility. Make use
	 * of createRoute(Link, Link) method if possible.
	 */
	@Deprecated
	public RouteWRefs createRoute() {
		return new NodeNetworkRoute();
	}

}
