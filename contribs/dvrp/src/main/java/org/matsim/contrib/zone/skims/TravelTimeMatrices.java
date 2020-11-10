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
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.OptionalTime;

import ch.sbb.matsim.analysis.skims.FloatMatrix;
import ch.sbb.matsim.routing.graph.Graph;
import ch.sbb.matsim.routing.graph.LeastCostPathTree;

/**
 * Based on NetworkSkimMatrices from sbb-matsim-extensions
 *
 * @author Michal Maciejewski (michalm)
 */
public final class TravelTimeMatrices {

	public static <T> FloatMatrix<T> calculateTravelTimeMatrix(Network routingNetwork, Map<T, Node> centralNodes,
			double departureTime, TravelTime travelTime, TravelDisutility travelDisutility, int numberOfThreads) {
		Graph graph = new Graph(routingNetwork);
		ExecutorServiceWithResource<LeastCostPathTree> executorService = new ExecutorServiceWithResource<>(
				IntStream.range(0, numberOfThreads)
						.mapToObj(i -> new LeastCostPathTree(graph, travelTime, travelDisutility))
						.collect(toList()));

		FloatMatrix<T> travelTimeMatrix = new FloatMatrix<>(centralNodes.keySet(), Float.NaN);
		Counter counter = new Counter("DVRP free-speed TT matrix: zone ", " / " + centralNodes.size());
		executorService.submitRunnablesAndWait(centralNodes.keySet()
				.stream()
				.map(z -> (lcpTree -> computeForDepartureZone(z, centralNodes, departureTime, travelTimeMatrix, lcpTree,
						counter))));

		executorService.shutdown();
		return travelTimeMatrix;
	}

	private static <T> void computeForDepartureZone(T fromZoneId, Map<T, Node> centralNodes, double departureTime,
			FloatMatrix<T> travelTimeMatrix, LeastCostPathTree lcpTree, Counter counter) {
		counter.incCounter();
		Node fromNode = centralNodes.get(fromZoneId);
		lcpTree.calculate(fromNode.getId().index(), departureTime, null, null);

		for (T toZoneId : centralNodes.keySet()) {
			Node toNode = centralNodes.get(toZoneId);
			int nodeIndex = toNode.getId().index();
			OptionalTime currOptionalTime = lcpTree.getTime(nodeIndex);
			double currTime = currOptionalTime.orElseThrow(() -> new RuntimeException("Undefined Time"));
			double tt = currTime - departureTime;
			travelTimeMatrix.set(fromZoneId, toZoneId, (float)tt);
		}
	}
}
