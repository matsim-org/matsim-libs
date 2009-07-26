/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.marcel.pt.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;



/**
 *
 *
 * @author mrieser
 */
/*package*/ class TransitRouterNetwork {

	private final List<TransitRouterNetworkLink> links = new ArrayList<TransitRouterNetworkLink>();
	private final List<TransitRouterNetworkNode> nodes = new ArrayList<TransitRouterNetworkNode>();
	private QuadTree<TransitRouterNetworkNode> qtNodes = null;

	public TransitRouterNetwork() {

	}

	/*package*/ static class TransitRouterNetworkNode {
		final TransitRouteStop stop;
		final TransitRoute route;
		final TransitLine line;
		final List<TransitRouterNetworkLink> outgoingLinks = new ArrayList<TransitRouterNetworkLink>();

		public TransitRouterNetworkNode(final TransitRouteStop stop, final TransitRoute route, final TransitLine line) {
			this.stop = stop;
			this.route = route;
			this.line = line;
		}
	}

	/*package*/ static class TransitRouterNetworkLink {
		final TransitRouterNetworkNode fromNode;
		final TransitRouterNetworkNode toNode;
		final TransitRoute route;
		final TransitLine line;
		public TransitRouterNetworkLink(final TransitRouterNetworkNode fromNode, final TransitRouterNetworkNode toNode, final TransitRoute route, final TransitLine line) {
			this.fromNode = fromNode;
			this.toNode = toNode;
			this.route = route;
			this.line = line;
		}
	}

	public TransitRouterNetworkNode createNode(final TransitRouteStop stop, final TransitRoute route, final TransitLine line) {
		final TransitRouterNetworkNode node = new TransitRouterNetworkNode(stop, route, line);
		this.nodes.add(node);
		return node;
	}

	public TransitRouterNetworkLink createLink(final TransitRouterNetworkNode fromNode, final TransitRouterNetworkNode toNode, final TransitRoute route, final TransitLine line) {
		final TransitRouterNetworkLink link = new TransitRouterNetworkLink(fromNode, toNode, route, line);
		this.links.add(link);
		fromNode.outgoingLinks.add(link);
		return link;
	}

	/*package*/ Collection<TransitRouterNetworkNode> getNodes() {
		return this.nodes;
	}

	/*package*/ Collection<TransitRouterNetworkLink> getLinks() {
		return this.links;
	}

	public void finishInit() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (TransitRouterNetworkNode node : this.nodes) {
			Coord c = node.stop.getStopFacility().getCoord();
			if (c.getX() < minX) {
				minX = c.getX();
			}
			if (c.getY() < minY) {
				minY = c.getY();
			}
			if (c.getX() > maxX) {
				maxX = c.getX();
			}
			if (c.getY() > maxY) {
				maxY = c.getY();
			}
		}

		QuadTree<TransitRouterNetworkNode> quadTree = new QuadTree<TransitRouterNetworkNode>(minX, minY, maxX, maxY);
		for (TransitRouterNetworkNode node : this.nodes) {
			Coord c = node.stop.getStopFacility().getCoord();
			quadTree.put(c.getX(), c.getY(), node);
		}
		this.qtNodes = quadTree;
	}

	public final Collection<TransitRouterNetworkNode> getNearestNodes(final Coord coord, final double distance) {
		return this.qtNodes.get(coord.getX(), coord.getY(), distance);
	}

	public TransitRouterNetworkNode getNearestNode(final Coord coord) {
		return this.qtNodes.get(coord.getX(), coord.getY());
	}
}
