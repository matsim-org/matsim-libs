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
import org.matsim.core.utils.geometry.CoordUtils;

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

	/**
	 * Calculates azimuth from two points
	 */
	public static double getAzimuth(Coord from, Coord to) {
		// calculates azimuth/bearing of two nodes in radians

		double deltaE = to.getX()-from.getX();
		double deltaN = to.getY()-from.getY();
		double az = 0;

		double az2 = Math.atan2(deltaE, deltaN);

		if(az2 < 0)
			az2 = az2+2*Math.PI;

		if(az2 >= 2*Math.PI)
			az2 = az2-2*Math.PI;

		return az2;
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
	public static Coord getClosestPointOnLine(Node from, Node to, Node refPoint) {	return getClosestPointOnLine(from.getCoord(), to.getCoord(), refPoint.getCoord());}
	public static Coord getClosestPointOnLine(Node from, Node to, Coord refPoint) {	return getClosestPointOnLine(from.getCoord(), to.getCoord(), refPoint);}


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

}
