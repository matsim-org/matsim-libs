/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package org.matsim.contrib.zone.skims;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.IdCollectors;
import org.matsim.api.core.v01.IdMap;
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

	public static IdMap<Node, Zone> findOriginDestinationZones(RoutingParams params, Map<Zone, Node> centralNodes, double departureTime,
			boolean origin) {
		IdMap<Node, Zone> originZones = new IdMap<>(Node.class);
		var nodes = params.routingNetwork.getNodes().values();
		var endZones = centralNodes.entrySet().stream().collect(IdCollectors.toIdMap(Node.class, e -> e.getValue().getId(), Map.Entry::getKey));
		// We concurrently put values into originZones, but never twice for the same key. Since we use IdMap, doing so is safe as long as the map
		// does not need to get resized during this operation.
		Calculation<Node> calculation = (lcpTree, n) -> findZoneForNode(n, endZones, departureTime, originZones, lcpTree, origin);
		calculate(params, nodes, calculation, "DVRP free-speed " + (origin ? "origin" : "destination") + " zones: node ");
		return originZones;
	}

	private static void findZoneForNode(Node fromNode, IdMap<Node, Zone> endZones, double departureTime, IdMap<Node, Zone> originZones,
			LeastCostPathTree lcpTree, boolean origin) {
		LeastCostPathTree.StopCriterion stopCriterion = (nodeIndex, arrivalTime, travelCost, distance, departureTime1) -> {
			var stop = endZones.containsKey(nodeIndex);
			if (stop) {
				originZones.put(fromNode.getId(), endZones.get(nodeIndex));
			}
			return stop;
		};

		if (origin) {
			lcpTree.calculate(fromNode.getId().index(), departureTime, null, null, stopCriterion);
		} else {
			lcpTree.calculateBackwards(fromNode.getId().index(), departureTime, null, null, stopCriterion);
		}
	}

	public static SparseMatrix calculateTravelTimeSparseMatrix(RoutingParams params, double maxDistance, double departureTime) {
		SparseMatrix travelTimeMatrix = new SparseMatrix();
		var nodes = params.routingNetwork.getNodes().values();
		Calculation<Node> calculation = (lcpTree, n) -> computeForDepartureNode(n, nodes, departureTime, travelTimeMatrix, lcpTree, maxDistance);
		calculate(params, nodes, calculation, "DVRP free-speed TT sparse matrix: node ");
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

	private interface Calculation<E> {
		void calculate(LeastCostPathTree lcpTree, E element);
	}

	private static <E> void calculate(RoutingParams params, Collection<? extends E> elements, Calculation<E> calculation, String counterPrefix) {
		var trees = IntStream.range(0, params.numberOfThreads)
				.mapToObj(i -> new LeastCostPathTree(new SpeedyGraph(params.routingNetwork), params.travelTime, params.travelDisutility))
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
