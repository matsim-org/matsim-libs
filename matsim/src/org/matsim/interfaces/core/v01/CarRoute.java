/* *********************************************************************** *
 * project: org.matsim.*
 * CarRoute.java
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

package org.matsim.interfaces.core.v01;

import java.util.List;

import org.matsim.network.Link;
import org.matsim.network.Node;

public interface CarRoute extends Route {

	/**
	 * Never use String arguments as parameter except they contain a real String, i.e. 
	 * textual information like a word or phrase.
	 * @param route
	 */
	@Deprecated 
	public abstract void setNodes(final String route);

	public abstract List<Node> getNodes();

	public abstract void setLinks(final Link startLink, final List<Link> srcRoute, final Link endLink);

	/**
	 * @param srcRoute
	 * @deprecated please use method {@link #setNodes(Link, List, Link)} which also specifies start and end Link
	 */
	@Deprecated
	public abstract void setNodes(final List<Node> srcRoute);
	
	public abstract void setNodes(final Link startLink, final List<Node> srcRoute, final Link endLink);

	public abstract void setTravelCost(final double travelCost);
	
	public abstract double getTravelCost();

	/**
	 * Returns the list of links that build the route. The links where the route
	 * starts and ends (the links where the activities are on) are <b>not</b>
	 * included in the list.
	 * @return a list containing the links the agents plans to travel along
	 */
	public abstract List<Link> getLinks();

	/**
	 * This method returns a new Route object with the subroute of this beginning at fromNode
	 * till toNode. If from or twoNode are not found in this, an IllegalArgumentException is thrown.
	 * @param fromNode
	 * @param toNode
	 * @return A flat copy of the original Route
	 */
	public abstract CarRoute getSubRoute(final Node fromNode, final Node toNode);

}
