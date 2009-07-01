/* *********************************************************************** *
 * project: org.matsim.*
 * PreProcessEuclidean.java
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

package org.matsim.core.router.util;

import org.apache.log4j.Logger;

import org.matsim.core.api.network.*;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Pre-processes a given network, gathering information which can be used by a
 * AStarEuclidean when computing least-cost paths between a start and an end
 * node. Specifically, computes the minimal travel cost per length unit over all
 * links, which is used by AStarEuclidean's heuristic function during routing.
 *
 * @author lnicolas
 */
public class PreProcessEuclidean extends PreProcessDijkstra {

	private static final Logger log = Logger.getLogger(PreProcessEuclidean.class);

	// Must be initialized to MAX_VALUE, otherwise updateMaxFreeSpeed(...) does
	// not change minTravelCostPerLength
	private double minTravelCostPerLength = Double.POSITIVE_INFINITY;

	protected TravelMinCost costFunction;

	/**
	 * @param costFunction
	 *          A cost function that returns the minimal possible cost for each link.
	 */
	public PreProcessEuclidean(final TravelMinCost costFunction) {
		this.costFunction = costFunction;
	}

	@Override
	public void run(final NetworkLayer network) {
		super.run(network);

		if (checkLinkLengths(network) == false) {
			log.warn("There are links with stored length smaller than their Euclidean distance in this network. Thus, A* cannot guarantee to calculate the least-cost paths between two nodes.  This warning may be given although the A* router is nowhere used.");
		}

		updateMinTravelCostPerLength(network);
	}

	void updateMinTravelCostPerLength(final NetworkLayer network) {
		for (LinkImpl link : network.getLinks().values()) {
			double minCost = this.costFunction.getLinkMinimumTravelCost(link) / link.getLength();
			if (getMinTravelCostPerLength() > minCost) {
				setMinTravelCostPerLength(minCost);
			}
		}
	}

	private boolean checkLinkLengths(final NetworkLayer network) {
		for (LinkImpl link : network.getLinks().values()) {
			double linkLength = link.getLength();
			double eucDist = CoordUtils.calcDistance(link.getFromNode().getCoord(), link.getToNode().getCoord());
			if (linkLength < eucDist) {
				if (log.isDebugEnabled()) {
					log.debug("link " + link.getId() + " has length " + linkLength + " which is smaller than the euclidean distance " + eucDist);
				}
				return false;
			}
		}
		return true;
	}

	void setMinTravelCostPerLength(final double maxFreeSpeed) {
		this.minTravelCostPerLength = maxFreeSpeed;
	}

	/**
	 * @return the minimal travel cost per length unit over all links in the
	 *         network.
	 */
	public double getMinTravelCostPerLength() {
		return this.minTravelCostPerLength;
	}

	/**
	 * @return The cost function that was used to calculate the minimal travel
	 *         cost per length unit over all links in the network.
	 */
	public TravelCost getCostFunction() {
		return this.costFunction;
	}
}
