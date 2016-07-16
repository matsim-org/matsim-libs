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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Builds a network where transit vehicles can drive along and assigns the correct
 * links to the transit stop facilities and routes of transit lines. As each transit
 * stop facility can only be connected to at most one link, the algorithm is forced
 * to duplicated transit stop facilities in certain cases to build the network.
 *
 * @author mrieser
 */
public class CreatePseudoNetwork {

	private final TransitSchedule schedule;
	private final Network network;
	private final String prefix;

	private final Map<Tuple<Node, Node>, Link> links = new HashMap<Tuple<Node, Node>, Link>();
	private final Map<Tuple<Node, Node>, TransitStopFacility> stopFacilities = new HashMap<Tuple<Node, Node>, TransitStopFacility>();
	private final Map<TransitStopFacility, Node> nodes = new HashMap<TransitStopFacility, Node>();
	private final Map<TransitStopFacility, List<TransitStopFacility>> facilityCopies = new HashMap<TransitStopFacility, List<TransitStopFacility>>();

	private long linkIdCounter = 0;

	private final Set<String> transitModes = Collections.singleton(TransportMode.pt);

	public CreatePseudoNetwork(final TransitSchedule schedule, final Network network, final String networkIdPrefix) {
		this.schedule = schedule;
		this.network = network;
		this.prefix = networkIdPrefix;
	}

	public void createNetwork() {

		List<Tuple<TransitLine, TransitRoute>> toBeRemoved = new LinkedList<Tuple<TransitLine, TransitRoute>>();

		for (TransitLine tLine : this.schedule.getTransitLines().values()) {
			for (TransitRoute tRoute : tLine.getRoutes().values()) {
				ArrayList<Id<Link>> routeLinks = new ArrayList<Id<Link>>();
				TransitRouteStop prevStop = null;
				for (TransitRouteStop stop : tRoute.getStops()) {
					Link link = getNetworkLink(prevStop, stop);
					routeLinks.add(link.getId());
					prevStop = stop;
				}

				if (routeLinks.size() > 0) {
					NetworkRoute route = RouteUtils.createNetworkRoute(routeLinks, this.network);
					tRoute.setRoute(route);
				} else {
					System.err.println("Line " + tLine.getId() + " route " + tRoute.getId() + " has less than two stops. Removing this route from schedule.");
					toBeRemoved.add(new Tuple<TransitLine, TransitRoute>(tLine, tRoute));
				}
			}
		}

		for (Tuple<TransitLine, TransitRoute> remove : toBeRemoved) {
			remove.getFirst().removeRoute(remove.getSecond());
		}
	}

	private Link getNetworkLink(final TransitRouteStop fromStop, final TransitRouteStop toStop) {
		TransitStopFacility fromFacility = (fromStop == null) ? toStop.getStopFacility() : fromStop.getStopFacility();
		TransitStopFacility toFacility = toStop.getStopFacility();

		Node fromNode = this.nodes.get(fromFacility);
		if (fromNode == null) {
			fromNode = this.network.getFactory().createNode(Id.create(this.prefix + toFacility.getId(), Node.class), fromFacility.getCoord());
			this.network.addNode(fromNode);
			this.nodes.put(toFacility, fromNode);
		}

		Node toNode = this.nodes.get(toFacility);
		if (toNode == null) {
			toNode = this.network.getFactory().createNode(Id.create(this.prefix + toFacility.getId(), Node.class), toFacility.getCoord());
			this.network.addNode(toNode);
			this.nodes.put(toFacility, toNode);
		}

		Tuple<Node, Node> connection = new Tuple<Node, Node>(fromNode, toNode);
		Link link = this.links.get(connection);
		if (link == null) {
			link = createAndAddLink(fromNode, toNode, connection);

			if (toFacility.getLinkId() == null) {
				toFacility.setLinkId(link.getId());
				this.stopFacilities.put(connection, toFacility);
			} else {
				List<TransitStopFacility> copies = this.facilityCopies.get(toFacility);
				if (copies == null) {
					copies = new ArrayList<TransitStopFacility>();
					this.facilityCopies.put(toFacility, copies);
				}
				Id<TransitStopFacility> newId = Id.create(toFacility.getId().toString() + "." + Integer.toString(copies.size() + 1), TransitStopFacility.class);
				TransitStopFacility newFacility = this.schedule.getFactory().createTransitStopFacility(newId, toFacility.getCoord(), toFacility.getIsBlockingLane());
				newFacility.setStopPostAreaId(toFacility.getId().toString());
				newFacility.setLinkId(link.getId());
				newFacility.setName(toFacility.getName());
				copies.add(newFacility);
				this.nodes.put(newFacility, toNode);
				this.schedule.addStopFacility(newFacility);
				toStop.setStopFacility(newFacility);
				this.stopFacilities.put(connection, newFacility);
			}
		} else {
			toStop.setStopFacility(this.stopFacilities.get(connection));
		}
		return link;
	}

	private Link createAndAddLink(Node fromNode, Node toNode,
			Tuple<Node, Node> connection) {
		Link link;
		link = this.network.getFactory().createLink(Id.create(this.prefix + this.linkIdCounter++, Link.class), fromNode, toNode);
		if (fromNode == toNode) {
			link.setLength(50);
		} else {
			link.setLength(CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()));
		}
		link.setFreespeed(30.0 / 3.6);
		link.setCapacity(500);
		link.setNumberOfLanes(1);
		this.network.addLink(link);
		link.setAllowedModes(this.transitModes);
		this.links.put(connection, link);
		return link;
	}

	public Link getLinkBetweenStops(final TransitStopFacility fromStop, final TransitStopFacility toStop) {
		Node fromNode = this.nodes.get(fromStop);
		Node toNode = this.nodes.get(toStop);
		Tuple<Node, Node> connection = new Tuple<Node, Node>(fromNode, toNode);
		return this.links.get(connection);
	}

}
