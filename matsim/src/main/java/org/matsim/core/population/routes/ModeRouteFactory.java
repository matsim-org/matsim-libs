/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.population.routes;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mrieser / senozon
 */
public class ModeRouteFactory {
	private final Map<String, RouteFactory> routeFactories = new HashMap<String, RouteFactory>();
	private RouteFactory defaultFactory = new GenericRouteFactory();

	public ModeRouteFactory() {
		this.routeFactories.put(TransportMode.car, new LinkNetworkRouteFactory());
		this.routeFactories.put(TransportMode.ride, new LinkNetworkRouteFactory());
		this.routeFactories.put(TransportMode.pt, new GenericRouteFactory());
	}
	
	/**
	 * @param transportMode the transport mode the route should be for
	 * @param startLinkId the link where the route starts
	 * @param endLinkId the link where the route ends
	 * @return a new Route for the specified mode
	 *
	 * @see #setRouteFactory(String, RouteFactory)
	 */
	public Route createRoute(final String transportMode, final Id<Link> startLinkId, final Id<Link> endLinkId) {
		RouteFactory factory = this.routeFactories.get(transportMode);
		if (factory == null) {
			factory = this.defaultFactory;
		}
		return factory.createRoute(startLinkId, endLinkId);
	}

	/**
	 * Registers a {@link RouteFactory} for the specified mode. If <code>factory</code> is <code>null</code>,
	 * the existing entry for this <code>mode</code> will be deleted. If <code>mode</code> is <code>null</code>,
	 * then the default factory is set that is used if no specific RouteFactory for a mode is set.
	 *
	 * @param transportMode
	 * @param factory
	 */
	public void setRouteFactory(final String transportMode, final RouteFactory factory) {
		if (transportMode == null) {
			this.defaultFactory = factory;
		} else {
			if (factory == null) {
				this.routeFactories.remove(transportMode);
			} else {
				this.routeFactories.put(transportMode, factory);
			}
		}
	}

}
