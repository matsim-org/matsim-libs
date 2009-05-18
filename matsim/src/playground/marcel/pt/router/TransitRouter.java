/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouter.java
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

package playground.marcel.pt.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.network.Link;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.Time;
import org.matsim.transitSchedule.TransitStopFacility;

import playground.marcel.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;
import playground.marcel.pt.transitSchedule.Departure;
import playground.marcel.pt.transitSchedule.TransitLine;
import playground.marcel.pt.transitSchedule.TransitRoute;
import playground.marcel.pt.transitSchedule.TransitRouteStop;
import playground.marcel.pt.transitSchedule.TransitSchedule;

public class TransitRouter {

	private final TransitSchedule schedule;
	private final TransitRouterNetwork transitNetwork;
	private final TransitRouterNetworkWrapper wrappedNetwork;

	public TransitRouter(final TransitSchedule schedule) {
		this.schedule = schedule;
		this.transitNetwork = buildNetwork();
		this.wrappedNetwork = new TransitRouterNetworkWrapper(this.transitNetwork);
		new NetworkWriter(this.wrappedNetwork, "wrappedNetwork.xml").write();
	}

	public List<Id> calcRoute(final Coord fromCoord, final Coord toCoord, final double departureTime) {
		// find possible start stops
		TransitRouterNetwork.TransitRouterNetworkNode fromNode = this.transitNetwork.getNearestNode(fromCoord);
		TransitRouterNetworkWrapper.NodeWrapper fromNodeWrapped = this.wrappedNetwork.getWrappedNode(fromNode);

		// find possible end stops
		TransitRouterNetwork.TransitRouterNetworkNode toNode = this.transitNetwork.getNearestNode(toCoord);
		TransitRouterNetworkWrapper.NodeWrapper toNodeWrapped = this.wrappedNetwork.getWrappedNode(toNode);

		// find routes between start and end stops

		TransitRouterNetworkTravelTimeCost c = new TransitRouterNetworkTravelTimeCost();
		Dijkstra d = new Dijkstra(this.wrappedNetwork, c, c);
		Path p = d.calcLeastCostPath(fromNodeWrapped, toNodeWrapped, departureTime);
		ArrayList<Id> linkIds = new ArrayList<Id>(p.links.size());
		for (Link l : p.links) {
			linkIds.add(l.getId());
			System.out.println(l.getId().toString());
		}

		// build route
		return linkIds;
	}

	private TransitRouterNetwork buildNetwork() {
		final TransitRouterNetwork network = new TransitRouterNetwork();

		// build nodes and links connecting the nodes according to the transit routes
		for (TransitLine line : this.schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				TransitRouterNetworkNode prevNode = null;
				for (TransitRouteStop stop : route.getStops()) {
					TransitRouterNetworkNode node = network.createNode(stop, route, line);
					if (prevNode != null) {
						/*TransitRouterNetworkLink link = */network.createLink(prevNode, node, route, line);
					}
					prevNode = node;
				}
			}
		}
		network.finishInit(); // not nice to call "finishInit" here before we added all links...

		// connect all stops with walking links if they're located less than 100m from each other
		for (TransitRouterNetworkNode node : network.getNodes()) {
			for (TransitRouterNetworkNode node2 : network.getNearestNodes(node.stop.getStopFacility().getCoord(), 100)) {
				if (node != node2) {
					network.createLink(node, node2, null, null); // not sure if null is correct here
				}
			}
		}

		return network;
	}

	/*package*/ void getNextDeparturesAtStop(final TransitStopFacility stop, final double time) {
		Collection<TransitRouterNetworkNode> nodes = this.transitNetwork.getNearestNodes(stop.getCoord(), 0);
		for (TransitRouterNetworkNode node : nodes) {
			double depDelay = node.stop.getDepartureDelay();
			double routeStartTime = time - depDelay;
			double diff = Double.POSITIVE_INFINITY;
			Departure bestDeparture = null;
			for (Departure departure : node.route.getDepartures().values()) {
				if (routeStartTime <= (departure.getDepartureTime()) && ((departure.getDepartureTime() - routeStartTime) < diff)) {
					bestDeparture = departure;
					diff = departure.getDepartureTime() - routeStartTime;
				}
			}
			if (bestDeparture == null) {
				System.out.println("Line: " + node.line.getId().toString()
					+ "  Route: " + node.route.getId()
					+ "  NO DEPARTURE FOUND!");
			} else {
				System.out.println("Line: " + node.line.getId().toString()
						+ "  Route: " + node.route.getId()
						+ "  Departure at: " + Time.writeTime(bestDeparture.getDepartureTime() + depDelay)
						+ "  Waiting time: " + Time.writeTime(diff));
			}
		}
	}

}
