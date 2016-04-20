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


package playground.polettif.multiModalMap.tools;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.filter.NetworkLinkFilter;
import org.matsim.core.network.filter.NetworkNodeFilter;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;

public class NetworkTools {

	protected static Logger log = Logger.getLogger(NetworkTools.class);

	/**
	 * Looks for nodes within search radius of coord (using {@link NetworkImpl#getNearestNodes(Coord, double)},
	 * fetches all in- and outlinks and sorts them ascending by their
	 * distance to the coordiantes given. Only returns maxNLinks or
	 * all links within maxLinkDistance (whichever is reached earlier).
	 *
	 * <p/>
	 * Distance Link-Coordinate is calculated via  in {@link org.matsim.core.utils.geometry.CoordUtils#distancePointLinesegment(Coord, Coord, Coord)}).
	 *
	 * @param networkImpl A network implementation
	 * @param coord the coordinate from which the closest links are
	 *              to be searched
	 * @param nodeSearchRadius Only links from and to nodes within this
	 *                         radius are considered
	 * @param maxNLinks How many links should be returned. Note: Method
	 *                  an return more than n links if two links have the
	 *                  same distance from the facility.
	 * @param maxLinkDistance Only returns links which are closer than
	 *                        this distance to the coordinate.
	 * @return the list of closest links
	 */
	public static List<Link> findNClosestLinks(NetworkImpl networkImpl, Coord coord, double nodeSearchRadius, int maxNLinks, double maxLinkDistance) {
		List<Link> closestLinks = new ArrayList<>();

		Collection<Node> nearestNodes = networkImpl.getNearestNodes(coord, nodeSearchRadius);
		SortedMap<Double, Link> closestLinksMap = new TreeMap<>();
		double incr = 0.0001; double tol=0.001;

		if(nearestNodes.size() == 0) {
			return closestLinks;
		} else {
			for (Node node : nearestNodes) {
				Map<Id<Link>, ? extends Link> outLinks = node.getOutLinks();
				Map<Id<Link>, ? extends Link> inLinks = node.getInLinks();
				double lineSegmentDistance;

				for (Link linkCandidate : outLinks.values()) {
					// check if link is already in the closestLinks set
					if (!closestLinksMap.containsValue(linkCandidate)) {
						lineSegmentDistance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), coord);

						// since distance is used as key, we need to ensure the exact distance is not used already TODO maybe check for side of the road?
						while (closestLinksMap.containsKey(lineSegmentDistance))
							lineSegmentDistance += incr;

						closestLinksMap.put(lineSegmentDistance, linkCandidate);
					}
				}
				for (Link linkCandidate : inLinks.values()) {
					if (!closestLinksMap.containsValue(linkCandidate)) {
						lineSegmentDistance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), coord);
						while (closestLinksMap.containsKey(lineSegmentDistance)) {
							lineSegmentDistance += incr;
						}
						closestLinksMap.put(lineSegmentDistance, linkCandidate);
					}
				}
			}

			int i = 1; double previousDistance = 2*tol;
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

	public static List<Link> findNClosestLinks(NetworkImpl networkImpl, Coord coord, int maxNClosestLinks, double nodeSearchRadius) {
		return findNClosestLinks(networkImpl, coord, nodeSearchRadius, maxNClosestLinks, Double.MAX_VALUE);
	}

	public static List<Link> findNClosestLinks(NetworkImpl networkImpl, Coord coord, int maxNClosestLinks) {
		return findNClosestLinks(networkImpl, coord, Double.MAX_VALUE, maxNClosestLinks, Double.MAX_VALUE);
	}

	/**
	 * Adds a node on the position of coord and connects it with two links to the neareast node of the network.
	 * @param coord where the new node should be created
	 * @param network that should be modified
	 * @param idPrefix the prefix for the new node and links
	 * @param idCounter is simply appended to the idPrefix and incremented
	 * @return a list with the two newly created links
	 */
	@Deprecated
	public static List<Link> addArtificialLinksToNetwork(Coord coord, Network network, String idPrefix, int idCounter) {
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

	public static List<Link> addArtificialLinksToNetwork(Coord coord, Network network, Set<String> networkModes, String idPrefix, int idCounter) {
		NetworkImpl networkImpl = (NetworkImpl) network;
		NetworkFactory networkFactory = network.getFactory();

		Node newNode = networkFactory.createNode(Id.create(idPrefix + "node_" + idCounter, Node.class), coord);
		Node nearestNode = networkImpl.getNearestNode(coord);
		Link newLink1 = networkFactory.createLink(Id.createLinkId(idPrefix + idCounter + ":1"), newNode, nearestNode);
		Link newLink2 = networkFactory.createLink(Id.createLinkId(idPrefix + idCounter + ":2"), nearestNode, newNode);

		newLink1.setAllowedModes(networkModes);
		newLink2.setAllowedModes(networkModes);

		network.addNode(newNode);
		network.addLink(newLink1);
		network.addLink(newLink2);

		List<Link> newLinks = new ArrayList<>();
		newLinks.add(newLink1);
		newLinks.add(newLink2);

		return newLinks;
	}

	/**
	 * @return the azimuth from two points in [rad]
	 */
	public static double getAzimuth(Coord from, Coord to) {
		double deltaE = to.getX()-from.getX();
		double deltaN = to.getY()-from.getY();

		double az2 = Math.atan2(deltaE, deltaN);

		if(az2 < 0)
			az2 = az2+2*Math.PI;

		if(az2 >= 2*Math.PI)
			az2 = az2-2*Math.PI;

		return az2;
	}

	/**
	 * @return whether Coord2 lies<br/>
	 * [1] North-East<br/>
	 * [2] South-East<br/>
	 * [3] South-West<br/>
	 * [4] North-West<br/>
	 * of Coord1
	 */
	public static int getCompassQuarter(Coord baseCoord, Coord toCoord) {
		double az = getAzimuth(baseCoord, toCoord);


		if(az < Math.PI/2) {
			return 1;
		} else if(az >= Math.PI/2 && az < Math.PI) {
			return 2;
		} else if(az > Math.PI && az < 1.5*Math.PI) {
			return 3;
		} else {
			return 4;
		}
	}

	/**
	 * @return Returns the point on the line between lineStart and lineEnd which
	 * is closest to refPoint.
	 */
	public static Coord getClosestPointOnLine(Coord lineStart, Coord lineEnd, Coord refPoint) {
		double azLine = getAzimuth(lineStart, lineEnd);
		double azPoint = getAzimuth(lineStart, refPoint);
		double azDiff = (azLine > azPoint ? azLine-azPoint : azPoint-azLine);

		double distanceToNewPoint = Math.cos(azDiff) * CoordUtils.calcEuclideanDistance(lineStart, refPoint);

		// assuming precision < 1 mm is not needed
		double newN = lineStart.getY() + Math.round(Math.cos(azLine) * distanceToNewPoint * 1000) / 1000.;
		double newE = lineStart.getX() + Math.round(Math.sin(azLine) * distanceToNewPoint * 1000) / 1000.;

		return new Coord(newE, newN);
	}

	public static Coord getClosestPointOnLine(Link link, Coord refPoint) {	return getClosestPointOnLine(link.getFromNode().getCoord(), link.getToNode().getCoord(), refPoint);}


	/**
	 * @return the opposite direction link
	 */
	public static Link getOppositeLink(Link link) {
		if (link == null) {
			return null;
		}

		Link oppositeDirectionLink = null;
		Map<Id<Link>, ? extends Link> inLinks = link.getFromNode().getInLinks();
		if(inLinks != null) {
			for (Link inLink : inLinks.values()) {
				if (inLink.getFromNode().equals(link.getToNode())) {
					oppositeDirectionLink = inLink;
				}
			}
		}

		return oppositeDirectionLink;
	}

	/**
	 * A debug method to assign weights to network links as number of lanes.
	 */
	public static void visualizeWeightsAsLanes(Network network, Map<Id<Link>, Double> weightMap) {
		for(Map.Entry<Id<Link>, Double> w : weightMap.entrySet()) {
			network.getLinks().get(w.getKey()).setNumberOfLanes(w.getValue());
		}
	}


	/**
	 * Adds a node on the splitPointCoordinates and splits the link into two new links
	 * @param network network the operations runs on, is modified.
	 * @param linkId of the link which should be split
	 * @param splitPointCoordinates of the point where the link should be separated. Practically the link is removed
	 *                              and two new links connecting the nodes are added. The splitPoint is moved onto the link.
	 * @param newObjectPrefix prefix for new the link and node (default: "split_")
	 */
	@Deprecated
	public static void splitLink(Network network, Id<Link> linkId, Coord splitPointCoordinates, String newObjectPrefix) {
		String prefix;

		if(newObjectPrefix == null) {
			prefix = "split_";
		} else {
			prefix = newObjectPrefix;
		}

		int intId = 0;

		Link link = network.getLinks().get(linkId);

		Coord coordinatesOnLink = getClosestPointOnLine(link, splitPointCoordinates);

		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(network);

		String newNodeIdString = link.getFromNode().getId().toString()+"_"+link.getToNode().getId().toString();
		String newLinkIdString = newNodeIdString+"_"+linkId.toString();

		while(network.getNodes().containsKey(Id.createNodeId(prefix+intId))) { intId++; }
		while(network.getLinks().containsKey(Id.createLinkId(prefix+intId))) { intId++; }

		Node newNode = networkFactory.createNode(Id.createNodeId(prefix+intId), coordinatesOnLink);
		Link newLink = networkFactory.createLink(Id.createLinkId(prefix+intId), newNode, link.getToNode());
		newLink.setLength(CoordUtils.calcEuclideanDistance(newNode.getCoord(), link.getToNode().getCoord()));
		newLink.setAllowedModes(link.getAllowedModes());
		newLink.setCapacity(link.getCapacity());
		newLink.setFreespeed(link.getFreespeed());
		newLink.setNumberOfLanes(link.getNumberOfLanes());

		network.addNode(newNode);
		network.addLink(newLink);

		link.setToNode(newNode);
		link.setLength(CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(), newNode.getCoord()));
	}

	/**
	 * Creates a deep copy of a network
	 * @param network
	 * @return
	 */
	public static Network copyNetwork(Network network) {
		return createFilteredNetwork(network, null, null);
	}

	/**
	 * Creates a new network (deep copy) which only contains link with transportMode
	 * @param network the base network that should be copied
	 * @param linkFilter to decide which links should be kept
	 * @return
	 */
	@Deprecated
	public static Network createFilteredNetwork(Network network, NetworkLinkFilter linkFilter, NetworkNodeFilter nodeFilter) {
		Network newNetwork = NetworkUtils.createNetwork();

		NetworkLinkFilter lf = (linkFilter == null ? new NetworkLinkFilterAllLinks() : linkFilter);
		NetworkNodeFilter nf = (nodeFilter == null ? new NetworkNodeFilterAllNodes() : nodeFilter);

		// Add node if it passes filter
		for (Node node : network.getNodes().values()) {
			if(nf.judgeNode(node)) {
				addNewNode(newNetwork, node);
			}
		}

		// Add link if it passes filter
		for(Link link : network.getLinks().values()) {
			if(lf.judgeLink(link)) {
				addNewLink(newNetwork, link);
			}
		}

		new NetworkCleaner().run(newNetwork);
		return newNetwork;
	}

	/**
	 * Creates a  network which only contains link with transportMode
	 * @param network the base network that should be copied
	 * @param linkFilter to decide which links should be kept
	 * @return
	 */
	@Deprecated
	public static Network getFilteredNetwork(Network network, NetworkLinkFilter linkFilter, NetworkNodeFilter nodeFilter) {
		Network newNetwork = NetworkUtils.createNetwork();

		NetworkLinkFilter lf = (linkFilter == null ? new NetworkLinkFilterAllLinks() : linkFilter);
		NetworkNodeFilter nf = (nodeFilter == null ? new NetworkNodeFilterAllNodes() : nodeFilter);

		// Add node if it passes filter
		for (Node node : network.getNodes().values()) {
			if(nf.judgeNode(node)) {
				addNewNode(newNetwork, node);
			}
		}
		for(Link link : network.getLinks().values()) {
			if(lf.judgeLink(link)) {
				newNetwork.addLink(link);
			}
		}

		return newNetwork;
	}

	@Deprecated
	public static Network addNewNode(Network network, Node node) {
		Id<Node> nodeId = Id.create(node.getId().toString(), Node.class);
		if (!network.getNodes().containsKey(nodeId)) {
			Node newNode = network.getFactory().createNode(nodeId, node.getCoord());
			network.addNode(newNode);
		}
		return network;
	}

	@Deprecated
	public static Network addNewLink(Network network, Link link) {
		Id<Link> linkId = Id.create(link.getId().toString(), Link.class);
		Id<Node> fromNodeId = Id.create(link.getFromNode().getId().toString(), Node.class);
		Id<Node> toNodeId = Id.create(link.getToNode().getId().toString(), Node.class);

		if(!network.getLinks().containsKey(linkId)) { // todo && network.getNodes().containsKey(fromNodeId) && network.getNodes().containsKey(toNodeId))
			Link newLink = network.getFactory().createLink(linkId, network.getNodes().get(fromNodeId), network.getNodes().get(toNodeId));
			newLink.setAllowedModes(link.getAllowedModes());
			newLink.setCapacity(link.getCapacity());
			newLink.setFreespeed(link.getFreespeed());
			newLink.setLength(link.getLength());
			newLink.setNumberOfLanes(link.getNumberOfLanes());
			network.addLink(newLink);
		}
		return network;
	}

	/**
	 * Calculates the extent of the given network.
	 * @param network
	 * @return Array of Coords with the minimal South-West and the maximal North-East Coordinates
	 */
	public static Coord[] getExtent(Network network) {
		double maxE = 0;
		double maxN = 0;
		double minS = Double.MAX_VALUE;
		double minW = Double.MAX_VALUE;

		for(Node node : network.getNodes().values()) {
			if(node.getCoord().getX() > maxE) {
				maxE = node.getCoord().getX();
			}
			if(node.getCoord().getY() > maxN) {
				maxN = node.getCoord().getY();
			}
			if(node.getCoord().getX() < minW) {
				minW = node.getCoord().getX();
			}
			if(node.getCoord().getY() < minS) {
				minS = node.getCoord().getY();
			}
		}

		return new Coord[]{new Coord(minW, minS), new Coord(maxE, maxN)};
	}
}
