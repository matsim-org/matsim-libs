/* *********************************************************************** *
 * project: org.matsim.*
 * DijkstraLegHandler.java
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

package playground.andreas.intersection.dijkstra;

import java.util.ArrayList;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.LegRouter;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;

/**
 * A AbstractPersonAlgorithm that calculates and sets the routes of a person's activities using {@link Dijkstra}.
 * 
 * This class replaces PlansCalcRouteDijkstra. The leg mode handlers can now be plugged into PlansCalcRoute. cdobler, okt'10
 *
 * @author mrieser, aneumann, cdobler
 */
public class DijkstraLegHandler implements LegRouter {

	Network wrappedNetwork;
	Network originalNetwork;
	
	/**
	 * The routing algorithm to be used for finding routes on the network with actual travel times.
	 */
	private final LeastCostPathCalculator routeAlgo;
	
	public DijkstraLegHandler(final Network originalNetwork, final Network wrappedNetwork, final PersonalizableTravelCost costCalculator, final PersonalizableTravelTime timeCalculator) {
		this.routeAlgo = new DijkstraFactory().createPathCalculator(wrappedNetwork, costCalculator, timeCalculator);
	}
	
	@Override
	public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
			String legMode = leg.getMode();
		
		if (TransportMode.car.equals(legMode)) {
			return handleCarLeg(leg, fromAct, toAct, depTime);
		} else {
			throw new RuntimeException("cannot handle legmode '" + legMode + "'.");
		}
	}
	
	/*
	 * Need to override it here, to ensure correct route conversion to wrapped network and vice versa.
	 * That's why I had to change visibility to protected.
	 *
	 * TODO [an] No other transport means are implemented, yet.
	 */
	protected double handleCarLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		double travTime = 0;
		Id fromLinkId = fromAct.getLinkId();
		Id toLinkId = toAct.getLinkId();
		if (fromLinkId == null) throw new RuntimeException("fromLink missing.");
		if (toLinkId == null) throw new RuntimeException("toLink missing.");

		Node startNode = this.wrappedNetwork.getNodes().get(fromLinkId);	// start at the end of the "current" link
		Node endNode = this.wrappedNetwork.getNodes().get(toLinkId); // the target is the start of the link

		if (!toLinkId.equals(fromLinkId)) {
			Path path = null;
			// do not drive/walk around, if we stay on the same link
			path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");

			ArrayList<Node> realRouteNodeList = new ArrayList<Node>();

			for (Node node : path.nodes) {
				realRouteNodeList.add(originalNetwork.getLinks().get(node.getId()).getToNode());
			}

			realRouteNodeList.remove(realRouteNodeList.size() - 1);

			NetworkRoute wrappedRoute = new LinkNetworkRouteImpl(null, null);
			wrappedRoute.setLinkIds(null, NetworkUtils.getLinkIds(RouteUtils.getLinksFromNodes(realRouteNodeList)), null);
			wrappedRoute.setTravelTime(path.travelTime);

			leg.setRoute(wrappedRoute);
			travTime = path.travelTime;
		} else {
			// create an empty route == staying on place if toLink == endLink
			NetworkRoute route = new LinkNetworkRouteImpl(null, null);
			route.setTravelTime(0);
			leg.setRoute(route);
			travTime = 0;
		}

		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy will cause problems with alternative implementations of Leg.  kai, apr'10
		return travTime;
	}

}
