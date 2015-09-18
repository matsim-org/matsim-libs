/* *********************************************************************** *
 * project: org.matsim.*
 * RouteFactory.java
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
import org.matsim.core.api.internal.MatsimFactory;

/**
 * Provides a simple factory method to create new Route-objects.
 * Implement this interface to provide one specific implementation
 * of Routes.
 *
 * @author mrieser
 */
public interface RouteFactory extends MatsimFactory {
	public Route createRoute(Id<Link> startLinkId, Id<Link> endLinkId);
	
	/**
	 * @return the type of the {@link Route}s created by this factory.
	 */
	public String getCreatedRouteType();
}
