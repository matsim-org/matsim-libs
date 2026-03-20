/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.optimizer.rebalancing.targetcalculator;

import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.ZonalDemandEstimator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * @author ricoraber
 * Within the min cost flow problem, alpha * target(z) + beta vehicles are sent to zone z (if possible).
 * That is, first, beta vehicles are sent to each zone.
 * Afterward, we apply rebalancing based on relative demand in the zones for the remaining rebalancable vehicles:
 * If there are more rebalancable vehicles than demand, then the target per zone is simply the demand in that zone.
 * If there are less rebalancable vehicles than demand, then we compute the relative demand per zone: w(zone) = demand(zone) / totalDemand,
 * and the target per zone is target(zone) = w(zone) * number of rebalancable vehicles.
 */
public class RelativeDemandEstimatorAsTargetCalculator implements RebalancingTargetCalculator {
	private final ZonalDemandEstimator demandEstimator;
	private final ZoneSystem zonalSystem;
	private final double demandEstimationPeriod;
	private final double beta;

	public RelativeDemandEstimatorAsTargetCalculator(ZonalDemandEstimator demandEstimator,	ZoneSystem zonalSystem, double demandEstimationPeriod, double beta) {
		this.demandEstimator = demandEstimator;
		this.demandEstimationPeriod = demandEstimationPeriod;
		this.zonalSystem = zonalSystem;
		this.beta = beta;
	}

	@Override
	public ToDoubleFunction<Zone> calculate(double time,
											Map<Zone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		int rebalancableVehicles = rebalancableVehiclesPerZone.values().stream().mapToInt(List::size).sum();
		int numZones = zonalSystem.getZones().size();
		double remainingVehicles = Math.max(0, rebalancableVehicles - numZones * beta); // How many vehicles are left after sending beta vehicles to each zone
		ToDoubleFunction<Zone> demandPerZone = demandEstimator.getExpectedDemand(time, demandEstimationPeriod);
		double totalDemand = zonalSystem.getZones().values().stream()
				.mapToDouble(demandPerZone)
				.sum();
		return zone -> {
			if (totalDemand > 0) {
				double demand = demandPerZone.applyAsDouble(zone);
				double relativeDemand = demand / totalDemand;
				double targetVehicles = Math.ceil(relativeDemand * remainingVehicles);
				return Math.min(demand, targetVehicles); // do not send more vehicles than demanded trips
			} else {
				return 0;
			}
		};
	}

}
