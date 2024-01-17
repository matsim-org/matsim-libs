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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.DefaultTransitPassengerRouteFactory;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;

/**
 * @author mrieser / senozon
 */
public final class RouteFactories {
	private final Map<Class<? extends Route>, RouteFactory> routeFactories = new HashMap<>();
	private RouteFactory defaultFactory = new GenericRouteFactory();
	private final Map<String, Class<? extends Route>> type2class = new HashMap<>();

	public RouteFactories() {
		this.setRouteFactory(NetworkRoute.class, new LinkNetworkRouteFactory());
		this.setRouteFactory(ExperimentalTransitRoute.class, new ExperimentalTransitRouteFactory());
		this.setRouteFactory(DefaultTransitPassengerRoute.class, new DefaultTransitPassengerRouteFactory());
	}

	/**
	 * @param routeClass the requested class of the route
	 * @param startLinkId the link where the route starts
	 * @param endLinkId the link where the route ends
	 * @return a new Route of the specified route type
	 *
	 * @see #setRouteFactory(Class, RouteFactory)
	 */
	@SuppressWarnings("unchecked")
	public <R extends Route> R createRoute(final Class<R> routeClass, final Id<Link> startLinkId, final Id<Link> endLinkId) {
		return (R)this.routeFactories.getOrDefault(routeClass, defaultFactory).createRoute(startLinkId, endLinkId);
	}

	/**
	 * Registers a {@link RouteFactory} for the specified route type. If <code>factory</code> is <code>null</code>,
	 * the existing entry for this <code>routeClass</code> will be deleted. If <code>routeClass</code> is <code>null</code>,
	 * then the default factory is set that is used if no specific RouteFactory for a routeType is set.
	 *
	 * @param routeClass
	 * @param factory
	 */
	public void setRouteFactory(final Class<? extends Route> routeClass, final RouteFactory factory) {
		if (routeClass == null) {
			this.defaultFactory = factory;
		} else {
			if (factory == null) {
				this.routeFactories.remove(routeClass);
			} else {
				this.routeFactories.put(routeClass, factory);
				this.type2class.put(factory.getCreatedRouteType(), routeClass);
			}
		}
	}

	public Class<? extends Route> getRouteClassForType(String routeType) {
		//Route.class will result in a generic route
		return this.type2class.getOrDefault(routeType, Route.class);
	}
}
