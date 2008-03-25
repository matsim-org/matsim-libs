/* *********************************************************************** *
 * project: org.matsim.*
 * RouteProvider.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.routeprovider;

import org.matsim.network.Link;
import org.matsim.plans.Route;

/**
 * @author jillenberger
 * @author dgrether
 *
 */
public interface RouteProvider {
	/**
	 * Requests the provider for a route from <tt>departureLink</tt> to <tt>destinationLink</tt>
	 * starting at time <tt>time</tt>.
	 *
	 * @param departureLink
	 *            the departure link.
	 * @param destinationLink
	 *            the destination link.
	 * @param time
	 *            the starting time in seconds.
	 * @return an instance of RouteI, or <tt>null</tt> if the provider was not
	 *         able to find an appropriate route or if the provider rejected
	 *         the request.
	 */
	public Route requestRoute(Link departureLink, Link destinationLink, double time);

	/**
	 * Each RouteProvider instance has a priority which should be a value in 0 .. 10, where
	 * 0 is the lowest priority, 10 the highest. Based on the Priority the route providers
	 * are chosen in the HierarchicalRouteProvider implementation.
	 * @return a priority in 0..10
	 */
	public int getPriority();
	/**
	 * Sets the priority of this route provider which should be a value between 0..10
	 * @param p int in 0..10
	 */
	public void setPriority(int p);

	public boolean providesRoute(Link currentLink, Route subRoute);


}
