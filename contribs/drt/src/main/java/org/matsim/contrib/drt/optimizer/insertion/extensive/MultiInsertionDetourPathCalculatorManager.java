package org.matsim.contrib.drt.optimizer.insertion.extensive;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.speedy.InertialFlowCutter;
import org.matsim.core.router.speedy.CHBuilder;
import org.matsim.core.router.speedy.CHGraph;
import org.matsim.core.router.speedy.CHTTFCustomizer;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MultiInsertionDetourPathCalculatorManager implements MobsimBeforeCleanupListener {

	private static final Logger LOG = LogManager.getLogger(MultiInsertionDetourPathCalculatorManager.class);

	/**
	 * Static, cross-instance cache for fully built CH graph skeletons.
	 * <p>
	 * Keyed by {@link SpeedyGraph} (i.e. by network topology).  The expensive
	 * CH contraction (InertialFlowCutter + CHBuilder) is performed only once per
	 * network.  The TTF customization (weight assignment) is done on each access
	 * via {@link CHTTFCustomizer} which uses a fingerprint-based fast no-op
	 * check to skip when travel times have not changed.
	 */
	private static final Map<SpeedyGraph, CHGraph> CH_GRAPH_CACHE = new ConcurrentHashMap<>();
	private static final Map<Network, SpeedyGraph> BASE_GRAPH_CACHE = new ConcurrentHashMap<>();

	private final Network network;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final DrtConfigGroup drtCfg;
	private final boolean useCH;
	private final List<MultiInsertionDetourPathCalculator> multiInsertionDetourPathCalculatorList;

	/**
	 * Creates a new manager.
	 *
	 * @param useCH if {@code true}, uses the CH (Contraction Hierarchies) accelerated
	 *              one-to-many path search; derive this from
	 *              {@code config.controller().getRoutingAlgorithmType() == ControllerConfigGroup.RoutingAlgorithmType.CHRouter}.
	 */
	MultiInsertionDetourPathCalculatorManager(Network network, TravelTime travelTime, TravelDisutility travelDisutility,
											  DrtConfigGroup drtCfg, boolean useCH) {
		this.network = network;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.drtCfg = drtCfg;
		this.useCH = useCH;
		this.multiInsertionDetourPathCalculatorList = new ArrayList<>();
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		multiInsertionDetourPathCalculatorList.forEach(i -> i.notifyMobsimBeforeCleanup(e));
	}

	MultiInsertionDetourPathCalculator create() {
		MultiInsertionDetourPathCalculator instance;
		if (useCH) {
			instance = new MultiInsertionDetourPathCalculator(getOrBuildCHGraph(), travelTime, travelDisutility, drtCfg);
		} else {
			instance = new MultiInsertionDetourPathCalculator(network, travelTime, travelDisutility, drtCfg);
		}
		this.multiInsertionDetourPathCalculatorList.add(instance);
		return instance;
	}

	/**
	 * Returns a shared CH graph for this network, building the contraction
	 * hierarchy on first access and (re-)customizing with current travel times.
	 * The contraction topology is cached per network; the TTF customization
	 * uses a fingerprint-based fast check to skip when travel times have not
	 * changed (the common case within a single MATSim iteration).
	 */
	private CHGraph getOrBuildCHGraph() {
		SpeedyGraph baseGraph = BASE_GRAPH_CACHE.computeIfAbsent(network, SpeedyGraphBuilder::build);
		CHGraph chGraph = CH_GRAPH_CACHE.computeIfAbsent(baseGraph, k -> {
			LOG.info("Building CHRouter graph for DRT insertion search on network with {} nodes, {} links – one-time cost.",
					network.getNodes().size(), network.getLinks().size());
			InertialFlowCutter.NDOrderResult ndOrder = new InertialFlowCutter(baseGraph).computeOrderWithBatches();
			return new CHBuilder(baseGraph, travelDisutility).buildWithOrderParallel(ndOrder);
		});
		// (Re-)customise with current travel times.
		// Double-checked locking: check the volatile fingerprint outside the lock
		// to avoid contention when travel times haven't changed (common case within
		// a single MATSim iteration).  Only enter synchronized when a real update
		// is needed; the inner customize() re-checks under lock.
		if (CHTTFCustomizer.needsRecustomization(chGraph, travelTime)) {
			synchronized (chGraph) {
				new CHTTFCustomizer().customize(chGraph, travelTime, travelDisutility);
			}
		}
		return chGraph;
	}
}
