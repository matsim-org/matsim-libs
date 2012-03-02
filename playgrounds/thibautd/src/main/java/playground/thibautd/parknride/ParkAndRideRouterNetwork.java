/* *********************************************************************** *
 * project: org.matsim.*
 * ParkNRideNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride;

import java.util.Collection;
import java.util.HashMap;
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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkLink;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Network based on the transit router network.
 * It agregates a car network with a pt network, with links from the car
 * part to the pt part representing park and ride stations.
 *
 * @author thibautd
 */
public class ParkAndRideRouterNetwork implements Network {
	private static final Logger log =
		Logger.getLogger(ParkAndRideRouterNetwork.class);

	private final Map<Id, TransitRouterNetworkNode> ptNodesPerStopId =
		new HashMap<Id, TransitRouterNetworkNode>();
	private final Map<Id, Node> nodes = new HashMap<Id, Node>();
	private final Map<Id, Link> links = new HashMap<Id, Link>();
	private final IdFactory ptLinkIdFactory = new IdFactory( "pt-link-" );
	private final IdFactory ptNodeIdFactory = new IdFactory( "pt-node-" );
	private final IdFactory pnrLinkIdFactory = new IdFactory( "pnr-link-" );

	// TODO: final!
	private final QuadTree<TransitRouterNetworkNode> transitNodesQuadTree;
	private final QuadTree<Node> carQuadTree;
	//private QuadTree<Node> carNodesQuadTree = null;

	/**
	 * @param carNetwork the regular network, to use for car routing
	 * @param transitNetwork the "network" used for transit routing. The ids of the
	 * links and nodes must be different of the ones from the car net!
	 * @param parkAndRideFacilities
	 */
	public ParkAndRideRouterNetwork(
			final Network carNetwork,
			final TransitSchedule schedule,
			final double maxBeelineWalkConnectionDistance,
			final ParkAndRideFacilities parkAndRideFacilities) {
		// TODO: decrease a little the uglyness by separating in mono-purpose methods
		log.info("registering car network");
		copyCarNetwork( carNetwork );

		carQuadTree = createCarQuadTree();

		log.info("start creating transit sub-network");
		final Counter linkCounter = new Counter(" new link #");
		final Counter nodeCounter = new Counter(" new node #");
		// build nodes and links connecting the nodes according to the transit routes
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				TransitRouterNetworkNode prevNode = null;
				for (TransitRouteStop stop : route.getStops()) {
					TransitRouterNetworkNode node = createTransitNode(stop, route, line);
					nodeCounter.incCounter();
					if (prevNode != null) {
						createTransitLink(prevNode, node, route, line);
						linkCounter.incCounter();
					}
					prevNode = node;
				}
			}
		}

		transitNodesQuadTree = createTransitQuadTree();

		// /////////////////////////////////////////////////////////////////////
		// transfer
		log.info("add transfer links");
		List<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>> transferTuples =
			new LinkedList<Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>>();
		// connect all stops with walking links if they're located less than beelineWalkConnectionDistance from each other
		for (Node genericNode : getNodes().values()) {
			if (genericNode instanceof TransitRouterNetworkNode) {
				TransitRouterNetworkNode node = (TransitRouterNetworkNode) genericNode;

				if (node.getInLinks().size() > 0) {
					// only add links from this node to other nodes if agents actually can arrive here
					for (TransitRouterNetworkNode node2 :
							getNearestTransitNodes(
								node.stop.getStopFacility().getCoord(),
								maxBeelineWalkConnectionDistance)) {

						if ((node != node2) && (node2.getOutLinks().size() > 0)) {
							// only add links to other nodes when agents can depart there
							if ((node.line != node2.line) ||
									(node.stop.getStopFacility() != node2.stop.getStopFacility())) {
								// do not yet add them to the network, as this would change in/out-links
								transferTuples.add(
										new Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode>(
											node,
											node2));
							}
						}
					}
				}
			}
		}

		for (Tuple<TransitRouterNetworkNode, TransitRouterNetworkNode> tuple : transferTuples) {
			createTransitLink(tuple.getFirst(), tuple.getSecond(), null, null);
			linkCounter.incCounter();
		}

		// /////////////////////////////////////////////////////////////////////
		// PnR links
		log.info( "creating PnR links" );
		Map<Id, ParkAndRideFacility> pnrFacilities = parkAndRideFacilities.getFacilities();
		int pnrLinksCount = 0;
		for (ParkAndRideFacility facility : pnrFacilities.values()) {
			List<Id> stops = facility.getStopsFacilitiesIds();
			//Node carNode = carQuadTree.get( facility.getCoord().getX() , facility.getCoord().getY() );
			// facility should reference a car link, as re-routing would not work
			// otherwise.
			Node carNode = links.get( facility.getLinkId() ).getToNode();

			for (Id stopId : stops) {
				createPnrLink(
							carNode,
							ptNodesPerStopId.get( stopId ),
							facility.getId());
				pnrLinksCount++;
				linkCounter.incCounter();
			}
		}

		log.info("PnR network statistics:");
		log.info(" # nodes: " + getNodes().size());
		log.info(" # links total:     " + getLinks().size());
		log.info(" # transfer links:  " + transferTuples.size());
		log.info(" # pnr links:  " + pnrLinksCount);
	}

	private void copyCarNetwork(final Network carNetwork) {
		nodeLoop:
		for (Node node : carNetwork.getNodes().values()) {
			for (Link link : node.getInLinks().values()) {
				if (link.getAllowedModes().contains( TransportMode.car )) {
					nodes.put( node.getId() , new WrappingNode( node ) );
					continue nodeLoop;
				}
			}
			for (Link link : node.getOutLinks().values()) {
				if (link.getAllowedModes().contains( TransportMode.car )) {
					nodes.put( node.getId() , new WrappingNode( node ) );
					continue nodeLoop;
				}
			}
		}
		for (Link link : carNetwork.getLinks().values()) {
			if (link.getAllowedModes().contains( TransportMode.car )) {
				links.put( link.getId() , new WrappingLink( link ) );
			}
		}
	}

	private QuadTree<Node> createCarQuadTree() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (Node node : this.nodes.values()) {
			if (node instanceof WrappingNode) {
				Coord c = node.getCoord();
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
		}

		QuadTree<Node> quadTree = new QuadTree<Node>(minX, minY, maxX, maxY);
		for (Node node : this.nodes.values()) {
			if (node instanceof WrappingNode) {
				Coord c = node.getCoord();
				quadTree.put(c.getX(), c.getY(), node);
			}
		}

		return quadTree;
	}

	private QuadTree<TransitRouterNetworkNode> createTransitQuadTree() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for (Node node : this.nodes.values()) {
			if (node instanceof TransitRouterNetworkNode) {
				Coord c = ((TransitRouterNetworkNode) node).stop.getStopFacility().getCoord();
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
		}

		QuadTree<TransitRouterNetworkNode> quadTree = new QuadTree<TransitRouterNetworkNode>(minX, minY, maxX, maxY);
		for (Node node : this.nodes.values()) {
			if (node instanceof TransitRouterNetworkNode) {
				TransitRouterNetworkNode transitNode = (TransitRouterNetworkNode) node;
				Coord c = transitNode.stop.getStopFacility().getCoord();
				quadTree.put(c.getX(), c.getY(), transitNode);
			}
		}

		return quadTree;
	}

	@Override
	public Map<Id, Node> getNodes() {
		return this.nodes;
	}

	@Override
	public Map<Id, Link> getLinks() {
		return this.links;
	}

	public Collection<TransitRouterNetworkNode> getNearestTransitNodes(final Coord coord, final double distance) {
		return this.transitNodesQuadTree.get(coord.getX(), coord.getY(), distance);
	}

	public TransitRouterNetworkNode getNearestTransitNode(final Coord coord) {
		return this.transitNodesQuadTree.get(coord.getX(), coord.getY());
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
	public void addNode(final Node nn) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addLink(final Link ll) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Link removeLink(final Id linkId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node removeNode(final Id nodeId) {
		throw new UnsupportedOperationException();
	}

	// /////////////////////////////////////////////////////////////////////////
	// helpers
	// /////////////////////////////////////////////////////////////////////////
	private TransitRouterNetworkNode createTransitNode(
			final TransitRouteStop stop,
			final TransitRoute route,
			final TransitLine line) {
		final TransitRouterNetworkNode node =
			new TransitRouterNetworkNode(
					ptNodeIdFactory.nextId(),
					stop,
					route,
					line);
		this.nodes.put(
				node.getId(),
				node);
		this.ptNodesPerStopId.put(
				stop.getStopFacility().getId(),
				node);
		return node;
	}

	private TransitRouterNetworkLink createTransitLink(
			final TransitRouterNetworkNode fromNode,
			final TransitRouterNetworkNode toNode,
			final TransitRoute route,
			final TransitLine line) {
		final TransitRouterNetworkLink link =
			new TransitRouterNetworkLink(
					ptLinkIdFactory.nextId(),
					fromNode,
					toNode,
					route,
					line);
		this.links.put(link.getId(), link);
		// ugly, but the add methods are not implemented...
		((Map<Id, Link>) fromNode.getOutLinks()).put( link.getId() , link );
		((Map<Id, Link>) toNode.getInLinks()).put( link.getId() , link );
		return link;
	}

	private ParkAndRideLink createPnrLink(
			final Node fromNode,
			final TransitRouterNetworkNode toNode,
			final Id pnrFacilityId) {
		final ParkAndRideLink link =
			new ParkAndRideLink(
					pnrLinkIdFactory.nextId(),
					fromNode,
					toNode,
					pnrFacilityId);
		this.links.put(
				link.getId(),
				link);
		fromNode.addOutLink( link );
		//toNode.addInLink( link );
		((Map<Id, Link>) toNode.getInLinks()).put( link.getId() , link );

		return link;
	}

	// /////////////////////////////////////////////////////////////////////////
	// classes
	// /////////////////////////////////////////////////////////////////////////
	private static class IdFactory {
		private final String prefix;
		private long count = Long.MIN_VALUE;

		public IdFactory(final String prefix) {
			this.prefix = prefix;
		}

		public Id nextId() {
			long n = count++;

			if (n == Long.MAX_VALUE) {
				throw new RuntimeException( "long overflow - no more available ids!" );
			}

			return new IdImpl( prefix + n );
		}
	}

	public static class ParkAndRideLink implements Link {
		private final Coord coord;
		private final Id id;
		private final Node fromNode;
		private final Node toNode;
		private final Id pnrFacilityId;

		private ParkAndRideLink(
				final Id id,
				final Node fromNode,
				final Node toNode,
				final Id pnrFacilityId) {
			this.id = id;
			this.fromNode = fromNode;
			this.toNode = toNode;

			this.coord = new CoordImpl(
					(fromNode.getCoord().getX() + toNode.getCoord().getX()) / 2d,
					(fromNode.getCoord().getY() + toNode.getCoord().getY()) / 2d);

			this.pnrFacilityId = pnrFacilityId;
		}

		@Override
		public Coord getCoord() {
			return coord;
		}

		@Override
		public Id getId() {
			return id;
		}

		@Override
		public Node getToNode() {
			return toNode;
		}

		@Override
		public Node getFromNode() {
			return fromNode;
		}

		@Override
		public Set<String> getAllowedModes() {
			return null;
		}

		public Id getParkAndRideFacilityId() {
			return pnrFacilityId;
		}

		// ////////////////////////////////////////////////////////////
		// unsupported
		// ////////////////////////////////////////////////////////////
		@Override
		public boolean setFromNode(final Node node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setToNode(final Node node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getLength() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getNumberOfLanes() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getNumberOfLanes(double time) {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getFreespeed() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getFreespeed(double time) {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getCapacity() {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getCapacity(double time) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setFreespeed(double freespeed) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setLength(double length) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setNumberOfLanes(double lanes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setCapacity(double capacity) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setAllowedModes(Set<String> modes) {
			throw new UnsupportedOperationException();
		}

	}

	private class WrappingNode implements Node {
		private final Node wrapped;
		private Map<Id, Link> inLinks = new HashMap<Id, Link>();
		private Map<Id, Link> outLinks = new HashMap<Id, Link>();
		private boolean linksAreImported = false;

		public WrappingNode(final Node wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public Id getId() {
			return wrapped.getId();
		}

		@Override
		public Coord getCoord() {
			return wrapped.getCoord();
		}

		@Override
		public boolean addInLink(final Link link) {
			inLinks.put( link.getId() , link );
			return true;
		}

		@Override
		public boolean addOutLink(final Link link) {
			outLinks.put( link.getId() , link);
			return true;
		}

		@Override
		public Map<Id, ? extends Link> getInLinks() {
			importLinks();
			return inLinks;
		}

		@Override
		public Map<Id, ? extends Link> getOutLinks() {
			importLinks();
			return outLinks;
		}

		private void importLinks() {
			if (!linksAreImported) {
				linksAreImported = true;
				
				for (Id id : wrapped.getInLinks().keySet()) {
					inLinks.put( id , links.get( id ) );
				}
				for (Id id : wrapped.getOutLinks().keySet()) {
					outLinks.put( id , links.get( id ) );
				}
			}
		}
	}

	private class WrappingLink implements Link {
		private final Link wrapped;
		private Node fromNode = null;
		private Node toNode = null;

		public WrappingLink(final Link link) {
			wrapped = link;
		}


		@Override
		public Node getToNode() {
			if (toNode == null) {
				toNode = nodes.get( wrapped.getToNode().getId() );
			}
			return toNode;
		}


		@Override
		public Node getFromNode() {
			if (fromNode == null) {
				fromNode = nodes.get( wrapped.getFromNode().getId() );
			}
			return fromNode;
		}

		// /////////////////////////////////////////////////////////////////////
		// setters: unsupported
		// /////////////////////////////////////////////////////////////////////
		@Override
		public boolean setFromNode(Node node) {
			throw new UnsupportedOperationException();
		}


		@Override
		public boolean setToNode(Node node) {
			throw new UnsupportedOperationException();
		}




		@Override
		public void setFreespeed(double freespeed) {
			throw new UnsupportedOperationException();
		}


		@Override
		public void setLength(double length) {
			throw new UnsupportedOperationException();
		}


		@Override
		public void setNumberOfLanes(double lanes) {
			throw new UnsupportedOperationException();
		}


		@Override
		public void setCapacity(double capacity) {
			throw new UnsupportedOperationException();
		}


		@Override
		public void setAllowedModes(Set<String> modes) {
			throw new UnsupportedOperationException();
		}

		// /////////////////////////////////////////////////////////////////////
		// delegate calls
		// /////////////////////////////////////////////////////////////////////
		@Override
		public Id getId() {
			return wrapped.getId();
		}

		@Override
		public Coord getCoord() {
			return wrapped.getCoord();
		}

		@Override
		public double getLength() {
			return wrapped.getLength();
		}

		@Override
		public double getNumberOfLanes() {
			return wrapped.getNumberOfLanes();
		}

		@Override
		public double getNumberOfLanes(double time) {
			return wrapped.getNumberOfLanes(time);
		}

		@Override
		public double getFreespeed() {
			return wrapped.getFreespeed();
		}

		@Override
		public double getFreespeed(double time) {
			return wrapped.getFreespeed(time);
		}

		@Override
		public double getCapacity() {
			return wrapped.getCapacity();
		}

		@Override
		public double getCapacity(double time) {
			return wrapped.getCapacity(time);
		}

		@Override
		public Set<String> getAllowedModes() {
			return wrapped.getAllowedModes();
		}


	}
}


