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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

/**
 * Provides helper methods to work with routes.
 *
 * @author mrieser
 */
public class RouteUtils {
	private static final Logger log = LogManager.getLogger( RouteUtils.class ) ;
	
	private RouteUtils(){} // do not instantiate

	/**
	 * Returns all nodes the route passes between the start- and the end-link of the route.
	 *
	 * @param route
	 * @param network
	 * @return
	 */
	public static List<Node> getNodes(final NetworkRoute route, final Network network) {
		List<Id<Link>> linkIds = route.getLinkIds();
		List<Node> nodes = new ArrayList<>(linkIds.size() + 1);
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
		ArrayList<Link> links = new ArrayList<>(nodes.size());
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
		ArrayList<Link> links = new ArrayList<>(nodeIds.size());
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
	 * Calculates the distance of the route, <b>excluding</b> the distance traveled
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

	public static double calcTravelTimeExcludingStartEndLink( final NetworkRoute networkRoute, double now, Person person, Vehicle vehicle, final
	Network network, TravelTime travelTime ) {
		double newTravelTime = 0.0;
		for (Id<Link> routeLinkId : networkRoute.getLinkIds()) newTravelTime += travelTime.getLinkTravelTime(network.getLinks().get(routeLinkId),
					now + newTravelTime, person, vehicle);
		return newTravelTime;
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

	public static double calcTravelTime( final NetworkRoute networkRoute, final double relPosOnDepartureLink, final double relPosOnArrivalLink,
					     double now, Person person, Vehicle vehicle, final Network network, TravelTime travelTime) {

		if (!networkRoute.getStartLinkId().equals(networkRoute.getEndLinkId())){
			return 0.;
		}
		double startTime = now;

		// add relative distance of departure link
		now += (1.0 - relPosOnDepartureLink) * travelTime.getLinkTravelTime( network.getLinks().get( networkRoute.getStartLinkId() ), now, person, vehicle );

		// sum distance of all link besides departure and arrival link
		 now += calcTravelTimeExcludingStartEndLink( networkRoute, now, person, vehicle, network, travelTime);

		// add time on arrival link
		now += relPosOnArrivalLink * travelTime.getLinkTravelTime( network.getLinks().get( networkRoute.getEndLinkId() ), now, person, vehicle );

		return now - startTime;
	}

	@Deprecated // rename to calcDistanceExcludingStartEndLink.  kai, feb'25
	public static double calcDistance( final LeastCostPathCalculator.Path path ) {
		double length = 0. ;
		for ( Link link : path.links ) {
			length += link.getLength() ;
		}
		return length ;
	}
	@Deprecated // network argument is not needed; please inline.  kai, sep'20
	public static NetworkRoute createNetworkRoute( List<Id<Link>> routeLinkIds, Network network ) {
		return createNetworkRoute( routeLinkIds );
	}
	public static NetworkRoute createNetworkRoute( List<Id<Link>> routeLinkIds ) {
		Id<Link> startLinkId = routeLinkIds.get(0);
		List<Id<Link>> linksBetween = (routeLinkIds.size() > 2) ? routeLinkIds.subList(1, routeLinkIds.size() - 1) : new ArrayList<>(0);
		Id<Link> endLinkId = routeLinkIds.get(routeLinkIds.size() - 1);
		NetworkRoute route = createLinkNetworkRouteImpl(startLinkId, endLinkId);
		route.setLinkIds(startLinkId, linksBetween, endLinkId);
		return route;
	}
	
	public static double calcDistance(TransitPassengerRoute route, TransitSchedule ts, Network network) {
		Id<TransitLine> lineId = route.getLineId();
		Id<TransitRoute> routeId = route.getRouteId();
		Id<TransitStopFacility> enterStopId = route.getAccessStopId();
		Id<TransitStopFacility> exitStopId = route.getEgressStopId();
	
		TransitLine line = ts.getTransitLines().get(lineId);
		TransitRoute tr = line.getRoutes().get(routeId);
		
		TransitStopFacility accessFacility = ts.getFacilities().get(enterStopId);
		TransitStopFacility egressFacility = ts.getFacilities().get(exitStopId);
		
		return calcDistance(tr, accessFacility, egressFacility, network);
	}
	
	public static double calcDistance(TransitRoute tr, TransitStopFacility accessFacility, TransitStopFacility egressFacility, Network network) {
		Id<Link> enterLinkId = accessFacility.getLinkId();
		Id<Link> exitLinkId = egressFacility.getLinkId();
	
		NetworkRoute nr = tr.getRoute();
		double dist = 0;
		boolean count = false;
		if (enterLinkId.equals(nr.getStartLinkId())) {
			count = true;
		}
		for (Id<Link> linkId : nr.getLinkIds()) {
			if (count) {
				Link l = network.getLinks().get(linkId);
				if ( l==null ) {
					log.error( "link is null; linkId=" + linkId + "; network=" + network ) ;
				}
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
		Gbl.assertNotNull( route1 );
		Gbl.assertNotNull( route2 );
		Gbl.assertNotNull( network );
		
		double routeLength = 0. ;
		double coveredLength = 0. ;
		for ( Id<Link> id : route1.getLinkIds() ) {
			final Link link = network.getLinks().get( id );
			Gbl.assertNotNull( link );
			routeLength += link.getLength() ;
			if ( route2.getLinkIds().contains(id) ) {
				coveredLength += link.getLength() ;
			}
		}
		if ( routeLength > 0. ) {
			return coveredLength/routeLength ;
		} else {
			return 1. ; // route has zero length = fully covered by any other route.  (but they are not similar!?!?!?)
		}
	}

	public static Route createGenericRouteImpl(Id<Link> startLinkId, Id<Link> endLinkId) {
		return new GenericRouteImpl(startLinkId, endLinkId);
	}

	public static NetworkRoute createLinkNetworkRouteImpl(Id<Link> startLinkId, Id<Link> endLinkId) {
		return new LinkNetworkRouteImpl(startLinkId, endLinkId);
	}

	public static NetworkRoute createLinkNetworkRouteImpl(Id<Link> startLinkId, List<Id<Link>> linkIds,
			Id<Link> endLinkId) {
		return new LinkNetworkRouteImpl(startLinkId, linkIds, endLinkId);
	}

	public static NetworkRoute createLinkNetworkRouteImpl(Id<Link> startLinkId, Id<Link>[] linkIds,
			Id<Link> endLinkId) {
		return new LinkNetworkRouteImpl(startLinkId, linkIds, endLinkId);
	}

}
