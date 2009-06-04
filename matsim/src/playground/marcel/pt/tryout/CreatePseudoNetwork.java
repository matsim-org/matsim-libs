/* *********************************************************************** *
 * project: org.matsim.*
 * CreatePseudoNetwork.java
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

package playground.marcel.pt.tryout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.core.api.network.Link;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.network.Node;
import org.matsim.core.api.population.NetworkRoute;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.LinkNetworkRoute;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitSchedule;

public class CreatePseudoNetwork {

//	public static final String INPUT_SCHEDULE = "../thesis-data/examples/berta/schedule.xml";
//	public static final String OUTPUT_SCHEDULE = "../thesis-data/examples/berta/pseudoSchedule.xml";
//	public static final String OUTPUT_NETWORK = "../thesis-data/examples/berta/pseudoNetwork.xml";
	
	private Map<Tuple<TransitStopFacility, TransitStopFacility>, Link> links = null;
	private Map<TransitStopFacility, Node> nodes = null;
	private final NetworkLayer network;
	private final TransitSchedule schedule;
	private long linkIdCounter = 0;
	private long nodeIdCounter = 0;
	
	public CreatePseudoNetwork(final TransitSchedule schedule, final NetworkLayer network) {
		this.network = network;
		this.schedule = schedule;
	}
	
	public Network run() {
		this.linkIdCounter = 0;
		this.nodeIdCounter = 0;
//		try {
//			new TransitScheduleReaderV1(schedule, null).readFile(INPUT_SCHEDULE);
//		} catch (SAXException e) {
//			e.printStackTrace();
//		} catch (ParserConfigurationException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		
		this.links = new HashMap<Tuple<TransitStopFacility, TransitStopFacility>, Link>();
		this.nodes = new HashMap<TransitStopFacility, Node>();
		
		List<Tuple<TransitLine, TransitRoute>> toBeRemoved = new LinkedList<Tuple<TransitLine, TransitRoute>>();
		
		for (TransitLine tLine : schedule.getTransitLines().values()) {
			for (TransitRoute tRoute : tLine.getRoutes().values()) {
				ArrayList<Link> routeLinks = new ArrayList<Link>();
				TransitStopFacility prevFacility = null;
				for (TransitRouteStop stop : tRoute.getStops()) {
					TransitStopFacility facility = stop.getStopFacility();
					if (prevFacility != null) {
						Link link = getNetworkLink(prevFacility, facility);
						// add link to route
						routeLinks.add(link);
					}
					prevFacility = facility;
				}

				if (routeLinks.size() > 0) {
					Link startLink = routeLinks.get(0);
					List<Link> linksBetween = (routeLinks.size() > 2) ? routeLinks.subList(1, routeLinks.size() - 1) : new ArrayList<Link>(0);
					Link endLink = routeLinks.get(routeLinks.size() - 1);
					NetworkRoute route = new LinkNetworkRoute(startLink, endLink);
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
		
//		new NetworkWriter(network, OUTPUT_NETWORK).write();
//		try {
//			new TransitScheduleWriterV1(schedule).write(OUTPUT_SCHEDULE);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		return network;
	}
	
	private Link getNetworkLink(final TransitStopFacility fromStop, final TransitStopFacility toStop) {
		Tuple<TransitStopFacility, TransitStopFacility> connection = new Tuple<TransitStopFacility, TransitStopFacility>(fromStop, toStop);
		Link link = links.get(connection);
		if (link == null) {
			Node fromNode = nodes.get(fromStop);
			if (fromNode == null) {
				fromNode = network.createNode(new IdImpl(nodeIdCounter++), fromStop.getCoord());
				nodes.put(fromStop, fromNode);
			}
			Node toNode = nodes.get(toStop);
			if (toNode == null) {
				toNode = network.createNode(new IdImpl(nodeIdCounter++), toStop.getCoord());
				nodes.put(toStop, toNode);
			}
			link = network.createLink(new IdImpl(linkIdCounter++), fromNode, toNode, CoordUtils.calcDistance(fromStop.getCoord(), toStop.getCoord()), 30.0 / 3.6, 500, 1);
			links.put(new Tuple<TransitStopFacility, TransitStopFacility>(fromStop, toStop), link);
		}
		return link;
	}
}
