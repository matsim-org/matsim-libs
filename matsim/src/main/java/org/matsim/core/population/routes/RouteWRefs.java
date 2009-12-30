/* *********************************************************************** *
 * project: org.matsim.*
 * Route.java
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

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;

/**
 * Defines the minimum amount of information a route in MATSim must provide.
 *
 * @author mrieser
 */
public interface RouteWRefs extends Route {

	public Link getStartLink();
	public void setStartLink(final Link link);

	public Link getEndLink();
	public void setEndLink(final Link link);

	/* make the clone method public, but do NOT implement Cloneable
	 * so that implementations can decide on their own if they support
	 * clone() or not.
	 */
	public RouteWRefs clone();

}
