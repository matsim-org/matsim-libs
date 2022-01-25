package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import static java.util.stream.Collectors.toList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.NetDepartureReplenishDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem.Flow;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;

public class FeedforwardSignalHandler implements IterationStartsListener {
	private static final Logger log = Logger.getLogger(FeedforwardSignalHandler.class);
	private final DrtZonalSystem zonalSystem;
	private final Map<Double, List<Flow<DrtZone, DrtZone>>> feedforwardSignal = new HashMap<>();
	private final int timeBinSize;
	private final NetDepartureReplenishDemandEstimator netDepartureReplenishDemandEstimator;

	private final int simulationEndTime = 30; // simulation ending time in hour

	/**
	 * Constructor
	 */
	public FeedforwardSignalHandler(DrtZonalSystem zonalSystem,
			FeedforwardRebalancingStrategyParams strategySpecificParams,
			NetDepartureReplenishDemandEstimator netDepartureReplenishDemandEstimator) {
		this.zonalSystem = zonalSystem;
		this.netDepartureReplenishDemandEstimator = netDepartureReplenishDemandEstimator;
		timeBinSize = strategySpecificParams.getTimeBinSize();
	}

	private void calculateFeedforwardSignal() {
		netDepartureReplenishDemandEstimator.update(1);
		feedforwardSignal.clear();
		int progressCounter = 0;
		int numOfTimeBin = simulationEndTime * 3600 / timeBinSize;
		log.info("Start calculating rebalnace plan now");
		for (int i = 0; i < numOfTimeBin; i++) {
			double timeBin = i;
			ToDoubleFunction<DrtZone> netDepartureInputFunction = netDepartureReplenishDemandEstimator.getExpectedDemandForTimeBin(
					timeBin);
			List<DrtZoneVehicleSurplus> vehicleSurpluses = zonalSystem.getZones()
					.values()
					.stream()
					.map(z -> new DrtZoneVehicleSurplus(z, (int)netDepartureInputFunction.applyAsDouble(z) * -1))
					.collect(toList());

			feedforwardSignal.put(timeBin, TransportProblem.solveForVehicleSurplus(vehicleSurpluses));
			progressCounter++;
			log.debug("Calculating: " + (double)progressCounter * timeBinSize / simulationEndTime / 36 + "% complete");
		}
		log.info("Rebalance plan calculation is now complete! ");
	}

	public Map<Double, List<Flow<DrtZone, DrtZone>>> getFeedforwardSignal() {
		return feedforwardSignal;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		int iteration = event.getIteration();
		if (iteration > 0) {
			calculateFeedforwardSignal();
		}
	}
}
