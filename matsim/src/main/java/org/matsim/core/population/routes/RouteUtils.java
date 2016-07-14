/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.population.routes;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Provides helper methods to work with routes.
 *
 * @author mrieser
 */
public class RouteUtils {

	/**
	 * Returns all nodes the route passes between the start- and the end-link of the route.
	 *
	 * @param route
	 * @param network
	 * @return
	 */
	public static List<Node> getNodes(final NetworkRoute route, final Network network) {
		List<Id<Link>> linkIds = route.getLinkIds();
		List<Node> nodes = new ArrayList<Node>(linkIds.size() + 1);
		if ((linkIds.size() > 0)) {
			nodes.add(network.getLinks().get(linkIds.get(0)).getFromNode());
			for (Id<Link> linkId : linkIds) {
				Link link = network.getLinks().get(linkId);
				nodes.add(link.getToNode());
			}
		} else if (!route.getStartLinkId().equals(route.getEndLinkId())) {
			nodes.add(network.getLinks().get(route.getStartLinkId()).getToNode());
		}
		return nodes;
	}

	public static List<Link> getLinksFromNodes(final List<Node> nodes) {
		ArrayList<Link> links = new ArrayList<Link>(nodes.size());
		Node prevNode = null;
		for (Node node : nodes) {
			if (prevNode != null) {
				Link foundLink = findLink(prevNode, node);
				if (foundLink != null) {
					links.add(foundLink);
				}
			}
			prevNode = node;
		}
		links.trimToSize();
		return links;
	}

	public static List<Link> getLinksFromNodeIds(final Network network, final List<Id<Node>> nodeIds) {
		ArrayList<Link> links = new ArrayList<Link>(nodeIds.size());
		Node prevNode = null;
		for (Id<Node> nodeId : nodeIds) {
			Node node = network.getNodes().get(nodeId);
			if (prevNode != null) {
				Link foundLink = findLink(prevNode, node);
				if (foundLink != null) {
					links.add(foundLink);
				}
			}
			prevNode = node;
		}
		links.trimToSize();
		return links;
	}

	private static Link findLink(Node prevNode, Node node) {
		for (Link link : prevNode.getOutLinks().values()) {
			if (link.getToNode().equals(node)) {
				return link;
			}
		}
		return null;
	}

	public static NetworkRoute getSubRoute(final NetworkRoute route, final Node fromNode, final Node toNode, final Network network) {
		Id<Link> fromLinkId = null;
		Id<Link> toLinkId = null;

		List<Id<Link>> linkIds = new ArrayList<>(route.getLinkIds().size() + 2);
		linkIds.add(route.getStartLinkId());
		linkIds.addAll(route.getLinkIds());
		linkIds.add(route.getEndLinkId());
		for (Id<Link> linkId : linkIds) {
			Link link = network.getLinks().get(linkId);
			if (link.getToNode() == fromNode) {
				fromLinkId = link.getId();
			}
			if (link.getFromNode() == toNode) {
				toLinkId = link.getId();
				break; // we found the toLinkId, so we can stop searching, fromLinkId should be before after all
			}
		}

		return route.getSubRoute(fromLinkId, toLinkId);
	}


	/**
	 * Calculates the distance of the complete route, <b>excluding</b> the distance traveled
	 * on the start- and end-link of the route.
	 *
	 * @param route
	 * @param network
	 * @return
	 */
	public static double calcDistanceExcludingStartEndLink(final NetworkRoute route, final Network network) {
		double dist = 0;
		for (Id<Link> linkId : route.getLinkIds()) {
			dist += network.getLinks().get(linkId).getLength();
		}
		return dist;
	}
	

	/**
	 * Calculates the distance of the complete route, <b>including</b> the distance traveled
	 * on the start- and end-link of the route.
	 * 
	 * @param networkRoute
	 * @param relPosOnDepartureLink relative position on the departure link where vehicles start traveling
	 * @param relPosOnArrivalLink relative position on the arrival link where vehicles stop traveling
	 * @param network
	 * @return
	 */
	public static double calcDistance(final NetworkRoute networkRoute, final double relPosOnDepartureLink, final double relPosOnArrivalLink, final Network network) {
		// sum distance of all link besides departure and arrival link
		double routeDistance = calcDistanceExcludingStartEndLink(networkRoute, network);
		// add relative distance of departure link
		routeDistance += network.getLinks().get(networkRoute.getStartLinkId()).getLength() * (1.0 - relPosOnDepartureLink);
		if (!networkRoute.getStartLinkId().equals(networkRoute.getEndLinkId())){
			// add relative distance of arrival link
			routeDistance += network.getLinks().get(networkRoute.getEndLinkId()).getLength() * relPosOnArrivalLink;
		} else { // i.e. departure = arrival link
			// subtract relative distance of arrival link that is not traveled
			routeDistance -= network.getLinks().get(networkRoute.getEndLinkId()).getLength() * (1.0 - relPosOnArrivalLink);
		}
		return routeDistance;
	}

	public static NetworkRoute createNetworkRoute(List<Id<Link>> routeLinkIds, final Network network) {
		Id<Link> startLinkId = routeLinkIds.get(0);
		List<Id<Link>> linksBetween = (routeLinkIds.size() > 2) ? routeLinkIds.subList(1, routeLinkIds.size() - 1) : new ArrayList<Id<Link>>(0);
		Id<Link> endLinkId = routeLinkIds.get(routeLinkIds.size() - 1);
		LinkNetworkRouteImpl route = new LinkNetworkRouteImpl(startLinkId, endLinkId);
		route.setLinkIds(startLinkId, linksBetween, endLinkId);
		return route;
	}

	public static double calcDistance(ExperimentalTransitRoute route, TransitSchedule ts, Network network) {
		
		Id<TransitLine> lineId = route.getLineId();
		Id<TransitRoute> routeId = route.getRouteId();
		Id<TransitStopFacility> enterStopId = route.getAccessStopId();
		Id<TransitStopFacility> exitStopId = route.getEgressStopId();
	
		TransitLine line = ts.getTransitLines().get(lineId);
		TransitRoute tr = line.getRoutes().get(routeId);
	
		Id<Link> enterLinkId = ts.getFacilities().get(enterStopId).getLinkId();
		Id<Link> exitLinkId = ts.getFacilities().get(exitStopId).getLinkId();
	
		NetworkRoute nr = tr.getRoute();
		double dist = 0;
		boolean count = false;
		if (enterLinkId.equals(nr.getStartLinkId())) {
			count = true;
		}
		for (Id<Link> linkId : nr.getLinkIds()) {
			if (count) {
				Link l = network.getLinks().get(linkId);
				dist += l.getLength();
			}
			if (enterLinkId.equals(linkId)) {
				count = true;
			}
			if (exitLinkId.equals(linkId)) {
				count = false;
				break;
			}
		}
		if (count) {
			Link l = network.getLinks().get(nr.getEndLinkId());
			dist += l.getLength();
		}
		return dist;
	}

	/**
	 * How much of route is "covered" by the links of route2.  Based on Ramming.  Note that this is not symmetric,
	 * i.e. route1 can be fully covered by route2, but not the other way around.  kai, nov'13
	 * 
	 * @param route1
	 * @param route2
	 * @return a number between 0 (no coverage) and 1 (route2 fully covers route1)
	 */
	public static double calculateCoverage(NetworkRoute route1, NetworkRoute route2, Network network ) {
		double routeLength = 0. ;
		double coveredLength = 0. ;
		for ( Id<Link> id : route1.getLinkIds() ) {
			routeLength += network.getLinks().get( id ).getLength() ;
			if ( route2.getLinkIds().contains(id) ) {
				coveredLength += network.getLinks().get( id ).getLength() ;
			}
		}
		if ( routeLength > 0. ) {
			return coveredLength/routeLength ;
		} else {
			return 1. ; // route has zero length = fully covered by any other route.  (but they are not similar!?!?!?)
		}
	}

}
