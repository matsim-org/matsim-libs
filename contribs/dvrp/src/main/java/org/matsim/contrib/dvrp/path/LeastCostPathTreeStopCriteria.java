/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.path;

import java.util.BitSet;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.speedy.LeastCostPathTree.StopCriterion;

import com.google.common.base.Preconditions;

/**
 * @author Michal Maciejewski (michalm)
 */
public class LeastCostPathTreeStopCriteria {

	public static StopCriterion and(StopCriterion sc1, StopCriterion sc2) {
		return (nodeIndex, arrivalTime, travelCost, distance, departureTime) ->//
				sc1.stop(nodeIndex, arrivalTime, travelCost, distance, departureTime)//
						&& sc2.stop(nodeIndex, arrivalTime, travelCost, distance, departureTime);
	}

	public static StopCriterion or(StopCriterion sc1, StopCriterion sc2) {
		return (nodeIndex, arrivalTime, travelCost, distance, departureTime) ->//
				sc1.stop(nodeIndex, arrivalTime, travelCost, distance, departureTime)//
						|| sc2.stop(nodeIndex, arrivalTime, travelCost, distance, departureTime);
	}

	public static StopCriterion maxTravelTime(double maxTravelTime) {
		return (nodeIndex, arrivalTime, travelCost, distance, departureTime) -> arrivalTime - departureTime
				> maxTravelTime;
	}

	public static StopCriterion allEndNodesReached(Collection<Node> endNodes) {
		Preconditions.checkArgument(!endNodes.isEmpty(), "At least one end node must be provided.");

		final BitSet nodesToVisit = new BitSet(Id.getNumberOfIds(Node.class));
		endNodes.forEach(node -> nodesToVisit.set(node.getId().index()));

		return new StopCriterion() {
			private int counter = nodesToVisit.cardinality();

			public boolean stop(int nodeIndex, double arrivalTime, double travelCost, double distance,
					double departureTime) {
				if (nodesToVisit.get(nodeIndex)) {
					counter--;
				}
				return counter == 0;
			}
		};
	}
}
