/* *********************************************************************** *
 * project: org.matsim.* 												   *
 *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2023 by the members listed in the COPYING,        *
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
package org.matsim.contrib.zone.skims;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.contrib.zone.skims.SparseMatrix.NodeAndTime;
import org.matsim.contrib.zone.skims.SparseMatrix.SparseRow;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * Based on NetworkSkimMatrices from sbb-matsim-extensions
 *
 * @author Michal Maciejewski (michalm)
 */
public final class TravelTimeMatrices {

	public record RoutingParams(Network routingNetwork, TravelTime travelTime, TravelDisutility travelDisutility, int numberOfThreads) {
	}

	public static Matrix calculateTravelTimeMatrix(RoutingParams params, Map<Zone, Node> centralNodes, double departureTime) {
		Matrix travelTimeMatrix = new Matrix(centralNodes.keySet());
		Calculation<Zone> calculation = (lcpTree, z) -> computeForDepartureZone(z, centralNodes, departureTime, travelTimeMatrix, lcpTree);
		calculate(params, centralNodes.keySet(), calculation, "DVRP free-speed TT matrix: zone ");
		return travelTimeMatrix;
	}

	private static void computeForDepartureZone(Zone fromZone, Map<Zone, Node> centralNodes, double departureTime, Matrix travelTimeMatrix,
		LeastCostPathTree lcpTree) {
		Node fromNode = centralNodes.get(fromZone);
		lcpTree.calculate(fromNode.getId().index(), departureTime, null, null);

		for (Zone toZone : centralNodes.keySet()) {
			Node toNode = centralNodes.get(toZone);
			int nodeIndex = toNode.getId().index();
			OptionalTime currOptionalTime = lcpTree.getTime(nodeIndex);
			double currTime = currOptionalTime.orElseThrow(() -> new RuntimeException(
				"Undefined Time. Reason could be that the dvrp network is not fully connected. Please check and/or clean."));
			double tt = currTime - departureTime;
			travelTimeMatrix.set(fromZone, toZone, tt);
		}
	}

	public static Optional<SparseMatrix> calculateTravelTimeSparseMatrix(RoutingParams params, double maxDistance, double maxTravelTime,
		double departureTime) {
		SparseMatrix travelTimeMatrix = new SparseMatrix();
		if (maxDistance == 0 && maxTravelTime == 0) {
			return Optional.empty();
		}

		var nodes = params.routingNetwork.getNodes().values();
		var counter = "DVRP free-speed TT sparse matrix: node ";
		Calculation<Node> calculation = (lcpTree, n) -> computeForDepartureNode(n, nodes, departureTime, travelTimeMatrix, lcpTree, maxDistance,
			maxTravelTime);
		calculate(params, nodes, calculation, counter);
		return Optional.of(travelTimeMatrix);
	}

	private static void computeForDepartureNode(Node fromNode, Collection<? extends Node> nodes, double departureTime, SparseMatrix sparseMatrix,
		LeastCostPathTree lcpTree, double maxDistance, double maxTravelTime) {
		lcpTree.calculate(fromNode.getId().index(), departureTime, null, null,
			(nodeIndex, arrivalTime, travelCost, distance, departTime) -> distance >= maxDistance && arrivalTime >= departTime + maxTravelTime);

		List<NodeAndTime> neighborNodes = new ArrayList<>();
		for (Node toNode : nodes) {
			int toNodeIndex = toNode.getId().index();
			OptionalTime currOptionalTime = lcpTree.getTime(toNodeIndex);
			if (currOptionalTime.isUndefined()) {
				continue;
			}
			// these values may not the actual minimum, because many nodes may not get fully relaxed (due to the early termination)
			double currTime = currOptionalTime.seconds();
			double time = currTime - departureTime;
			neighborNodes.add(new NodeAndTime(toNodeIndex, time));
		}

		var sparseRow = new SparseRow(neighborNodes);
		sparseMatrix.setRow(fromNode, sparseRow);
	}

	private interface Calculation<E> {
		void calculate(LeastCostPathTree lcpTree, E element);
	}

	private static <E> void calculate(RoutingParams params, Collection<? extends E> elements, Calculation<E> calculation, String counterPrefix) {
		var trees = IntStream.range(0, params.numberOfThreads)
			.mapToObj(i -> new LeastCostPathTree(SpeedyGraphBuilder.build(params.routingNetwork), params.travelTime, params.travelDisutility))
			.toList();
		var executorService = new ExecutorServiceWithResource<>(trees);
		var counter = new Counter(counterPrefix, " / " + elements.size());

		executorService.submitRunnablesAndWait(elements.stream().map(e -> (lcpTree -> {
			counter.incCounter();
			calculation.calculate(lcpTree, e);
		})));

		counter.printCounter();
		executorService.shutdown();
	}
}
