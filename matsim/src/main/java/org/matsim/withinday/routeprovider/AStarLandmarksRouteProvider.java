/* *********************************************************************** *
 * project: org.matsim.*
 * AStarLandmarksRouteProvider.java
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

package org.matsim.withinday.routeprovider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.misc.NetworkUtils;

/**
 *
 * @author dgrether
 *
 */
public class AStarLandmarksRouteProvider extends AbstractRouteProvider {

	private FreespeedTravelTimeCost freespeedTravelCost;

	private final Network network;
	private AStarLandmarks landmarksAStar;

	/**
	 * Constructor for a RouteProvider with a default priority of 0.
	 * @param network
	 */
	public AStarLandmarksRouteProvider(final Network network, final CharyparNagelScoringConfigGroup config) {
		this(network, 0, config);
	}

	public AStarLandmarksRouteProvider(final Network network, final int priority, final CharyparNagelScoringConfigGroup config) {
		this.freespeedTravelCost = new FreespeedTravelTimeCost(config);
		this.network = network;
		PreProcessLandmarks preProcess = new PreProcessLandmarks(
				this.freespeedTravelCost);
		preProcess.run(network);
		this.landmarksAStar = new AStarLandmarks(network, preProcess, this.freespeedTravelCost,
				this.freespeedTravelCost);
		super.setPriority(priority);
	}

	@Override
	public NetworkRouteWRefs requestRoute(final Link departureLink, final Link destinationLink,
			final double time) {
		Path path = this.landmarksAStar.calcLeastCostPath(departureLink.getToNode(), destinationLink.getFromNode(), time);
		NetworkRouteWRefs route = (NetworkRouteWRefs) ((NetworkLayer) network).getFactory().createRoute(TransportMode.car, departureLink.getId(), destinationLink.getId());
		route.setLinkIds(departureLink.getId(), NetworkUtils.getLinkIds(path.links), destinationLink.getId());
		return route;
	}

	public boolean providesRoute(final Id currentLinkId, final NetworkRouteWRefs subRoute) {
		return true;
	}
}
