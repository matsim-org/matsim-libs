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

package org.matsim.population.routes;

import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Route;

/**
 * Provides a simple factory method to create new Route-objects.
 * Implement this interface to provide one specific implementation
 * of Routes.
 *
 * @author mrieser
 */
public interface RouteFactory {
	public Route createRoute(Link startLink, Link endLink);
}
