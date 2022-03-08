package org.matsim.contrib.drt.optimizer.rebalancing.targetcalculator;

import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.ZonalDemandEstimator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public class DemandEstimatorWithCustomMappingFunction implements RebalancingTargetCalculator {
	private final ZonalDemandEstimator demandEstimator;
	private final double demandEstimationPeriod;

	public DemandEstimatorWithCustomMappingFunction(ZonalDemandEstimator demandEstimator, double demandEstimationPeriod) {
		this.demandEstimator = demandEstimator;
		this.demandEstimationPeriod = demandEstimationPeriod;
	}

	@Override
	public ToDoubleFunction<DrtZone> calculate(double time, Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		return transform(demandEstimator.getExpectedDemand(time, demandEstimationPeriod));
	}

	/**
	 * Custom transform function (simple example)
	 */
	private ToDoubleFunction<DrtZone> transform(ToDoubleFunction<DrtZone> input) {
		return drtZone -> {
			double originalOutputValue = input.applyAsDouble(drtZone);
			if (originalOutputValue >= 1) {
				return 1;  // If the region is active, return 1. Otherwise, return 0.
			}
			return 0;
		};
	}
}
