package org.matsim.core.router.speedy;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mrieser / Simunto, sponsored by SBB Swiss Federal Railways
 */
public class SpeedyALTFactory implements LeastCostPathCalculatorFactory {

	private final Map<Network, SpeedyGraph> graphs = new ConcurrentHashMap<>();
	private final Map<SpeedyGraph, SpeedyALTData> landmarksData = new ConcurrentHashMap<>();

	@Override
	public LeastCostPathCalculator createPathCalculator(Network network, TravelDisutility travelCosts, TravelTime travelTimes) {
		SpeedyGraph graph = this.graphs.get(network);
		if (graph == null) {
			graph = SpeedyGraphBuilder.build(network);
			this.graphs.put(network, graph);
		}
		SpeedyALTData landmarks = this.landmarksData.get(graph);
		if (landmarks == null) {
			int landmarksCount = Math.min(16, graph.nodeCount);
			landmarks = new SpeedyALTData(graph, landmarksCount, travelCosts);
			this.landmarksData.put(graph, landmarks);
		}
		return new SpeedyALT(landmarks, travelTimes, travelCosts);
	}

}
