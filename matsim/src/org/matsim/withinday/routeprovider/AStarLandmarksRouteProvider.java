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

import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;

/**
 *
 * @author dgrether
 *
 */
public class AStarLandmarksRouteProvider extends AbstractRouteProvider {

	private FreespeedTravelTimeCost freespeedTravelCost;

	private AStarLandmarks landmarksAStar;

	/**
	 * Constructor for a RouteProvider with a default priority of 0.
	 * @param network
	 */
	public AStarLandmarksRouteProvider(final NetworkLayer network) {
		this(network, 0);
	}

	public AStarLandmarksRouteProvider(final NetworkLayer network, final int priority) {
		this.freespeedTravelCost = new FreespeedTravelTimeCost();
		PreProcessLandmarks preProcess = new PreProcessLandmarks(
				this.freespeedTravelCost);
		preProcess.run(network);
		this.landmarksAStar = new AStarLandmarks(network, preProcess, this.freespeedTravelCost,
				this.freespeedTravelCost);
		super.setPriority(priority);
	}

	@Override
	protected Route requestRoute(final Node departureNode, final Node destinationNode,
			final double time) {
		return this.landmarksAStar.calcLeastCostPath(departureNode, destinationNode, time);
	}

	public boolean providesRoute(final Link currentLink, final Route subRoute) {
		return true;
	}
}
