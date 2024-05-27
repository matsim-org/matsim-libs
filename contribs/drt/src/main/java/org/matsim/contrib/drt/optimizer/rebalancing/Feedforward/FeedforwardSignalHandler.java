package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.NetDepartureReplenishDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem.Flow;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import static java.util.stream.Collectors.toList;

public class FeedforwardSignalHandler implements IterationStartsListener {
	private static final Logger log = LogManager.getLogger(FeedforwardSignalHandler.class);
	private final ZoneSystem zonalSystem;
	private final Map<Integer, List<Flow<Zone, Zone>>> feedforwardSignal = new HashMap<>();
	private final int timeBinSize;
	private final NetDepartureReplenishDemandEstimator netDepartureReplenishDemandEstimator;

	private final int simulationEndTime = 30; // simulation ending time in hour

	/**
	 * Constructor
	 */
	public FeedforwardSignalHandler(ZoneSystem zonalSystem,
			FeedforwardRebalancingStrategyParams strategySpecificParams,
			NetDepartureReplenishDemandEstimator netDepartureReplenishDemandEstimator) {
		this.zonalSystem = zonalSystem;
		this.netDepartureReplenishDemandEstimator = netDepartureReplenishDemandEstimator;
		timeBinSize = strategySpecificParams.timeBinSize;
	}

	private void calculateFeedforwardSignal() {
		netDepartureReplenishDemandEstimator.updateForNextIteration();
		feedforwardSignal.clear();
		int progressCounter = 0;
		int numOfTimeBin = simulationEndTime * 3600 / timeBinSize;
		log.info("Start calculating rebalnace plan now");
		for (int t = 0; t < numOfTimeBin; t++) {
			ToDoubleFunction<Zone> netDepartureInputFunction = netDepartureReplenishDemandEstimator.getExpectedDemandForTimeBin(t);
			List<DrtZoneVehicleSurplus> vehicleSurpluses = zonalSystem.getZones()
					.values()
					.stream()
					.map(z -> new DrtZoneVehicleSurplus(z, (int)netDepartureInputFunction.applyAsDouble(z) * -1))
					.collect(toList());

			feedforwardSignal.put(t, TransportProblem.solveForVehicleSurplus(vehicleSurpluses));
			progressCounter++;
			log.debug("Calculating: " + (double)progressCounter * timeBinSize / simulationEndTime / 36 + "% complete");
		}
		log.info("Rebalance plan calculation is now complete! ");
	}

	public Map<Integer, List<Flow<Zone, Zone>>> getFeedforwardSignal() {
		return feedforwardSignal;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (event.getIteration() > 0) {
			calculateFeedforwardSignal();
		}
	}
}
