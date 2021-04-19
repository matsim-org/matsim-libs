/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2018.
 */

package org.matsim.contrib.zone.skims;

import static java.util.stream.Collectors.toList;

import java.util.Map;
import java.util.stream.IntStream;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.util.ExecutorServiceWithResource;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.OptionalTime;

import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.speedy.LeastCostPathTree;

/**
 * Based on NetworkSkimMatrices from sbb-matsim-extensions
 *
 * @author Michal Maciejewski (michalm)
 */
public final class TravelTimeMatrices {

	public static Matrix calculateTravelTimeMatrix(Network routingNetwork, Map<Zone, Node> centralNodes,
			double departureTime, TravelTime travelTime, TravelDisutility travelDisutility, int numberOfThreads) {
		SpeedyGraph graph = new SpeedyGraph(routingNetwork);
		ExecutorServiceWithResource<LeastCostPathTree> executorService = new ExecutorServiceWithResource<>(
				IntStream.range(0, numberOfThreads)
						.mapToObj(i -> new LeastCostPathTree(graph, travelTime, travelDisutility))
						.collect(toList()));

		Matrix travelTimeMatrix = new Matrix(centralNodes.keySet());
		Counter counter = new Counter("DVRP free-speed TT matrix: zone ", " / " + centralNodes.size());
		executorService.submitRunnablesAndWait(centralNodes.keySet()
				.stream()
				.map(z -> (lcpTree -> computeForDepartureZone(z, centralNodes, departureTime, travelTimeMatrix, lcpTree,
						counter))));

		executorService.shutdown();
		return travelTimeMatrix;
	}

	private static void computeForDepartureZone(Zone fromZone, Map<Zone, Node> centralNodes, double departureTime,
			Matrix travelTimeMatrix, LeastCostPathTree lcpTree, Counter counter) {
		counter.incCounter();
		Node fromNode = centralNodes.get(fromZone);
		lcpTree.calculate(fromNode.getId().index(), departureTime, null, null);

		for (Zone toZone : centralNodes.keySet()) {
			Node toNode = centralNodes.get(toZone);
			int nodeIndex = toNode.getId().index();
			OptionalTime currOptionalTime = lcpTree.getTime(nodeIndex);
			double currTime = currOptionalTime.orElseThrow(() -> new RuntimeException("Undefined Time. Reason could be that the dvrp network is not fully connected. Please check and/or clean."));
			double tt = currTime - departureTime;
			travelTimeMatrix.set(fromZone, toZone, tt);
		}
	}
}
