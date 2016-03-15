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
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;

/**
 * Created by polettif on 11.03.2016.
 */
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
	public static Network splitLink(Network network, Id<Link> linkId, Coord splitPointCoordinates) {

		Link link = network.getLinks().get(linkId);

		// get coordinates on the link
		Coord coordinatesOnLink = getClosestPointOnLine(link, splitPointCoordinates);

		NetworkFactoryImpl networkFactory = new NetworkFactoryImpl(network);

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

	public static Network splitLink(Network network, String linkId, Node splitPoint) {
		return splitLink(network, Id.createLinkId(linkId), splitPoint.getCoord());
	}

	public static Network splitLink(Network network, String linkId, Coord splitPointCoordinates) {
		return splitLink(network, Id.createLinkId(linkId), splitPointCoordinates);
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
	 * Finds the nearest link to a node.
	 * Looks for all nodes within a search Radius from the  and calculates distancePointLineSegment for all links for these nodes.
	 * If the distance is < the
	 *
	 * Aborts searching after a maxSearchRadius (default 500m)  Id<Link>
	 *
	 * TODO nodes and stopfacilities
	 * TODO minimal threshold, abort loops if distance < threshold
	 */
	public static void findNearestLink(Network network, Coord point, double maxSearchRadius) {
		/*
		// NetworkUtils.getSortedNodes
		Map<Id<Node>, ? extends Node> allNodes = network.getNodes();
		Map<Double, Node> distanceNodes = new HashMap<>();

		double shortestDistance = Double.MAX_VALUE;

		// get all nodes with distance from point < maxSearch
		for (Node n : allNodes.values()) {
			double distance = NetworkUtils.getEuclideanDistance(n.getCoord(), point);
			if (distance < maxSearchRadius) {
				distanceNodes.put(distance, n);
			}
		}
		// sort nodes by distance (proably not needed)
		Map<Double, Node> sortedNodesByDistance = new TreeMap<>(distanceNodes);

		Link closestLink = null;

		for(Node node : sortedNodesByDistance.values()) {
			// look at all links from or to nodeCandidates and calculate shortest distance from point to the link

			Map<Id<Link>, ? extends Link> outLinks = node.getOutLinks();
			Map<Id<Link>, ? extends Link> inLinks = node.getInLinks();

			for (Link linkCandidate : outLinks.values()) {
				double distance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), point);
				if (distance < shortestDistance) {
					shortestDistance = distance;
					closestLink = linkCandidate;
				}
			}

			for (Link linkCandidate : inLinks.values()) {
				double distance = CoordUtils.distancePointLinesegment(linkCandidate.getFromNode().getCoord(), linkCandidate.getToNode().getCoord(), point);
				if (distance < shortestDistance) {
					shortestDistance = distance;
					closestLink = linkCandidate;
				}
			}
		}

		if(closestLink != null) {
			return closestLink.getId();
		} else {

		}*/

	}
/*
	public static Id<Link> findNearestLink(Network network, Coord point) {
		double maxSearchRadius = DEFAULT_MAX_SEARCH_RADIUS;
		double radiusIncrement = DEFAULT_RADIUS_INCREMENT;
		double distanceThreshold = DEFAULT_DISTANCE_THRESHOLD;
		return findNearestLink(network, point, maxSearchRadius);
	}
*/
	/**
	 * alternate method via double distanceThreshold
	 */
//	public static Id<Link> findNearestLink(Network network, TransitStopFacility stopFacility)

}
