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
		return transform(time);
	}

	/**
	 * Custom transform function (simple example)
	 */
	private ToDoubleFunction<DrtZone> transform(double time) {
		ToDoubleFunction<DrtZone> originalFunction = demandEstimator.getExpectedDemand(time, demandEstimationPeriod);

		double lookAheadTime = 300; //TODO make this a variable (or read from config file: = rebalancing period)
		ToDoubleFunction<DrtZone> originalFunctionWithLookAhead = demandEstimator.getExpectedDemand(time + lookAheadTime, demandEstimationPeriod);

		return drtZone -> {
			double originalValue = Math.max(originalFunction.applyAsDouble(drtZone), originalFunctionWithLookAhead.applyAsDouble(drtZone));
			// Take the larger value of the current estimated demand and the estimated demand of the near future (i.e., with look ahead)
			if (originalValue >= 1) {
				return Math.max(1, originalValue % 5);  //TODO improve this structure
				// For any regions with request, send at least 1 vehicle to that area during the time bin. If there are
				// more requests, send 1 additional vehicle for every 5 (a variable) requests.
			}
			return 0;
		};
	}
}
