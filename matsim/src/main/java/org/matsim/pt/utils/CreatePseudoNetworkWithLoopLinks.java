/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePseudoNetwork
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

package org.matsim.pt.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;

import java.util.*;

/**
 * Builds a network where transit vehicles can drive along and assigns the correct
 * links to the transit stop facilities and routes of transit lines. Each stop facility
 * is assigned to a loop link, located in a node with the same coordinates as the stop.
 * The stop facility ID is used for node and link IDs.
 *
 * @author mrieser, davibicudo, rakow
 */
public class CreatePseudoNetworkWithLoopLinks {

	private final TransitSchedule schedule;
	private final Network network;
	private final String prefix;
	private final double linkFreeSpeed;
	private final double linkCapacity;

	private final Map<Tuple<Node, Node>, Link> links = new HashMap<>();
	private final Map<TransitStopFacility, Node> nodes = new HashMap<>();

	private final Set<String> transitModes = Collections.singleton(TransportMode.pt);

	public CreatePseudoNetworkWithLoopLinks(final TransitSchedule schedule, final Network network, final String networkIdPrefix) {
		this.schedule = schedule;
		this.network = network;
		this.prefix = networkIdPrefix;
		this.linkFreeSpeed = 100.0 / 3.6;
		this.linkCapacity = 100000.0;
	}

	public CreatePseudoNetworkWithLoopLinks(final TransitSchedule schedule, final Network network, final String networkIdPrefix,
                                            final double linkFreeSpeed, final double linkCapacity) {
		this.schedule = schedule;
		this.network = network;
		this.prefix = networkIdPrefix;
		this.linkFreeSpeed = linkFreeSpeed;
		this.linkCapacity = linkCapacity;
	}

	public void createNetwork() {

		createStopNodesAndLoopLinks();

		List<Tuple<TransitLine, TransitRoute>> toBeRemoved = new LinkedList<>();
		for (TransitLine tLine : this.schedule.getTransitLines().values()) {
			for (TransitRoute tRoute : tLine.getRoutes().values()) {

				if (tRoute.getStops().size() < 2) {
					System.err.println("Line " + tLine.getId() + " route " + tRoute.getId() + " has less than two stops. Removing this route from schedule.");
					toBeRemoved.add(new Tuple<>(tLine, tRoute));
					continue;
				}

				List<Id<Link>> routeLinks = new ArrayList<>();
				TransitRouteStop prevStop = null;

				for (TransitRouteStop stop : tRoute.getStops()) {
					if (prevStop != null) {
						Link link = getNetworkLink(prevStop, stop);
						routeLinks.add(link.getId());
					}

					// Add the loop links of all stops to the route
					routeLinks.add(getLoopLink(stop.getStopFacility()).getId());
					prevStop = stop;
				}

				NetworkRoute route = RouteUtils.createNetworkRoute(routeLinks);
				tRoute.setRoute(route);
			}
		}

		for (Tuple<TransitLine, TransitRoute> remove : toBeRemoved) {
			remove.getFirst().removeRoute(remove.getSecond());
		}
	}

	private void createStopNodesAndLoopLinks() {
		for (TransitStopFacility stop : this.schedule.getFacilities().values()) {
			Node node = this.network.getFactory().createNode(Id.createNodeId(this.prefix + stop.getId()), stop.getCoord());
			this.network.addNode(node);
			this.nodes.put(stop, node);

			Link loopLink = this.network.getFactory().createLink(Id.createLinkId (this.prefix + stop.getId()), node, node);
			// Loop links needs to have a length so that the travel time is not zero
			loopLink.setLength(1);
			loopLink.setFreespeed(linkFreeSpeed);
			loopLink.setCapacity(linkCapacity);
			// Ensure enough vehicles can be placed on the loop link
			loopLink.setNumberOfLanes(linkCapacity);
			loopLink.setAllowedModes(transitModes);

			stop.setLinkId(loopLink.getId());
			this.network.addLink(loopLink);
			Tuple<Node, Node> connection = new Tuple<>(node, node);
			this.links.put(connection, loopLink);
		}
	}

	/**
	 * Get the loop link for a stop facility.
	 */
	private Link getLoopLink(final TransitStopFacility stop) {
		Node node = this.nodes.get(stop);
		Tuple<Node, Node> connection = new Tuple<>(node, node);
		return this.links.get(connection);
	}

	private Link getNetworkLink(final TransitRouteStop fromStop, final TransitRouteStop toStop) {
		TransitStopFacility fromFacility = fromStop.getStopFacility();
		TransitStopFacility toFacility = toStop.getStopFacility();

		Node fromNode = this.nodes.get(fromFacility);
		Node toNode = this.nodes.get(toFacility);

		Tuple<Node, Node> connection = new Tuple<>(fromNode, toNode);
		Link link = this.links.get(connection);
		return link == null ? createAndAddLink(connection) : link;
	}

	private Link createAndAddLink(Tuple<Node, Node> connection) {
		Node fromNode = connection.getFirst();
		Node toNode = connection.getSecond();
		Link link;
		link = this.network.getFactory().createLink(Id.createLinkId(fromNode.getId() + "-" + toNode.getId()),
			fromNode, toNode);

		double dist = CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord());
		link.setLength(dist);
		link.setFreespeed(linkFreeSpeed);
		link.setCapacity(linkCapacity);
		link.setNumberOfLanes(1);

		this.network.addLink(link);
		link.setAllowedModes(this.transitModes);
		this.links.put(connection, link);
		return link;
	}

}
