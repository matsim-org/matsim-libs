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

package org.matsim.population.routes;

import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Route;


/**
 * Creates new instances of {@link NodeCarRoute}.
 * 
 * @author mrieser
 */
public class NodeCarRouteFactory implements RouteFactory {


	public Route createRoute(Link startLink, Link endLink) {
		return new NodeCarRoute(startLink, endLink);
	}

	/**
	 * Method only available for backward compatibility. Make use
	 * of createRoute(Link, Link) method if possible.
	 */
	@Deprecated
	public Route createRoute() {
		return new NodeCarRoute();
	}

}
