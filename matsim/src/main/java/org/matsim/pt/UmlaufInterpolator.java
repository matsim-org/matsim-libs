
/* *********************************************************************** *
 * project: org.matsim.*
 * UmlaufInterpolator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.pt;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public final class UmlaufInterpolator {

	private final Network network;
	private final LeastCostPathCalculator routingAlgo;

	public UmlaufInterpolator(Network network, final ScoringConfigGroup config) {
		super();
		this.network = network;
		FreespeedTravelTimeAndDisutility travelTimes = new FreespeedTravelTimeAndDisutility(config);
		this.routingAlgo = new DijkstraFactory().createPathCalculator(network, travelTimes, travelTimes);
	}

	/**
	 * (also make sure that they are physically connected on the network)
	 */
	public void addUmlaufStueckToUmlauf(UmlaufStueckI umlaufStueck, Umlauf umlauf) {
		List<UmlaufStueckI> existingUmlaufStuecke = umlauf.getUmlaufStuecke();

		// check if final link of last umlaufStueck and first link of new umlaufStueck are connected; otherwise compute and insert connecting route:
		if (! existingUmlaufStuecke.isEmpty()) {
			UmlaufStueckI previousUmlaufStueck = existingUmlaufStuecke.get(existingUmlaufStuecke.size() - 1);
			NetworkRoute previousCarRoute = previousUmlaufStueck.getCarRoute();
			Id<Link> fromLinkId = previousCarRoute.getEndLinkId();
			Id<Link> toLinkId = umlaufStueck.getCarRoute().getStartLinkId();
			if (!fromLinkId.equals(toLinkId)) {
				insertWenden(fromLinkId, toLinkId, umlauf);
			}
		}

		existingUmlaufStuecke.add(umlaufStueck);
	}

	private void insertWenden(Id<Link> fromLinkId, Id<Link> toLinkId, Umlauf umlauf) {
		Node startNode = this.network.getLinks().get(fromLinkId).getToNode();
		Node endNode = this.network.getLinks().get(toLinkId).getFromNode();
		double depTime = 0.0;
		Path wendenPath = routingAlgo.calcLeastCostPath(startNode, endNode, depTime, null, null);
		if (wendenPath == null) {
			throw new RuntimeException("No route found from node "
								   + startNode.getId() + " to node " + endNode.getId() + ".");
		}
		NetworkRoute route = RouteUtils.createLinkNetworkRouteImpl(fromLinkId, toLinkId);
		route.setLinkIds(fromLinkId, NetworkUtils.getLinkIds(wendenPath.links), toLinkId);
		umlauf.getUmlaufStuecke().add(new Wenden(route));
	}

}
