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
import java.util.OptionalInt;
import java.util.function.IntToDoubleFunction;
import java.util.function.ToIntFunction;

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

	public static StopCriterion withMaxTravelTime(StopCriterion chainedCriterion, double maxTravelTime) {
		return maxTravelTime < Double.POSITIVE_INFINITY ?
				or(maxTravelTime(maxTravelTime), chainedCriterion) :
				chainedCriterion;
	}

	public static StopCriterion maxTravelTime(double maxTravelTime) {
		Preconditions.checkArgument(maxTravelTime >= 0);
		return (nodeIndex, arrivalTime, travelCost, distance, departureTime) -> arrivalTime - departureTime
				> maxTravelTime;
	}

	public static StopCriterion allEndNodesReached(Collection<Node> endNodes) {
		return allEndNodesReached(endNodes, node -> node.getId().index());
	}

	/**
	 * Creates a stop criterion that fires when all end nodes have been reached.
	 * Uses the provided {@code nodeIndexFunction} to translate MATSim nodes to
	 * internal tree indices, which is necessary when the graph uses spatial
	 * (Z-order) node reordering.
	 *
	 * @param endNodes           the target nodes
	 * @param nodeIndexFunction  maps a MATSim {@link Node} to the internal index
	 *                           used by the shortest-path tree (e.g. {@code graph::getNodeIndex})
	 */
	public static StopCriterion allEndNodesReached(Collection<Node> endNodes,
			ToIntFunction<Node> nodeIndexFunction) {
		Preconditions.checkArgument(!endNodes.isEmpty(), "At least one end node must be provided.");

		final BitSet nodesToVisit = new BitSet();
		endNodes.forEach(node -> nodesToVisit.set(nodeIndexFunction.applyAsInt(node)));

		return new StopCriterion() {
			private int counter = nodesToVisit.cardinality();

			public boolean stop(int nodeIndex, double arrivalTime, double travelCost, double distance,
					double departureTime) {
				if (nodesToVisit.get(nodeIndex)) {
					counter--;
					nodesToVisit.clear(nodeIndex);
				}
				return counter == 0; // stop if all end nodes reached
			}
		};
	}

	public static class LeastCostEndNodeReached implements StopCriterion {
		// zero or positive values allowed
		private final IntToDoubleFunction additionalCostByNodeIndex;

		private final BitSet nodesToVisit = new BitSet();
		private int counter;

		private int bestEndNodeIndex = -1;
		private double bestEndNodeCost = Double.POSITIVE_INFINITY;

		public LeastCostEndNodeReached(Collection<Node> endNodes, IntToDoubleFunction additionalCostByNodeIndex) {
			this(endNodes, additionalCostByNodeIndex, node -> node.getId().index());
		}

		/**
		 * @param endNodes                  the target nodes
		 * @param additionalCostByNodeIndex additional cost function per node index
		 * @param nodeIndexFunction         maps a MATSim {@link Node} to the internal
		 *                                  index used by the shortest-path tree
		 */
		public LeastCostEndNodeReached(Collection<Node> endNodes, IntToDoubleFunction additionalCostByNodeIndex,
				ToIntFunction<Node> nodeIndexFunction) {
			Preconditions.checkArgument(!endNodes.isEmpty(), "At least one end node must be provided.");

			this.additionalCostByNodeIndex = additionalCostByNodeIndex;
			endNodes.forEach(node -> nodesToVisit.set(nodeIndexFunction.applyAsInt(node)));
			counter = nodesToVisit.cardinality();
		}

		public boolean stop(int nodeIndex, double arrivalTime, double travelCost, double distance,
				double departureTime) {
			if (travelCost >= bestEndNodeCost) {
				return true; // stop - no other end node can be better than the current bestEndNode
			}

			if (nodesToVisit.get(nodeIndex)) {
				counter--;

				double endNodeCost = travelCost + additionalCostByNodeIndex.applyAsDouble(nodeIndex);
				if (endNodeCost < bestEndNodeCost) {
					bestEndNodeIndex = nodeIndex;
					bestEndNodeCost = endNodeCost;

					if (endNodeCost == travelCost) {// additionalCost is 0
						return true; // stop - no other end node can be better than the current bestEndNode
					}
				}
			}
			return counter == 0; // stop if all end nodes reached
		}

		public OptionalInt getBestEndNodeIndex() {
			return bestEndNodeIndex == -1 ? OptionalInt.empty() : OptionalInt.of(bestEndNodeIndex);
		}
	}
}
