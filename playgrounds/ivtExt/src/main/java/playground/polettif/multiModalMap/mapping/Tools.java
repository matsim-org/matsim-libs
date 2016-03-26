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


package playground.polettif.multiModalMap.mapping;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;

public class Tools {

	protected static Logger log = Logger.getLogger(Tools.class);

	// default params in [m]
	private static final double DEFAULT_SEARCH_RADIUS = 50;
	private static final double DEFAULT_RADIUS_INCREMENT = 50;
	private static final double DEFAULT_MAX_SEARCH_RADIUS = 500;
	private static final double DEFAULT_DISTANCE_THRESHOLD = 10;


	/**
	 * Adds a node on the splitPointCoordinates and splits the link into two new links
	 * @param network network the operations runs on
	 * @param linkId of the link which should be split
	 * @param splitPointCoordinates of the point where the link should be separated. Practically the link is removed
	 *                              and two new links connecting the nodes are added. The splitPoint is moved onto the link.
	 *
	 * @return edited network
	 */
	// TODO move to class LinkSplitter, static implementation gets problematic
	@Deprecated
	public static Network splitLink(Network network, Id<Link> linkId, Coord splitPointCoordinates) {

		Link link = network.getLinks().get(linkId);


		// get coordinates on the link
		Coord coordinatesOnLink = getClosestPointOnLine(link, splitPointCoordinates);

		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(network);

		// TODO generate different link name (current implementation gets veeeeery long
		String hash = ""; //":"+Integer.toString(coordinatesOnLink.hashCode());
		String newNodeIdString = link.getFromNode().getId().toString()+link.getToNode().getId().toString()+hash;
		String newLinkIdString1 = linkId.toString()+"_"+newNodeIdString;
		String newLinkIdString2 = newNodeIdString+"_"+linkId.toString();

		Id<Node> newNodeId = Id.createNodeId(newNodeIdString);
		Id<Link> newLinkId1 = Id.createLinkId(newLinkIdString1);
		Id<Link> newLinkId2 = Id.createLinkId(newLinkIdString2);

		Node newNode = networkFactory.createNode(newNodeId, coordinatesOnLink);

		network.addNode(newNode);
		network.addLink(networkFactory.createLink(newLinkId1, newNode, network.getLinks().get(linkId).getToNode()));
		network.addLink(networkFactory.createLink(newLinkId2, network.getLinks().get(linkId).getFromNode(), newNode));
		network.removeLink(linkId);

		return network;
	}

	/**
	 * Calculates bearing from two points
	 *
	 * @author polettif
	 */
	public static double getAzimuth(Coord from, Coord to) {
		// calculates azimuth/bearing of two nodes in radians

		double deltaE = to.getX()-from.getX();
		double deltaN = to.getY()-from.getY();
		double az = 0;

		double az2 = Math.atan2(deltaE, deltaN);

		/* done via atan2
		if(deltaE >= 0) {
			if(deltaN >= 0) {
				az = Math.atan(deltaE/deltaN);
			} else {
				az = Math.atan(deltaE / deltaN) + Math.PI;
			}
		} else {
			if (deltaN >= 0) {
				az = Math.atan(deltaE/deltaN) + 2*Math.PI;
			} else {
				az = Math.atan(deltaE/deltaN) + Math.PI;
			}
		}
		*/

		if(az2 < 0)
			az2 = az2+2*Math.PI;

		if(az2 >= 2*Math.PI)
			az2 = az2-2*Math.PI;

		return az2;
	}

	/**
	 *
	 * Maybe have a look at CoordUtils.distancePointLinesegment
	 *
	 * @param lineStart
	 * @param lineEnd
	 * @param refPoint
	 * @return
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
	 * Based on org.matsim.contrib.networkEditor.visualizing.NetBlackboard
	 *
	 * Gets the nearest Link to a Coordinate using brute force to find it
	 * @param coord
	 * @return The closest link
	 *
	 * getNearestLinkImproved() does not work since it uses networkImpl methods
	 */
	public static Link getNearestLinkBrute(Network network, Coord coord, double threshold){
		Link selected = null;
		double minDist = Double.MAX_VALUE;
		for(Link link : network.getLinks().values()){
			double thisDist = CoordUtils.distancePointLinesegment(link.getFromNode().getCoord(), link.getToNode().getCoord(), coord);
			if(thisDist < minDist) {
				minDist = thisDist;
				selected = link;
				if(thisDist < threshold)
					break;
			}
		}
		return selected;
	}

	public static Link getNearestLinkBrute(Network network, Coord coord){
		return getNearestLinkBrute(network, coord, 0.0);
	}

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
	 * Within search radius look for nodes and then search for the closest link.
	 *
	 * @param coord Coordinate to search the closest link.
	 * @return Null if no such link could be found.
	 */
	public static Link findClosestLink(Network network, Coord coord, double searchRadius) {
		Collection<Node> nearestNodes = ((NetworkImpl) network).getNearestNodes(coord, searchRadius);

		double minDist = Double.MAX_VALUE;
		Link selected = null;
		for(Node node : nearestNodes) {
			Map<Id<Link>, ? extends Link> outLinks = node.getOutLinks();
			Map<Id<Link>, ? extends Link> inLinks = node.getInLinks();
			double lineSegmentDistance = 0;

			for (Link linkCandidate : outLinks.values()) {
				lineSegmentDistance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), coord);
				if (lineSegmentDistance < minDist) {
					minDist = lineSegmentDistance;
					selected = linkCandidate;
				}
			}

			/*
			for (Link linkCandidate : inLinks.values()) {
				lineSegmentDistance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), coord);
				if (lineSegmentDistance < minDist) {
					minDist = lineSegmentDistance;
					selected = linkCandidate;
				}
			}
			*/
		}

		// get opposite link if stopFacility is on the left side of the link TODO change for countries driving on the left
/*
		if(selected != null) {
			if(Tools.getAzimuth(selected.getFromNode().getCoord(), coord) < Tools.getAzimuth(selected.getFromNode().getCoord(), selected.getToNode().getCoord())) {
				selected = Tools.getOppositeLink(selected);
			}
		}
*/
		return selected;
	}

	/**
	 * Looks for nodes within search radius of coord, then searches the closest links. Returns links wich are closer
	 * than maxLinkDistance (calculated via distancePointLineSegment() in {@link org.matsim.core.utils.geometry.CoordUtils}).
	 */
	public static List<Link> findClosestLinks(NetworkImpl networkImpl, Coord coord, double searchRadius, double maxLinkDistance) {
		Collection<Node> nearestNodes = networkImpl.getNearestNodes(coord, searchRadius);
		List<Link> closestLinks = new ArrayList<>();
		for (Node node : nearestNodes) {
			Map<Id<Link>, ? extends Link> outLinks = node.getOutLinks();
			double lineSegmentDistance;

			for (Link linkCandidate : outLinks.values()) {
				lineSegmentDistance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), coord);
				if (lineSegmentDistance < maxLinkDistance) {
					closestLinks.add(linkCandidate);
				}
			}
		}
		return closestLinks;
	}

	/**
	 * Looks for nodes within search radius of coord, then searches the closest links (calculated via distancePointLineSegment()
	 * in {@link org.matsim.core.utils.geometry.CoordUtils}).
	 *
	 * TODO Can return more than n links if links have the same distance from the facility (diff <1m).
	 *
	 * @return the closest n links to coord
	 */
	public static List<Link> findOnlyNClosestLinks(NetworkImpl networkImpl, Coord coord, double searchRadius, int n) {
		Collection<Node> nearestNodes = networkImpl.getNearestNodes(coord, searchRadius);
		SortedMap<Double, Link> closestLinksMap = new TreeMap<>();
		double incr = 0.1; double tol=2.0;



		if(nearestNodes.size() == 0) {
			return null;
		} else {
			for (Node node : nearestNodes) {
				Map<Id<Link>, ? extends Link> outLinks = node.getOutLinks();
				Map<Id<Link>, ? extends Link> inLinks = node.getInLinks();
				double lineSegmentDistance;

				for (Link linkCandidate : outLinks.values()) {
					lineSegmentDistance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), coord);

					// since distance is used as key, we need to ensure the exact distance is not used already TODO maybe check for side of the road?
					while(closestLinksMap.containsKey(lineSegmentDistance))
						lineSegmentDistance+=incr;

					closestLinksMap.put(lineSegmentDistance, linkCandidate);
				}
				for (Link linkCandidate : inLinks.values()) {
					lineSegmentDistance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), coord);
					while(closestLinksMap.containsKey(lineSegmentDistance)) {
						lineSegmentDistance+=incr;
					}
					closestLinksMap.put(lineSegmentDistance, linkCandidate);
				}
			}

			List<Link> closestLinks = new ArrayList<>();

			int i = 1; double d=0;
			for(Map.Entry<Double, Link> e : closestLinksMap.entrySet()) { //int i=0; i < n && i < closestLinksArray.length; i++) {
				if(i > n && (e.getKey()-d > tol))
					break;
				closestLinks.add(e.getValue());
				i++;
			}

			return closestLinks;
		}
	}



	/**
	 * Finds the nearest link to a node.
	 * Looks for all nodes within a search Radius from the  and calculates distancePointLineSegment for all links for these nodes.
	 * If the distance is < the
	 *
	 * Aborts searching after a maxSearchRadius (default 500m)
	 *
	 * TODO nodes and stopfacilities
	 * TODO minimal threshold, abort loops if distance < threshold
	 */
/*
	public static Id<Link> findNearestLink1(Network network, Coord point, double maxSearchRadius) throws Exception {
		return Id.createLinkId("");

		// NetworkUtils.getSortedNodes
		Map<Id<Node>, ? extends Node> allNodes = network.getNodes();
		Map<Double, Node> distanceNodes = new HashMap<>();
		Map<Id<Link>, Link> links = new HashMap<>();

		double shortestDistance = Double.MAX_VALUE;

		// get all nodes with distance from point < maxSearch
		for (Node n : allNodes.values()) {
			double distance = NetworkUtils.getEuclideanDistance(n.getCoord(), point);
			if (distance < maxSearchRadius) {
				n.getOutLinks();
				n.getInLinks();

		}
		// sort nodes by distance (proably not needed)
		Map<Double, Node> sortedNodesByDistance = new TreeMap<>(distanceNodes);

		Link closestLink = null;

		for(Node node : sortedNodesByDistance.values()) {
			// look at all links from or to nodeCandidates and calculate shortest distance from point to the link

			Map<Id<Link>, ? extends Link> outLinks = node.getOutLinks();
			Map<Id<Link>, ? extends Link> inLinks = node.getInLinks();
			double lineSegmentDistance = 0;

			for (Link linkCandidate : outLinks.values()) {
				lineSegmentDistance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), point);
				if (lineSegmentDistance < shortestDistance) {
					shortestDistance = distance;
					closestLink = linkCandidate;
				}
			}

			for (Link linkCandidate : inLinks.values()) {
				lineSegmentDistance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), point);
				if (lineSegmentDistance < shortestDistance) {
					shortestDistance = distance;
					closestLink = linkCandidate;
				}
			}
		}

		if(closestLink != null) {
			return closestLink.getId();
		} else {
			throw new Exception("No links found!");
		}
	} */
}
