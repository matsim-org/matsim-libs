package org.matsim.core.router.speedy;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mrieser / Simunto, sponsored by SBB Swiss Federal Railways
 * @author sebhoerl, IRT SystemX
 */
@Singleton
public class SpeedyALTFactory implements LeastCostPathCalculatorFactory {
	private final int threads;

	private final Map<Network, SpeedyGraph> graphs = new ConcurrentHashMap<>();
	private final Map<SpeedyGraph, SpeedyALTData> landmarksData = new ConcurrentHashMap<>();

	@Inject
	public SpeedyALTFactory(final GlobalConfigGroup globalConfigGroup) {
		this(globalConfigGroup.getNumberOfThreads());
	}

	public SpeedyALTFactory(int threads) {
		this.threads = threads;
	}

	public SpeedyALTFactory() {
		this(4);
	}

	@Override
	public LeastCostPathCalculator createPathCalculator(Network network, TravelDisutility travelCosts, TravelTime travelTimes) {
		SpeedyGraph graph = graphs.computeIfAbsent(network, SpeedyGraphBuilder::build);
		
		SpeedyALTData landmarks = landmarksData.computeIfAbsent(graph, g -> {
			int landmarksCount = Math.min(16, g.nodeCount);
			return new SpeedyALTData(g, landmarksCount, travelCosts, threads);
		});
		
		return new SpeedyALT(landmarks, travelTimes, travelCosts);
	}

}
