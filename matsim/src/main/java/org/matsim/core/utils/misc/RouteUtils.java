/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.misc;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRouteWRefs;

/**
 * Provides helper methods to work with routes.
 *
 * @author mrieser
 */
public class RouteUtils {

	/**
	 * Returns all nodes the route passes between the start- and the end-link of the route.
	 *
	 * @param route
	 * @param network
	 * @return
	 */
	public static List<Node> getNodes(final NetworkRouteWRefs route, final Network network) {
		List<Node> nodes = new ArrayList<Node>(route.getLinkIds().size() + 1);
		if ((route.getLinkIds().size() > 0)) {
			nodes.add(network.getLinks().get(route.getLinkIds().get(0)).getFromNode());
			for (Id linkId : route.getLinkIds()) {
				Link link = network.getLinks().get(linkId);
				nodes.add(link.getToNode());
			}
		} else if (!route.getStartLinkId().equals(route.getEndLinkId())) {
			nodes.add(network.getLinks().get(route.getStartLinkId()).getToNode());
		}
		return nodes;
	}
}
