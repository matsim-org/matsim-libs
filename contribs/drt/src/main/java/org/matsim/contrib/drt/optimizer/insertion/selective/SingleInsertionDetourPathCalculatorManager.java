package org.matsim.contrib.drt.optimizer.insertion.selective;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.ArrayList;
import java.util.List;

public class SingleInsertionDetourPathCalculatorManager implements MobsimBeforeCleanupListener {

	private final Network network;
	private final TravelTime travelTime;
	private final TravelDisutility travelDisutility;
	private final DrtConfigGroup drtCfg;
	private final LeastCostPathCalculatorFactory factory;
	private final List<SingleInsertionDetourPathCalculator> singleInsertionDetourPathCalculators;

	/**
	 * Creates a new manager using the supplied {@link LeastCostPathCalculatorFactory}.
	 * Prefer injecting the globally bound factory (which honours
	 * {@code config.controller.routingAlgorithmType}) over constructing a factory manually.
	 */
	public SingleInsertionDetourPathCalculatorManager(Network network, TravelTime travelTime,
													  TravelDisutility travelDisutility,
													  DrtConfigGroup drtCfg,
													  LeastCostPathCalculatorFactory factory) {
		this.network = network;
		this.travelTime = travelTime;
		this.travelDisutility = travelDisutility;
		this.drtCfg = drtCfg;
		this.factory = factory;
		this.singleInsertionDetourPathCalculators = new ArrayList<>();
	}

	@Override
	public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
		singleInsertionDetourPathCalculators.forEach(i -> i.notifyMobsimBeforeCleanup(e));
	}

	public SingleInsertionDetourPathCalculator create() {
		SingleInsertionDetourPathCalculator instance = new SingleInsertionDetourPathCalculator(
				network, travelTime, travelDisutility, drtCfg.getNumberOfThreads(), factory);
		this.singleInsertionDetourPathCalculators.add(instance);
		return instance;
	}
}
