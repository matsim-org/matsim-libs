/* *********************************************************************** *
 * project: org.matsim.*
 * ReactRouteGuidance.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

/**
 *
 */
package playground.johannes.itsc08;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.withinday.routeprovider.RouteProvider;

import playground.johannes.eut.EventBasedTTProvider;

/**
 * @author illenberger
 *
 */
public class ReactRouteGuidance implements RouteProvider {

	private LeastCostPathCalculator algorithm;

	private RoutableLinkCost linkcost;

	public ReactRouteGuidance(Network network, TravelTime traveltimes) {
		this.linkcost = new RoutableLinkCost();
		this.linkcost.traveltimes = traveltimes;
		this.algorithm = new Dijkstra(network, this.linkcost, this.linkcost);
	}

	@Override
	public int getPriority() {
		return 10;
	}

	@Override
	public boolean providesRoute(Link currentLinkId, NetworkRouteWRefs subRoute) {
		return true;
	}

	@Override
	public synchronized NetworkRouteWRefs requestRoute(Link departureLink, Link destinationLink,
			double time) {
		if(linkcost.traveltimes instanceof EventBasedTTProvider) {
			((EventBasedTTProvider)linkcost.traveltimes).requestLinkCost();
		}
		Path path = this.algorithm.calcLeastCostPath(departureLink.getToNode(),
					destinationLink.getFromNode(), time);
		NetworkRouteWRefs route = new NodeNetworkRouteImpl();
		route.setStartLink(departureLink);
		route.setEndLink(destinationLink);
		route.setNodes(path.nodes);
		return route;
	}

	@Override
	public void setPriority(int p) {

	}

	private class RoutableLinkCost implements TravelTime, TravelCost {

		private TravelTime traveltimes;

		@Override
		public double getLinkTravelTime(Link link, double time) {
			return this.traveltimes.getLinkTravelTime(link, time);
		}

		@Override
		public double getLinkTravelCost(Link link, double time) {
			return this.traveltimes.getLinkTravelTime(link, time);
		}

	}
}
