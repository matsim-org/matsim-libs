/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package org.matsim.contrib.zone.skims;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.contrib.zone.Zone;
import org.matsim.contrib.zone.skims.SparseMatrix.NodeAndTime;
import org.matsim.contrib.zone.skims.SparseMatrix.SparseRow;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.speedy.SpeedyGraph;
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
		BiConsumer<LeastCostPathTree, Zone> calculation = (lcpTree, z) -> computeForDepartureZone(z, centralNodes, departureTime, travelTimeMatrix,
				lcpTree);
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

	public static SparseMatrix calculateTravelTimeSparseMatrix(RoutingParams params, double maxDistance, double departureTime) {
		SparseMatrix travelTimeMatrix = new SparseMatrix();
		var nodes = params.routingNetwork.getNodes().values();
		var counter = "DVRP free-speed TT sparse matrix: node ";
		BiConsumer<LeastCostPathTree, Node> calculation = (lcpTree, n) -> computeForDepartureNode(n, nodes, departureTime, travelTimeMatrix, lcpTree,
				maxDistance);
		calculate(params, nodes, calculation, counter);
		return travelTimeMatrix;
	}

	private static void computeForDepartureNode(Node fromNode, Collection<? extends Node> nodes, double departureTime, SparseMatrix sparseMatrix,
			LeastCostPathTree lcpTree, double maxDistance) {
		lcpTree.calculate(fromNode.getId().index(), departureTime, null, null,
				(nodeIndex, arrivalTime, travelCost, distance, departTime) -> distance >= maxDistance);

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

	private static <E> void calculate(RoutingParams params, Collection<? extends E> elements, BiConsumer<LeastCostPathTree, E> calculation,
			String counterPrefix) {
		var trees = IntStream.range(0, params.numberOfThreads)
				.mapToObj(i -> new LeastCostPathTree(new SpeedyGraph(params.routingNetwork), params.travelTime, params.travelDisutility))
				.toList();
		var executorService = new ExecutorServiceWithResource<>(trees);
		var counter = new Counter(counterPrefix, " / " + elements.size());

		executorService.submitRunnablesAndWait(elements.stream().map(e -> (lcpTree -> {
			counter.incCounter();
			calculation.accept(lcpTree, e);
		})));

		counter.printCounter();
		executorService.shutdown();
	}
}
