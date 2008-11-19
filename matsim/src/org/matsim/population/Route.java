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

package org.matsim.population;

import java.util.ArrayList;
import java.util.List;

import org.matsim.basic.v01.BasicRoute;
import org.matsim.network.Link;
import org.matsim.network.Node;

public interface Route extends BasicRoute {

	public abstract void setRoute(final String route);

	public abstract List<Node> getRoute();

	public abstract void setLinkRoute(List<Link> srcRoute);

	public abstract void setRoute(List<Node> srcRoute);

	public abstract void setRoute(final ArrayList<Node> route, final double travelTime, final double travelCost);

	public abstract double getTravelCost();

	/**
	 * Returns the list of links that build the route. The links where the route
	 * starts and ends (the links where the activities are on) are <b>not</b>
	 * included in the list.
	 * @return an array containing the links the agents plans to travel along
	 */
	public abstract Link[] getLinkRoute();

	/**
	 * This method returns a new Route object with the subroute of this beginning at fromNode
	 * till toNode. If from or twoNode are not found in this, an IllegalArgumentException is thrown.
	 * @param fromNode
	 * @param toNode
	 * @return A flat copy of the original Route
	 */
	public abstract Route getSubRoute(final Node fromNode, final Node toNode);

}
