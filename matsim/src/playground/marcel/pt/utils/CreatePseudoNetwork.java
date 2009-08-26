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

package playground.marcel.pt.utils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.api.TransitLine;
import org.matsim.transitSchedule.api.TransitRoute;
import org.matsim.transitSchedule.api.TransitRouteStop;
import org.matsim.transitSchedule.api.TransitSchedule;
import org.matsim.transitSchedule.api.TransitStopFacility;

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
	private final NetworkLayer network;
	private final String prefix;

	private final Map<Tuple<NodeImpl, NodeImpl>, LinkImpl> links = new HashMap<Tuple<NodeImpl, NodeImpl>, LinkImpl>();
	private final Map<TransitStopFacility, NodeImpl> nodes = new HashMap<TransitStopFacility, NodeImpl>();
	private final Map<TransitStopFacility, NodeImpl> startNodes = new HashMap<TransitStopFacility, NodeImpl>();
	private final Map<TransitStopFacility, List<TransitStopFacility>> facilityCopies = new HashMap<TransitStopFacility, List<TransitStopFacility>>();

	private long linkIdCounter = 0;
	private long nodeIdCounter = 0;

	private Set<TransportMode> transitModes = EnumSet.of(TransportMode.pt);
	
	public CreatePseudoNetwork(final TransitSchedule schedule, final NetworkLayer network, final String networkIdPrefix) {
		this.schedule = schedule;
		this.network = network;
		this.prefix = networkIdPrefix;
	}

	public void createNetwork() {

		List<Tuple<TransitLine, TransitRoute>> toBeRemoved = new LinkedList<Tuple<TransitLine, TransitRoute>>();

		for (TransitLine tLine : this.schedule.getTransitLines().values()) {
			for (TransitRoute tRoute : tLine.getRoutes().values()) {
				ArrayList<Link> routeLinks = new ArrayList<Link>();
				TransitRouteStop prevStop = null;
				for (TransitRouteStop stop : tRoute.getStops()) {
					Link link = getNetworkLink(prevStop, stop);
					routeLinks.add(link);
					prevStop = stop;
				}

				if (routeLinks.size() > 0) {
					Link startLink = routeLinks.get(0);
					List<Link> linksBetween = (routeLinks.size() > 2) ? routeLinks.subList(1, routeLinks.size() - 1) : new ArrayList<Link>(0);
					Link endLink = routeLinks.get(routeLinks.size() - 1);
					NetworkRouteWRefs route = new LinkNetworkRouteImpl(startLink, endLink);
					route.setLinks(startLink, linksBetween, endLink);
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
		TransitStopFacility fromFacility = (fromStop == null) ? null : fromStop.getStopFacility();
		TransitStopFacility toFacility = toStop.getStopFacility();

		NodeImpl fromNode;
		if (fromStop == null) {
			fromNode = this.startNodes.get(toFacility);
			if (fromNode == null) {
				Coord coord = new CoordImpl(toFacility.getCoord().getX() + 50, toFacility.getCoord().getY() + 50);
				fromNode = this.network.createNode(new IdImpl(this.prefix + this.nodeIdCounter++), coord);
				this.startNodes.put(toFacility, fromNode);
			}
		} else {
			fromNode = this.nodes.get(fromFacility);
		}
		if (fromNode == null) {
			fromNode = this.network.createNode(new IdImpl(this.prefix + this.nodeIdCounter++), fromFacility.getCoord());
			this.nodes.put(fromFacility, fromNode);
		}
		NodeImpl toNode = this.nodes.get(toFacility);
		if (toNode == null) {
			toNode = this.network.createNode(new IdImpl(this.prefix + this.nodeIdCounter++), toFacility.getCoord());
			this.nodes.put(toFacility, toNode);
		}

		Tuple<NodeImpl, NodeImpl> connection = new Tuple<NodeImpl, NodeImpl>(fromNode, toNode);
		LinkImpl link = this.links.get(connection);
		if (link == null) {
			link = this.network.createLink(new IdImpl(this.prefix + this.linkIdCounter++), fromNode, toNode, CoordUtils.calcDistance(fromNode.getCoord(), toNode.getCoord()), 30.0 / 3.6, 500, 1);
			link.setAllowedModes(transitModes);
			this.links.put(connection, link);

			if (toFacility.getLink() == null) {
				toFacility.setLink(link);
			} else {
				List<TransitStopFacility> copies = this.facilityCopies.get(toFacility);
				if (copies == null) {
					copies = new ArrayList<TransitStopFacility>();
					this.facilityCopies.put(toFacility, copies);
				}
				IdImpl newId = new IdImpl(toFacility.getId().toString() + "." + Integer.toString(copies.size() + 1));
				TransitStopFacility newFacility = this.schedule.getBuilder().createTransitStopFacility(newId, toFacility.getCoord(), toFacility.getIsBlockingLane());
				newFacility.setLink(link);
				copies.add(newFacility);
				this.nodes.put(newFacility, toNode);
				this.schedule.addStopFacility(newFacility);
				toStop.setStopFacility(newFacility);
			}
		}
		return link;
	}

	public Link getLinkBetweenStops(final TransitStopFacility fromStop, final TransitStopFacility toStop) {
		NodeImpl fromNode = this.nodes.get(fromStop);
		NodeImpl toNode = this.nodes.get(toStop);
		Tuple<NodeImpl, NodeImpl> connection = new Tuple<NodeImpl, NodeImpl>(fromNode, toNode);
		return this.links.get(connection);
	}

}
