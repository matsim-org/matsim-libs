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
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkTransform;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.polettif.publicTransitMapping.mapping.networkRouter.FastAStarRouter;
import playground.polettif.publicTransitMapping.mapping.networkRouter.Router;

import java.util.*;

import static playground.polettif.publicTransitMapping.tools.ScheduleTools.getTransitRouteLinkIds;

/**
 * Provides Tools for analysing and manipulating networks.
 *
 * @author polettif
 */
@Deprecated
public class NetworkTools {

	protected static Logger log = Logger.getLogger(NetworkTools.class);

	private NetworkTools() {}

	public static Network readNetwork(String fileName) {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(fileName);
		return network;
	}

	public static void writeNetwork(Network network, String fileName) {
		new NetworkWriter(network).write(fileName);
	}

	public static Network createNetwork() {
		return NetworkUtils.createNetwork();
	}

	public static void transformNetwork(Network network, String fromCoordinateSystem, String toCoordinateSystem) {
		new NetworkTransform(TransformationFactory.getCoordinateTransformation(fromCoordinateSystem, toCoordinateSystem)).run(network);
	}

	public static void transformNetworkFile(String networkFile, String fromCoordinateSystem, String toCoordinateSystem) {
		log.info("... Transformig network from " + fromCoordinateSystem + " to " + toCoordinateSystem);
		Network network = readNetwork(networkFile);
		transformNetwork(network, fromCoordinateSystem, toCoordinateSystem);
		writeNetwork(network, networkFile);
	}

	/**
	 * Returns the nearest link for the given coordinate.
	 * Looks for nodes within search radius of coord (using,
	 * fetches all in- and outlinks returns the link with the smallest distance
	 * to the given coordinate. If there are two opposite links, the link with
	 * the coordinate on its right side is returned.<p/>
	 *
	 * @param network (instance of NetworkImpl)
	 * @param coord   the coordinate
	 */
	public static Link getNearestLink(Network network, Coord coord) {
		if(network instanceof Network) {
			Network networkImpl = (Network) network;
			double nodeSearchRadius = 1000.0;

			Link closestLink = null;
			double minDistance = Double.MAX_VALUE;
			final Coord coord1 = coord;
			final double distance = nodeSearchRadius;

			Collection<Node> nearestNodes = NetworkUtils.getNearestNodes(networkImpl,coord1, distance);

			while(nearestNodes.size() == 0) {
				nodeSearchRadius *= 2;
				final Coord coord2 = coord;
				final double distance1 = nodeSearchRadius;
				nearestNodes = NetworkUtils.getNearestNodes(networkImpl,coord2, distance1);
			}
			// check every in- and outlink of each node
			for(Node node : nearestNodes) {
				Set<Link> links = new HashSet<>(node.getOutLinks().values());
				links.addAll(node.getInLinks().values());
				double lineSegmentDistance;

				for(Link link : links) {
					// only use links with a viable network transport mode
					lineSegmentDistance = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);

					if(lineSegmentDistance < minDistance) {
						minDistance = lineSegmentDistance;
						closestLink = link;
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
	 * Looks for nodes within search radius of <tt>coord</tt>,
	 * fetches all in- and outlinks and sorts them ascending by their
	 * distance to the coordiantes given.
	 * <p/>
	 * The method then returns <tt>maxNLinks</tt> or all links within <tt>maxLinkDistance</tt>
	 * (whichever is reached earlier). Note: This method returns more than N links if two links have the same
	 * distance to the facility.
	 * <p/>
	 * Distance Link to Coordinate is calculated using {@link org.matsim.core.utils.geometry.CoordUtils#distancePointLinesegment}).

	 * @param network               	The network (must be instance of {@link Network})
	 * @param coord                 	the coordinate from which the closest links are
	 *                         			to be searched
	 * @param nodeSearchRadius        	Only links from and to nodes within this radius are considered.
	 * @param maxNLinks             	How many links should be returned.
	 * @param toleranceFactor       	After maxNLinks links have been found, additional links within
	 *                              	<tt>toleranceFactor</tt>*<tt>distance to the Nth link</tt>
	 *                              	are added to the set. Must be >= 1.
	 * @param networkTransportModes 	Only links with at least one of these transport modes are considered.
	 *                              	All links are considered if <tt>null</tt>.
	 * @param maxLinkDistance       	Only returns links which are closer than
	 *                         			this distance to the coordinate.
	 * @return list of the closest links from coordinate <tt>coord</tt>.
	 */
	public static List<Link> findClosestLinks(Network network, Coord coord, double nodeSearchRadius, int maxNLinks, double toleranceFactor, Set<String> networkTransportModes, double maxLinkDistance) {
		if(!(network instanceof Network)) {
			throw new IllegalArgumentException("network is not an instance of NetworkImpl");
		}

		List<Link> closestLinks = new ArrayList<>();
		final Coord coord1 = coord;
		final double distance = nodeSearchRadius;
		Collection<Node> nearestNodes = NetworkUtils.getNearestNodes(((Network) network),coord1, distance);

		if(nearestNodes.size() != 0) {
			// fetch every in- and outlink of each node
			HashSet<Link> links = new HashSet<>();
			for(Node node : nearestNodes) {
				links.addAll(node.getOutLinks().values());
				links.addAll(node.getInLinks().values());
			}

			SortedMap<Double, Set<Link>> closestLinksSortedByDistance = new TreeMap<>();

			// calculate lineSegmentDistance for all links
			double tolFactor = (toleranceFactor < 1 ? 1 : toleranceFactor);
			double maxSoftConstraintDistance = 0.0;
			for(Link link : links) {
				// only use links with a viable network transport mode
				if(networkTransportModes == null || MiscUtils.setsShareMinOneStringEntry(link.getAllowedModes(), networkTransportModes)) {
					double lineSegmentDistance = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);
					MapUtils.getSet(lineSegmentDistance, closestLinksSortedByDistance).add(link);
				}
			}

			int nLink = 0;
			for(Map.Entry<Double, Set<Link>> entry : closestLinksSortedByDistance.entrySet()) {
				if(entry.getKey() > maxLinkDistance) { break; }

				// when the link limit is reached, set the soft constraint distance
				if(nLink < maxNLinks && nLink+nLink+entry.getValue().size() >= maxNLinks) { maxSoftConstraintDistance = entry.getKey() * tolFactor; }

				// check if distance is greater than soft constraint distance
				if(nLink+entry.getValue().size() > maxNLinks && entry.getKey() > maxSoftConstraintDistance) { break; }

				// if no loop break has been reached, add link to list
				closestLinks.addAll(entry.getValue());
				nLink += entry.getValue().size();
			}
		}
		return closestLinks;
	}



	/**
	 * Creates a node and dummy/loop link on the coordinate of the stop facility and
	 * adds both to the network. The stop facility is NOT referenced.
	 *
	 * @return the new Link.
	 */
	public static Link createArtificialStopFacilityLink(TransitStopFacility stopFacility, Network network, String prefix, double freespeed, Set<String> transportModes) {
		NetworkFactory networkFactory = network.getFactory();

		Coord coord = stopFacility.getCoord();

		Node dummyNode = networkFactory.createNode(Id.createNodeId(prefix + stopFacility.getId() + "_node"), coord);
		Link dummyLink = networkFactory.createLink(Id.createLinkId(prefix + stopFacility.getId() + "_link"), dummyNode, dummyNode);

		dummyLink.setAllowedModes(transportModes);
		dummyLink.setLength(5);
		dummyLink.setFreespeed(freespeed);
		dummyLink.setCapacity(9999);

		if(!network.getNodes().containsKey(dummyNode.getId())) {
			network.addNode(dummyNode);
			network.addLink(dummyLink);
		}

		return dummyLink;
	}


	/**
 	 * Creates and returns a mode filtered network.
	 * @param network the input network, is not modified
	 * @param transportModes Links of the input network that share at least one network mode
	 *                       with this set are added to the new network. The returned network
	 *                       is empty if <tt>null</tt>.
	 * @return the filtered new network
	 */
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
		return CoordTools.coordIsOnRightSideOfLine(coord, link.getFromNode().getCoord(), link.getToNode().getCoord());
	}

	/**
	 * Checks if a link sequence has loops (i.e. the same link is passed twice).
	 */
	public static boolean linkSequenceHasLoops(List<Link> linkSequence) {
		Set tmpSet = new HashSet<>(linkSequence);
		return tmpSet.size() < linkSequence.size();
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
	 * Integrates <tt>network B</tt> into <tt>network A</tt>. Network
	 * A contains all links and nodes of both networks
	 * after integration.
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

	public static void shortenLink(Link link, Node toNode) {
		link.setToNode(toNode);
		link.setLength(CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), toNode.getCoord()));
	}

	public static void shortenLink(Node fromNode, Link link) {
		link.setFromNode(fromNode);
		link.setLength(CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), fromNode.getCoord()));
	}



	/**
	 * Sets the free speed of all links with the networkMode to the
	 * defined value.
	 */
	public static void setFreeSpeedOfLinks(Network network, String networkMode, double freespeedValue) {
		for(Link link : network.getLinks().values()) {
			if(link.getAllowedModes().contains(networkMode)) {
				link.setFreespeed(freespeedValue);
			}
		}
	}

	/**
	 * Resets the link length of all links with the given link Mode
	 */
	public static void resetLinkLength(Network network, String networkMode) {
		for(Link link : network.getLinks().values()) {
			if(link.getAllowedModes().contains(networkMode)) {
				double l = CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
				link.setLength(l > 0 ? l : 1);
			}
		}
	}

	/**
	 * Creates mode dependent routers based on the actual network modes used.
	 */
	public static Map<String, Router> guessRouters(TransitSchedule schedule, Network network) {
		Map<String, Set<String>> modeAssignments = new HashMap<>();
		for(TransitLine transitLine : schedule.getTransitLines().values()) {
			for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
				Set<String> usedNetworkModes = MapUtils.getSet(transitRoute.getTransportMode(), modeAssignments);
				List<Link> links = getLinksFromIds(network, getTransitRouteLinkIds(transitRoute));
				for(Link link : links) {
					usedNetworkModes.addAll(link.getAllowedModes());
				}
			}
		}

		Map<Set<String>, Router> modeDependentRouters = new HashMap<>();
		for(Set<String> networkModes : modeAssignments.values()) {
			if(!modeDependentRouters.containsKey(networkModes)) {
				modeDependentRouters.put(networkModes, FastAStarRouter.createModeSeparatedRouter(network, networkModes));
			}
		}

		Map<String, Router> routers = new HashMap<>();

		for(Map.Entry<String, Set<String>> e : modeAssignments.entrySet()) {
			routers.put(e.getKey(), modeDependentRouters.get(e.getValue()));
		}
		return routers;
	}

	/**
	 * Replaces all non-car link modes with "pt"
	 */
	public static void replaceNonCarModesWithPT(Network network) {
		log.info("... Replacing all non-car link modes with \"pt\"");

		Set<String> modesCar = Collections.singleton(TransportMode.car);

		Set<String> modesCarPt = new HashSet<>();
		modesCarPt.add(TransportMode.car);
		modesCarPt.add(TransportMode.pt);

		Set<String> modesPt = new HashSet<>();
		modesPt.add(TransportMode.pt);

		for(Link link : network.getLinks().values()) {
			if(link.getAllowedModes().size() == 0 && link.getAllowedModes().contains(TransportMode.car)) {
				link.setAllowedModes(modesCar);
			}
			if(link.getAllowedModes().size() > 0 && link.getAllowedModes().contains(TransportMode.car)) {
				link.setAllowedModes(modesCarPt);
			} else if(!link.getAllowedModes().contains(TransportMode.car)) {
				link.setAllowedModes(modesPt);
			}
		}
	}

	/**
	 * @return only links that have the same allowed modes set
	 */
	public static Set<Link> filterLinkSetExactlyByModes(Collection<? extends Link> links, Set<String> transportModes) {
		Set<Link> returnSet = new HashSet<>();
		for(Link l : links) {
			if(l.getAllowedModes().equals(transportModes)) {
				returnSet.add(l);
			}
		}
		return returnSet;
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
