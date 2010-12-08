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

package org.matsim.pt.router;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;


/**
 * @author mrieser
 */
public final class TransitRouterNetwork implements Network {

	private final static Logger log = Logger.getLogger(TransitRouterNetwork.class);

	private final Map<Id, TransitRouterNetworkLink> links = new LinkedHashMap<Id, TransitRouterNetworkLink>();
	private final Map<Id, TransitRouterNetworkNode> nodes = new LinkedHashMap<Id, TransitRouterNetworkNode>();
	private QuadTree<TransitRouterNetworkNode> qtNodes = null;

	private long nextNodeId = 0;
	private long nextLinkId = 0;

	public static final class TransitRouterNetworkNode implements Node {

		public final TransitRouteStop stop;
		public final TransitRoute route;
		public final TransitLine line;
		final Id id;
		final Map<Id, TransitRouterNetworkLink> ingoingLinks = new LinkedHashMap<Id, TransitRouterNetworkLink>();
		final Map<Id, TransitRouterNetworkLink> outgoingLinks = new LinkedHashMap<Id, TransitRouterNetworkLink>();

		public TransitRouterNetworkNode(final Id id, final TransitRouteStop stop, final TransitRoute route, final TransitLine line) {
			this.id = id;
			this.stop = stop;
			this.route = route;
			this.line = line;
		}

		@Override
		public Map<Id, ? extends Link> getInLinks() {
			return this.ingoingLinks;
		}

		@Override
		public Map<Id, ? extends Link> getOutLinks() {
			return this.outgoingLinks;
		}

		@Override
		public boolean addInLink(final Link link) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addOutLink(final Link link) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Coord getCoord() {
			return this.stop.getStopFacility().getCoord();
		}

		@Override
		public Id getId() {
			return this.id;
		}

		public TransitRouteStop getStop() {
			return stop;
		}

		public TransitRoute getRoute() {
			return route;
		}

		public TransitLine getLine() {
			return line;
		}
	}

	public static final class TransitRouterNetworkLink implements Link {

		public final TransitRouterNetworkNode fromNode;
		public final TransitRouterNetworkNode toNode;
		final TransitRoute route;
		final TransitLine line;
		final Id id;
		private final double length;

		public TransitRouterNetworkLink(final Id id, final TransitRouterNetworkNode fromNode, final TransitRouterNetworkNode toNode, final TransitRoute route, final TransitLine line) {
			this.id = id;
			this.fromNode = fromNode;
			this.toNode = toNode;
			this.route = route;
			this.line = line;
			this.length = CoordUtils.calcDistance(this.toNode.stop.getStopFacility().getCoord(), this.fromNode.stop.getStopFacility().getCoord());
		}

		@Override
		public TransitRouterNetworkNode getFromNode() {
			return this.fromNode;
		}

		@Override
		public TransitRouterNetworkNode getToNode() {
			return this.toNode;
		}

		@Override
		public double getCapacity() {
			return getCapacity(Time.UNDEFINED_TIME);
		}

		@Override
		public double getCapacity(final double time) {
			return 9999;
		}

		@Override
		public double getFreespeed() {
			return getFreespeed(Time.UNDEFINED_TIME);
		}

		@Override
		public double getFreespeed(final double time) {
			return 10;
		}

		@Override
		public Id getId() {
			return this.id;
		}

		@Override
		public double getNumberOfLanes() {
			return getNumberOfLanes(Time.UNDEFINED_TIME);
		}

		@Override
		public double getNumberOfLanes(final double time) {
			return 1;
		}

		@Override
		public double getLength() {
			return this.length;
		}

		@Override
		public void setCapacity(final double capacity) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setFreespeed(final double freespeed) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setFromNode(final Node node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setNumberOfLanes(final double lanes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setLength(final double length) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setToNode(final Node node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Coord getCoord() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> getAllowedModes() {
			return null;
		}

		@Override
		public void setAllowedModes(final Set<String> modes) {
			throw new UnsupportedOperationException();
		}

		public TransitRoute getRoute() {
			return route;
		}

		public TransitLine getLine() {
			return line;
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

	@Override
	public Map<Id, ? extends TransitRouterNetworkNode> getNodes() {
		return this.nodes;
	}

	@Override
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

	public Collection<TransitRouterNetworkNode> getNearestNodes(final Coord coord, final double distance) {
		return this.qtNodes.get(coord.getX(), coord.getY(), distance);
	}

	public TransitRouterNetworkNode getNearestNode(final Coord coord) {
		return this.qtNodes.get(coord.getX(), coord.getY());
	}

	@Override
	public double getCapacityPeriod() {
		return 3600.0;
	}

	@Override
	public NetworkFactory getFactory() {
		return null;
	}

	@Override
	public double getEffectiveLaneWidth() {
		return 3;
	}

	@Override
	public void addNode(Node nn) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addLink(Link ll) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Link removeLink(Id linkId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node removeNode(Id nodeId) {
		throw new UnsupportedOperationException();
	}

	public static TransitRouterNetwork createFromSchedule(final TransitSchedule schedule, final double maxBeelineWalkConnectionDistance) {
		final TransitRouterNetwork network = new TransitRouterNetwork();

		// build nodes and links connecting the nodes according to the transit routes
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				TransitRouterNetworkNode prevNode = null;
				for (TransitRouteStop stop : route.getStops()) {
					TransitRouterNetworkNode node = network.createNode(stop, route, line);
					if (prevNode != null) {
						TransitRouterNetworkLink link = network.createLink(prevNode, node, route, line);
					}
					prevNode = node;
				}
			}
		}
		network.finishInit(); // not nice to call "finishInit" here before we added all links...
		// in my view, it would be possible to completely do without finishInit: do the
		// additions to the central data structures as items come in, not near the end.  I would
		// prefer that because nobody could forget the "finishInit".  kai, apr'10
		// well, not really. finishInit creates the quadtree, for this, the extent must be known,
		// which is not at the very start, so the quadtree data structure cannot be updated as
		// links come in. mrieser, dec'10

		List<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>> toBeAdded = new LinkedList<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>>();
		// connect all stops with walking links if they're located less than beelineWalkConnectionDistance from each other
		for (TransitRouterNetworkNode node : network.getNodes().values()) {
			if (node.getInLinks().size() > 0) { // only add links from this node to other nodes if agents actually can arrive here
				for (TransitRouterNetworkNode node2 : network.getNearestNodes(node.stop.getStopFacility().getCoord(), maxBeelineWalkConnectionDistance)) {
					if ((node != node2) && (node2.getOutLinks().size() > 0)) { // only add links to other nodes when agents can depart there
						if ((node.line != node2.line) || (node.stop.getStopFacility() != node2.stop.getStopFacility())) {
							// do not yet add them to the network, as this would change in/out-links
							toBeAdded.add(new Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>(node, node2));
						}
					}
				}
			}
		}
		for (Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode> tuple : toBeAdded) {
			network.createLink(tuple.getFirst(), tuple.getSecond(), null, null);
		}

		log.info("transit router network statistics:");
		log.info(" # nodes: " + network.getNodes().size());
		log.info(" # links: " + network.getLinks().size());

		return network;
	}
}
