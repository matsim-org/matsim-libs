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
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

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
	public static List<Node> getNodes(final NetworkRoute route, final Network network) {
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

	public static List<Link> getLinksFromNodes(final List<Node> nodes) {
		ArrayList<Link> links = new ArrayList<Link>(nodes.size());
		Node prevNode = null;
		for (Node node : nodes) {
			if (prevNode != null) {
				Link foundLink = findLink(prevNode, node);
				if (foundLink != null) {
					links.add(foundLink);
				}
			}
			prevNode = node;
		}
		links.trimToSize();
		return links;
	}

	private static Link findLink(Node prevNode, Node node) {
		for (Link link : prevNode.getOutLinks().values()) {
			if (link.getToNode().equals(node)) {
				return link;
			}
		}
		return null;
	}

	public static NetworkRoute getSubRoute(final NetworkRoute route, final Node fromNode, final Node toNode, final Network network) {
		Id fromLinkId = null;
		Id toLinkId = null;

		List<Id> linkIds = new ArrayList<Id>(route.getLinkIds().size() + 2);
		linkIds.add(route.getStartLinkId());
		linkIds.addAll(route.getLinkIds());
		linkIds.add(route.getEndLinkId());
		for (Id linkId : linkIds) {
			Link link = network.getLinks().get(linkId);
			if (link.getToNode() == fromNode) {
				fromLinkId = link.getId();
			}
			if (link.getFromNode() == toNode) {
				toLinkId = link.getId();
				break; // we found the toLinkId, so we can stop searching, fromLinkId should be before after all
			}
		}

		return route.getSubRoute(fromLinkId, toLinkId);
	}


	/**
	 * Calculates the distance of the complete route, <b>excluding</b> the distance traveled
	 * on the start- and end-link of the route.
	 *
	 * @param route
	 * @param network
	 * @return
	 */
	public static double calcDistance(final NetworkRoute route, final Network network) {
		double dist = 0;
		for (Id linkId : route.getLinkIds()) {
			dist += network.getLinks().get(linkId).getLength();
		}
		return dist;
	}


	public static NetworkRoute createNetworkRoute(List<Id> routeLinkIds, final Network network) {
		Id startLinkId = routeLinkIds.get(0);
		List<Id> linksBetween = (routeLinkIds.size() > 2) ? routeLinkIds.subList(1, routeLinkIds.size() - 1) : new ArrayList<Id>(0);
		Id endLinkId = routeLinkIds.get(routeLinkIds.size() - 1);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(startLinkId, endLinkId, network);
		route.setLinkIds(startLinkId, linksBetween, endLinkId);
		return route;
	}

}
