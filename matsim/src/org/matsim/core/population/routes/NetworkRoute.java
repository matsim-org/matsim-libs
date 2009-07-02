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

package org.matsim.core.population.routes;

import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Route;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;


/**
 * A route that describes a path in a (road) network. Thus, the route is described usually
 * as a series of links or nodes.
 *
 * @author mrieser
 */
public interface NetworkRoute extends Route {
	
	public LinkImpl getStartLink();

	public List<NodeImpl> getNodes();

	public void setLinks(final LinkImpl startLink, final List<LinkImpl> srcRoute, final LinkImpl endLink);

	/**
	 * @param srcRoute
	 * @deprecated please use method {@link #setNodes(LinkImpl, List, LinkImpl)} which also specifies start and end Link
	 */
	@Deprecated
	public void setNodes(final List<NodeImpl> srcRoute);

	public void setNodes(final LinkImpl startLink, final List<NodeImpl> srcRoute, final LinkImpl endLink);

	public void setTravelCost(final double travelCost);

	public double getTravelCost();

	/**
	 * Returns the list of links that build the route. The links where the route
	 * starts and ends (the links where the activities are on) are <b>not</b>
	 * included in the list.
	 * @return a list containing the links the agents plans to travel along
	 */
	public List<LinkImpl> getLinks();

	public List<Id> getLinkIds();
	
	/**
	 * This method returns a new Route object with the subroute of this beginning at fromNode
	 * till toNode. If from or twoNode are not found in this, an IllegalArgumentException is thrown.
	 * @param fromNode
	 * @param toNode
	 * @return A flat copy of the original Route
	 */
	public NetworkRoute getSubRoute(final NodeImpl fromNode, final NodeImpl toNode);

}
