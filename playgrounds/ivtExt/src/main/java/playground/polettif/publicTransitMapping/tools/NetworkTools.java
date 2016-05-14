/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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


package playground.polettif.publicTransitMapping.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.publicTransitMapping.config.PublicTransitMappingConfigGroup;
import playground.polettif.publicTransitMapping.mapping.router.LinkFilterMode;

import java.util.*;

import static playground.polettif.publicTransitMapping.tools.NetworkTools.coordIsOnRightSideOfLink;

/**
 * Provides Tools for analysing and manipulating networks.
 *
 * @author polettif
 */
public class NetworkTools {

	protected static Logger log = Logger.getLogger(NetworkTools.class);

	public static Network loadNetwork(String filePath) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(filePath);
		return network;
	}

	public static void writeNetwork(Network network, String filePath) {
		new NetworkWriter(network).write(filePath);
	}

	public static Network createNetwork() {
		return NetworkUtils.createNetwork();
	}

	public static void transformNetwork(Network network, String fromCoordinateSystem, String toCoordinateSystem) {
		new NetworkTransform(TransformationFactory.getCoordinateTransformation(fromCoordinateSystem, toCoordinateSystem)).run(network);
	}

	public static void transformNetworkFile(String networkFile, String fromCoordinateSystem, String toCoordinateSystem) {
		log.info("... Transformig network from " + fromCoordinateSystem + " to " + toCoordinateSystem);
		Network network = loadNetwork(networkFile);
		transformNetwork(network, fromCoordinateSystem, toCoordinateSystem);
		writeNetwork(network, networkFile);
	}

	/**
	 * Looks for nodes within search radius of coord (using {@link NetworkImpl#getNearestNodes(Coord, double)},
	 * fetches all in- and outlinks returns the link with the smallest distance
	 * to the given coordinate. If there are two opposite links, the link with
	 * the coordinate on the right side is returned.<p/>
	 *
	 * @param network (instance of NetworkImpl)
	 * @param coord   the coordinate
	 */
	public static Link getNearestLink(Network network, Coord coord) {
		if(network instanceof NetworkImpl) {
			NetworkImpl networkImpl = (NetworkImpl) network;
			double nodeSearchRadius = 200.0;

			Set<Link> visitedLinks = new HashSet<>();
			Link closestLink = null;
			double minDistance = Double.MAX_VALUE;

			Collection<Node> nearestNodes = networkImpl.getNearestNodes(coord, nodeSearchRadius);

			while(nearestNodes.size() == 0) {
				nodeSearchRadius *= 2;
				nearestNodes = networkImpl.getNearestNodes(coord, nodeSearchRadius);
			}
			// check every in- and outlink of each node
			for(Node node : nearestNodes) {
				Set<Link> links = new HashSet<>(node.getOutLinks().values());
				links.addAll(node.getInLinks().values());
				double lineSegmentDistance;

				for(Link outLink : links) {
					// only use links with a viable network transport mode
					lineSegmentDistance = CoordUtils.distancePointLinesegment(outLink.getFromNode().getCoord(), outLink.getToNode().getCoord(), coord);

					if(lineSegmentDistance < minDistance) {
						minDistance = lineSegmentDistance;
						closestLink = outLink;
					}

				}
			}

			// check for opposite link
			Link oppositeLink = getOppositeLink(closestLink);
			if(oppositeLink != null && !coordIsOnRightSideOfLink(coord, closestLink)) {
				return oppositeLink;
			} else {
				return closestLink;
			}

		} else {
			return null;
		}
	}

	/**
	 * Looks for nodes within search radius of coord (using {@link NetworkImpl#getNearestNodes(Coord, double)},
	 * fetches all in- and outlinks and sorts them ascending by their
	 * distance to the coordiantes given. Only returns maxNLinks or
	 * all links within maxLinkDistance (whichever is reached earlier).
	 * <p>
	 * <p/>
	 * Distance Link-Coordinate is calculated via  in {@link org.matsim.core.utils.geometry.CoordUtils#distancePointLinesegment(Coord, Coord, Coord)}).
	 *
	 * @param networkImpl      A network implementation
	 * @param coord            the coordinate from which the closest links are
	 *                         to be searched
	 * @param nodeSearchRadius Only links from and to nodes within this
	 *                         radius are considered
	 * @param maxNLinks        How many links should be returned. Note: Method
	 *                         an return more than n links if two links have the
	 *                         same distance from the facility.
	 * @param maxLinkDistance  Only returns links which are closer than
	 *                         this distance to the coordinate.
	 * @return the list of closest links
	 */
	public static List<Link> findNClosestLinks(NetworkImpl networkImpl, Coord coord, double nodeSearchRadius, int maxNLinks, double maxLinkDistance) {
		List<Link> closestLinks = new ArrayList<>();

		Collection<Node> nearestNodes = networkImpl.getNearestNodes(coord, nodeSearchRadius);
		SortedMap<Double, Link> closestLinksMap = new TreeMap<>();
		double incr = 0.0001;
		double tol = 0.001;

		if(nearestNodes.size() == 0) {
			return closestLinks;
		} else {
			// check every in- and outlink of each node
			for(Node node : nearestNodes) {
				Map<Id<Link>, ? extends Link> outLinks = node.getOutLinks();
				Map<Id<Link>, ? extends Link> inLinks = node.getInLinks();
				double lineSegmentDistance;

				for(Link outLink : outLinks.values()) {
					// check if link is already in the closestLinks set
					if(!closestLinksMap.containsValue(outLink)) {
						// only use links with a viable network transport mode
						lineSegmentDistance = CoordUtils.distancePointLinesegment(outLink.getFromNode().getCoord(), outLink.getToNode().getCoord(), coord);

						// since distance is used as key, we need to ensure the exact distance is not used already
						while(closestLinksMap.containsKey(lineSegmentDistance))
							lineSegmentDistance += incr;

						closestLinksMap.put(lineSegmentDistance, outLink);
					}
				}
				for(Link inLink : inLinks.values()) {
					if(!closestLinksMap.containsValue(inLink)) {
						lineSegmentDistance = CoordUtils.distancePointLinesegment(inLink.getFromNode().getCoord(), inLink.getToNode().getCoord(), coord);
						while(closestLinksMap.containsKey(lineSegmentDistance)) {
							lineSegmentDistance += incr;
						}
						closestLinksMap.put(lineSegmentDistance, inLink);
					}
				}
			}

			int i = 1;
			double previousDistance = 2 * tol;
			for(Map.Entry<Double, Link> entry : closestLinksMap.entrySet()) {
				// if the distance difference to the previous link is less than tol, add the link as well
				if(i > maxNLinks && Math.abs(entry.getKey() - previousDistance) >= tol) {
					break;
				}
				if(entry.getKey() > maxLinkDistance) {
					break;
				}

				previousDistance = entry.getKey();
				closestLinks.add(entry.getValue());
				i++;
			}

			return closestLinks;
		}
	}

	/**
	 * Looks for nodes within search radius of coord (using {@link NetworkImpl#getNearestNodes(Coord, double)},
	 * fetches all in- and outlinks and sorts them ascending by their
	 * distance to the coordiantes given. Only returns links with the allowed
	 * networkTransportMode for the input scheduleTransportMode (defined in
	 * config). Returns maxNLinks or all links within maxLinkDistance (whichever
	 * is reached earlier).
	 * <p>
	 * <p/>
	 * Distance Link-Coordinate is calculated via  in {@link org.matsim.core.utils.geometry.CoordUtils#distancePointLinesegment(Coord, Coord, Coord)}).
	 *
	 * @param networkImpl           A network implementation (needed
	 *                              for {@link NetworkImpl#getNearestNodes(Coord, double)}
	 * @param coord                 the coordinate from which the closest links
	 *                              are searched
	 * @param scheduleTransportMode the transport mode of the "current" transitRoute.
	 *                              The config should define which networkTransportModes
	 *                              are allowed for this scheduleMode
	 * @param config                The config defining maxNnodes, search radius etc.
	 * @return a Set of the closest links
	 */
	public static Set<Link> findClosestLinksByMode(NetworkImpl networkImpl, Coord coord, String scheduleTransportMode, PublicTransitMappingConfigGroup config) {
		Set<Link> closestLinks = new HashSet<>();

		Collection<Node> nearestNodes = networkImpl.getNearestNodes(coord, config.getNodeSearchRadius());
		SortedMap<Double, Link> closestLinksMap = new TreeMap<>();
		double incr = 0.001;
		double toleranceFactor = (config.getLinkDistanceTolerance() < 1 ? 1 : config.getLinkDistanceTolerance());
		int maxNLinks = config.getMaxNClosestLinks();

		Set<String> networkTransportModes = config.getModeRoutingAssignment().get(scheduleTransportMode);

		if(nearestNodes.size() == 0) {
			return closestLinks;
		} else {
			// check every in- and outlink of each node
			for(Node node : nearestNodes) {
				Set<Link> links = new HashSet<>(node.getInLinks().values());
				links.addAll(node.getOutLinks().values());

				double lineSegmentDistance;

				for(Link link : links) {
					// check if link is already in the closestLinks set
					if(!closestLinksMap.containsValue(link)) {
						// only use links with a viable network transport mode
						if(MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), networkTransportModes)) {
							lineSegmentDistance = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);

							// since distance is used as key, we need to ensure the exact distance is not used already
							while(closestLinksMap.containsKey(lineSegmentDistance))
								lineSegmentDistance += incr;

							closestLinksMap.put(lineSegmentDistance, link);
						}
					}
				}
			}

			int i = 1;
			double maxSoftDistance = Double.MAX_VALUE;
			for(Map.Entry<Double, Link> entry : closestLinksMap.entrySet()) {
				if(i == maxNLinks) {
					maxSoftDistance = (entry.getKey() + 2 * incr) * toleranceFactor;
				}

				// if the distance difference to the previous link is less than tol, add the link as well
				if(i > maxNLinks && entry.getKey() > maxSoftDistance) {
					break;
				}
				if(entry.getKey() > config.getMaxStopFacilityDistance()) {
					break;
				}
				closestLinks.add(entry.getValue());
				i++;
			}

			return closestLinks;
		}
	}


	/**
	 * Creates a node and dummy/loop link on the coordinate of the stop facility and
	 * adds both to the network. The stop facility is NOT referenced.
	 *
	 * @return the new Link.
	 */
	public static Link createArtificialStopFacilityLink(TransitStopFacility stopFacility, Network network, String prefix) {
		NetworkFactory networkFactory = network.getFactory();

		Coord coord = stopFacility.getCoord();

		Node dummyNode = networkFactory.createNode(Id.createNodeId(prefix + stopFacility.getId() + "_node"), coord);
		Link dummyLink = networkFactory.createLink(Id.createLinkId(prefix + stopFacility.getId() + "_link"), dummyNode, dummyNode);

		dummyLink.setAllowedModes(Collections.singleton(PublicTransitMappingConfigGroup.ARTIFICIAL_LINK_MODE));
		dummyLink.setLength(1.0);

		if(!network.getNodes().containsKey(dummyNode.getId())) {
			network.addNode(dummyNode);
			network.addLink(dummyLink);
		}

		return dummyLink;
	}


	public static Network filterNetworkByLinkMode(Network network, Set<String> transportModes) {
		NetworkFilterManager filterManager = new NetworkFilterManager(network);
		filterManager.addLinkFilter(new LinkFilter(transportModes));
		return filterManager.applyFilters();
	}

	public static Network filterNetworkExceptLinkMode(Network network, Set<String> transportModes) {
		NetworkFilterManager filterManager = new NetworkFilterManager(network);
		filterManager.addLinkFilter(new InverseLinkFilter(transportModes));
		return filterManager.applyFilters();
	}

	/**
	 * @return the opposite direction link. <tt>null</tt> if there is no opposite link.
	 */
	public static Link getOppositeLink(Link link) {
		if(link == null) {
			return null;
		}

		Link oppositeDirectionLink = null;
		Map<Id<Link>, ? extends Link> inLinks = link.getFromNode().getInLinks();
		if(inLinks != null) {
			for(Link inLink : inLinks.values()) {
				if(inLink.getFromNode().equals(link.getToNode())) {
					oppositeDirectionLink = inLink;
				}
			}
		}

		return oppositeDirectionLink;
	}

	/**
	 * @return true if the coordinate is on the right hand side of the link (or on the link).
	 */
	public static boolean coordIsOnRightSideOfLink(Coord coord, Link link) {
		double azLink = CoordTools.getAzimuth(link.getFromNode().getCoord(), link.getToNode().getCoord());
		double azToCoord = CoordTools.getAzimuth(link.getFromNode().getCoord(), coord);

		double diff = azToCoord-azLink;

		if(diff == 0 || azToCoord-Math.PI == azLink) {
			return true;
		} else if(diff > 0 && diff < Math.PI) {
			return true;
		} else if(diff > 0 && diff > Math.PI) {
			return false;
		} else if(diff < 0 && diff < -Math.PI){
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks if a link sequence has loops (i.e. the same link is passed twice).
	 *
	 * @param links
	 */
	public static boolean linkSequenceHasLoops(List<Link> links) {
		Set tmpSet = new HashSet<>(links);
		return tmpSet.size() < links.size();
	}


	/**
	 * Checks if a link sequence has u-turns (i.e. the opposite direction link is
	 * passed immediately after a link).
	 */
	public static boolean linkSequenceHasUTurns(List<Link> links) {
		for(int i = 1; i < links.size(); i++) {
			if(links.get(i).getToNode().equals(links.get(i - 1).getFromNode())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * A debug method to assign weights to network links as number of lanes.
	 * The network is changed permanently, so this should really only be used for
	 * debugging.
	 */
	public static void visualizeWeightsAsLanes(Network network, Map<Id<Link>, Double> weightMap) {
		for(Map.Entry<Id<Link>, Double> w : weightMap.entrySet()) {
			network.getLinks().get(w.getKey()).setNumberOfLanes(w.getValue());
		}
	}

	/**
	 * @return the network links from a given list of link ids
	 */
	public static List<Link> getLinksFromIds(Network network, List<Id<Link>> linkIds) {
		Map<Id<Link>, ? extends Link> links = network.getLinks();
		List<Link> list = new ArrayList<>();
		for(Id<Link> linkId : linkIds) {
			list.add(links.get(linkId));
		}
		return list;
	}

	/**
	 * Merges all network into baseNetworks. If a link id already
	 * exists in the base network, the link is not added to it.
	 *
	 * @param baseNetwork the network in which all other networks are integrated
	 * @param networks    collection of networks to merge into the base network
	 */
	public static void mergeNetworks(Network baseNetwork, Collection<Network> networks) {
		log.info("Merging networks...");

		int numberOfLinksBefore = baseNetwork.getLinks().size();
		int numberOfNodesBefore = baseNetwork.getNodes().size();

		for(Network currentNetwork : networks) {
			integrateNetwork(baseNetwork, currentNetwork);
		}

		log.info("... Total number of links added to network: " + (baseNetwork.getLinks().size() - numberOfLinksBefore));
		log.info("... Total number of nodes added to network: " + (baseNetwork.getNodes().size() - numberOfNodesBefore));
		log.info("Merging networks... done.");
	}

	/**
	 * Integrates network B into network A. Network
	 * A contains all links and nodes of both networks
	 * after integration.
	 *
	 * @param networkA
	 * @param networkB
	 */
	public static void integrateNetwork(final Network networkA, final Network networkB) {
		final NetworkFactory factory = networkA.getFactory();

		// Nodes
		for(Node node : networkB.getNodes().values()) {
			Id<Node> nodeId = Id.create(node.getId().toString(), Node.class);
			if(!networkA.getNodes().containsKey(nodeId)) {
				Node newNode = factory.createNode(nodeId, node.getCoord());
				networkA.addNode(newNode);
			}
		}

		// Links
		double capacityFactor = networkA.getCapacityPeriod() / networkB.getCapacityPeriod();
		for(Link link : networkB.getLinks().values()) {
			Id<Link> linkId = Id.create(link.getId().toString(), Link.class);
			if(!networkA.getLinks().containsKey(linkId)) {
				Id<Node> fromNodeId = Id.create(link.getFromNode().getId().toString(), Node.class);
				Id<Node> toNodeId = Id.create(link.getToNode().getId().toString(), Node.class);
				Link newLink = factory.createLink(linkId, networkA.getNodes().get(fromNodeId), networkA.getNodes().get(toNodeId));
				newLink.setAllowedModes(link.getAllowedModes());
				newLink.setCapacity(link.getCapacity() * capacityFactor);
				newLink.setFreespeed(link.getFreespeed());
				newLink.setLength(link.getLength());
				newLink.setNumberOfLanes(link.getNumberOfLanes());
				networkA.addLink(newLink);
			}
		}
	}

	/**
	 * Adds a node on the position of coord and connects it with two links to the neareast node of the network.
	 *
	 * @param coord     where the new node should be created
	 * @param network   that should be modified
	 * @param idPrefix  the prefix for the new node and links
	 * @param idCounter is simply appended to the idPrefix and incremented
	 * @return a list with the two newly created links
	 */
	@Deprecated
	public static List<Link> createLinkToNearestNode(Coord coord, Network network, String idPrefix, int idCounter) {
		NetworkImpl networkImpl = (NetworkImpl) network;
		NetworkFactory networkFactory = network.getFactory();

		Node newNode = networkFactory.createNode(Id.create(idPrefix + "node_" + idCounter, Node.class), coord);
		Node nearestNode = networkImpl.getNearestNode(coord);
		Link newLink = networkFactory.createLink(Id.createLinkId(idPrefix + idCounter + ":1"), newNode, nearestNode);
		Link newLink2 = networkFactory.createLink(Id.createLinkId(idPrefix + idCounter + ":2"), nearestNode, newNode);

		network.addNode(newNode);
		network.addLink(newLink);
		network.addLink(newLink2);

		List<Link> newLinks = new ArrayList<>();
		newLinks.add(newLink);
		newLinks.add(newLink2);

		return newLinks;
	}

	@Deprecated
	public static void shortenLink(Link link, Node toNode) {
		link.setToNode(toNode);
		link.setLength(CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), toNode.getCoord()));
	}

	@Deprecated
	public static void shortenLink(Node fromNode, Link link) {
		link.setFromNode(fromNode);
		link.setLength(CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), fromNode.getCoord()));
	}

	/**
	 * Looks for nodes within search radius of coord (using {@link NetworkImpl#getNearestNodes},
	 * fetches all in- and outlinks and sorts them ascending by their
	 * distance to the coordiantes given. Only returns maxNLinks or
	 * all links within maxLinkDistance (whichever is reached earlier).
	 * <p/>
	 * If N links are reached, additional links are added to the set
	 * if their distance is less than toleranceFactor * distance to the
	 * farthest link
	 * <p>
	 * <p/>
	 * Distance Link-Coordinate is calculated via  in {@link org.matsim.core.utils.geometry.CoordUtils#distancePointLinesegment(Coord, Coord, Coord)}).
	 *
	 * @param networkImpl      A network implementation
	 * @param coord            the coordinate from which the closest links are
	 *                         to be searched
	 * @param nodeSearchRadius Only links from and to nodes within this
	 *                         radius are considered
	 * @param maxNLinks        How many links should be returned. Note: Method
	 *                         an return more than n links if two links have the
	 *                         same distance from the facility.
	 * @param maxLinkDistance  Only returns links which are closer than
	 *                         this distance to the coordinate.
	 * @param toleranceFactor  [> 1]
	 * @return the list of closest links
	 */
	@Deprecated
	public static List<Link> findClosestLinksSoftConstraints(NetworkImpl networkImpl, Coord coord, double nodeSearchRadius, int maxNLinks, double maxLinkDistance, double toleranceFactor) {
		List<Link> closestLinks = new ArrayList<>();

		Collection<Node> nearestNodes = networkImpl.getNearestNodes(coord, nodeSearchRadius);
		SortedMap<Double, Link> closestLinksMap = new TreeMap<>();
		double incr = 0.001;
		if(toleranceFactor < 1)
			toleranceFactor = 1;

		if(nearestNodes.size() == 0) {
			return closestLinks;
		} else {
			// check every in- and outlink of each node
			for(Node node : nearestNodes) {
				Map<Id<Link>, ? extends Link> outLinks = node.getOutLinks();
				Map<Id<Link>, ? extends Link> inLinks = node.getInLinks();
				double lineSegmentDistance;

				for(Link outLink : outLinks.values()) {
					// check if link is already in the closestLinks set
					if(!closestLinksMap.containsValue(outLink)) {
						// only use links with a viable network transport mode
						lineSegmentDistance = CoordUtils.distancePointLinesegment(outLink.getFromNode().getCoord(), outLink.getToNode().getCoord(), coord);

						// since distance is used as key, we need to ensure the exact distance is not used already
						while(closestLinksMap.containsKey(lineSegmentDistance))
							lineSegmentDistance += incr;

						closestLinksMap.put(lineSegmentDistance, outLink);
					}
				}
				for(Link inLink : inLinks.values()) {
					if(!closestLinksMap.containsValue(inLink)) {
						lineSegmentDistance = CoordUtils.distancePointLinesegment(inLink.getFromNode().getCoord(), inLink.getToNode().getCoord(), coord);
						while(closestLinksMap.containsKey(lineSegmentDistance)) {
							lineSegmentDistance += incr;
						}
						closestLinksMap.put(lineSegmentDistance, inLink);
					}
				}
			}

			int i = 1;
			double maxSoftDistance = 0;
			for(Map.Entry<Double, Link> entry : closestLinksMap.entrySet()) {
				if(i == maxNLinks) {
					maxSoftDistance = (entry.getKey() + 2 * incr) * toleranceFactor;
				}

				// if the distance difference to the previous link is less than tol, add the link as well
				if(i > maxNLinks && Math.abs(entry.getKey()) > maxSoftDistance) {
					break;
				}
				if(entry.getKey() > maxLinkDistance) {
					break;
				}
				closestLinks.add(entry.getValue());
				i++;
			}

			return closestLinks;
		}
	}

	/**
	 * Sets the free speed of all links with the networkMode to the
	 * defined value.
	 *
	 * @param network
	 */
	public static void setFreeSpeedOfLinks(Network network, String networkMode, double freespeedValue) {
		for(Link link : network.getLinks().values()) {
			if(link.getAllowedModes().contains(networkMode)) {
				link.setFreespeed(freespeedValue);
			}
		}
	}

	/**
	 * Link filters by mode
	 */
	private static class LinkFilter implements NetworkLinkFilter {

		private final Set<String> modes;

		public LinkFilter(Set<String> modes) {
			this.modes = modes;
		}

		@Override
		public boolean judgeLink(Link l) {
			return MiscUtils.setsShareMinOneStringEntry(l.getAllowedModes(), modes);
		}
	}

	private static class InverseLinkFilter implements NetworkLinkFilter {

		private final Set<String> modes;

		public InverseLinkFilter(Set<String> modes) {
			this.modes = modes;
		}

		@Override
		public boolean judgeLink(Link l) {
			return !MiscUtils.setsShareMinOneStringEntry(l.getAllowedModes(), modes);
		}
	}

}
