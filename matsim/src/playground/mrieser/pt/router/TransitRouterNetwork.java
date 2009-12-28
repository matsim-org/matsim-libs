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

package playground.mrieser.pt.router;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.world.Layer;


/**
 *
 *
 * @author mrieser
 */
/*package*/ class TransitRouterNetwork implements Network {

	private static final long serialVersionUID = 1L;

	private final Map<Id, TransitRouterNetworkLink> links = new LinkedHashMap<Id, TransitRouterNetworkLink>();
	private final Map<Id, TransitRouterNetworkNode> nodes = new LinkedHashMap<Id, TransitRouterNetworkNode>();
	private QuadTree<TransitRouterNetworkNode> qtNodes = null;

	private long nextNodeId = 0;
	private long nextLinkId = 0;

	public TransitRouterNetwork() {

	}

	/*package*/ static class TransitRouterNetworkNode implements Node {

		private static final long serialVersionUID = 1L;
		
		final TransitRouteStop stop;
		final TransitRoute route;
		final TransitLine line;
		final Id id;
		final Map<Id, TransitRouterNetworkLink> ingoingLinks = new LinkedHashMap<Id, TransitRouterNetworkLink>();
		final Map<Id, TransitRouterNetworkLink> outgoingLinks = new LinkedHashMap<Id, TransitRouterNetworkLink>();

		public TransitRouterNetworkNode(final Id id, final TransitRouteStop stop, final TransitRoute route, final TransitLine line) {
			this.id = id;
			this.stop = stop;
			this.route = route;
			this.line = line;
		}

		public Map<Id, ? extends Link> getInLinks() {
			return this.ingoingLinks;
		}

		public Map<Id, ? extends Link> getOutLinks() {
			return this.outgoingLinks;
		}

		public boolean addInLink(final Link link) {
			throw new UnsupportedOperationException();
		}

		public boolean addOutLink(final Link link) {
			throw new UnsupportedOperationException();
		}

		public Coord getCoord() {
			return this.stop.getStopFacility().getCoord();
		}

		public Id getId() {
			return this.id;
		}
	}

	/*package*/ static class TransitRouterNetworkLink implements Link {

		private static final long serialVersionUID = 1L;
		
		final TransitRouterNetworkNode fromNode;
		final TransitRouterNetworkNode toNode;
		final TransitRoute route;
		final TransitLine line;
		final Id id;
		public TransitRouterNetworkLink(final Id id, final TransitRouterNetworkNode fromNode, final TransitRouterNetworkNode toNode, final TransitRoute route, final TransitLine line) {
			this.id = id;
			this.fromNode = fromNode;
			this.toNode = toNode;
			this.route = route;
			this.line = line;
		}

		public TransitRouterNetworkNode getFromNode() {
			return this.fromNode;
		}

		public TransitRouterNetworkNode getToNode() {
			return this.toNode;
		}

		public double getCapacity(final double time) {
			return 9999;
		}

		public double getFreespeed(final double time) {
			return 10;
		}

		public Id getId() {
			return this.id;
		}

		public double getNumberOfLanes(final double time) {
			return 1;
		}

		public double getLength() {
			return CoordUtils.calcDistance(this.toNode.stop.getStopFacility().getCoord(), this.fromNode.stop.getStopFacility().getCoord());
		}

		public void setCapacity(final double capacity) {
			throw new UnsupportedOperationException();
		}

		public void setFreespeed(final double freespeed) {
			throw new UnsupportedOperationException();
		}

		public boolean setFromNode(final Node node) {
			throw new UnsupportedOperationException();
		}

		public void setNumberOfLanes(final double lanes) {
			throw new UnsupportedOperationException();
		}

		public void setLength(final double length) {
			throw new UnsupportedOperationException();
		}

		public boolean setToNode(final Node node) {
			throw new UnsupportedOperationException();
		}

		public Layer getLayer() {
			throw new UnsupportedOperationException();
		}

		public Coord getCoord() {
			throw new UnsupportedOperationException();
		}

		public Set<TransportMode> getAllowedModes() {
			return null;//allowedModes;
		}

		public void setAllowedModes(final Set<TransportMode> modes) {
			throw new UnsupportedOperationException();
		}

	}

	public TransitRouterNetworkNode createNode(final TransitRouteStop stop, final TransitRoute route, final TransitLine line) {
		final TransitRouterNetworkNode node = new TransitRouterNetworkNode(new IdImpl(this.nextNodeId++), stop, route, line);
		this.nodes.put(node.getId(), node);
		return node;
	}

	public TransitRouterNetworkLink createLink(final TransitRouterNetworkNode fromNode, final TransitRouterNetworkNode toNode, final TransitRoute route, final TransitLine line) {
		final TransitRouterNetworkLink link = new TransitRouterNetworkLink(new IdImpl(this.nextLinkId++), fromNode, toNode, route, line);
		this.links.put(link.getId(), link);
		fromNode.outgoingLinks.put(link.getId(), link);
		toNode.ingoingLinks.put(link.getId(), link);
		return link;
	}

	public Map<Id, ? extends TransitRouterNetworkNode> getNodes() {
		return this.nodes;
	}

	public Map<Id, ? extends TransitRouterNetworkLink> getLinks() {
		return this.links;
	}

	public void finishInit() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (TransitRouterNetworkNode node : this.nodes.values()) {
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
		for (TransitRouterNetworkNode node : this.nodes.values()) {
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

	public double getCapacityPeriod() {
		return 3600.0;
	}

	public NetworkFactory getFactory() {
		return null;
	}

	public double getEffectiveLaneWidth() {
		return 3;
	}

	public void addNode(Node nn) {
		// TODO Auto-generated method stub
		//
		throw new UnsupportedOperationException() ;
	}

	public void addLink(Link ll) {
		// TODO Auto-generated method stub
		//
		throw new UnsupportedOperationException() ;
	}

}
