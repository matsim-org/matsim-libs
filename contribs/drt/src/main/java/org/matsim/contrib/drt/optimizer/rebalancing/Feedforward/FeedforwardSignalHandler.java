package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.NetDepartureReplenishDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.TransportProblem.Flow;
import org.matsim.contrib.util.distance.DistanceUtils;
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
		feedforwardSignal.clear();
		int progressCounter = 0;
		int numOfTimeBin = simulationEndTime * 3600 / timeBinSize;
		log.info("Start calculating rebalnace plan now");
		for (int i = 0; i < numOfTimeBin; i++) {
			double timeBin = (double) i;
			List<Pair<DrtZone, Integer>> supply = new ArrayList<>();
			List<Pair<DrtZone, Integer>> demand = new ArrayList<>();
			for (DrtZone zone : zonalSystem.getZones().values()) {
				ToDoubleFunction<DrtZone> netDepartureInputFunction = netDepartureReplenishDemandEstimator
						.getExpectedDemandForTimeBin(timeBin);
				double netDeparture = netDepartureInputFunction.applyAsDouble(zone);
				if (netDeparture > 0) {
					demand.add(Pair.of(zone, (int) netDeparture));
				} else if (netDeparture < 0) {
					supply.add(Pair.of(zone, (int) (-1 * netDeparture)));
				}
			}
			List<Flow<DrtZone, DrtZone>> interZonalRelocations = new TransportProblem<>(this::calcStraightLineDistance)
					.solve(supply, demand);
			feedforwardSignal.put(timeBin, interZonalRelocations);
			progressCounter += 1;
			log.info("Calculating: " + Double.toString(progressCounter * timeBinSize / simulationEndTime / 36)
					+ "% complete");
		}
		log.info("Rebalance plan calculation is now complete! ");
	}

	private int calcStraightLineDistance(DrtZone zone1, DrtZone zone2) {
		return (int) DistanceUtils.calculateDistance(zone1.getCentroid(), zone2.getCentroid());
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
