/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package eu.eunoiaproject.bikesharing.framework.router;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.pt.router.TransitRouterNetwork.TransitRouterNetworkNode;

import java.util.List;

class InitialNodeWithSubTrip extends InitialNode {
	private final TransitRouterNetworkNode node;
	private final List<? extends PlanElement> subtrip;

	public InitialNodeWithSubTrip(
			final TransitRouterNetworkNode node,
			final double initialCost,
			final double initialTime,
			final List<? extends PlanElement> subtrip) {
		super(initialCost, initialTime);
		this.node = node;
		this.subtrip = subtrip;
	}

	public TransitRouterNetworkNode getNode() {
		return node;
	}

	public List<? extends PlanElement> getSubtrip() {
		return subtrip;
	}
}
