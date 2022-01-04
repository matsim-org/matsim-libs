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

import static java.util.stream.Collectors.toSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import org.apache.log4j.Logger;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZone;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.ZonalDemandEstimator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

/**
 * TODO add some description
 * if no demand => zone is not active => target is 0
 * if demand > 0 => equally distribute all available vehicles
 */
public class EqualRebalancableVehicleDistributionTargetCalculator implements RebalancingTargetCalculator {
	private static final Logger log = Logger.getLogger(EqualRebalancableVehicleDistributionTargetCalculator.class);

	private final ZonalDemandEstimator demandEstimator;
	private final DrtZonalSystem zonalSystem;
	private final double demandEstimationPeriod;

	public EqualRebalancableVehicleDistributionTargetCalculator(ZonalDemandEstimator demandEstimator,
			DrtZonalSystem zonalSystem, double demandEstimationPeriod) {
		this.demandEstimator = demandEstimator;
		this.zonalSystem = zonalSystem;
		this.demandEstimationPeriod = demandEstimationPeriod;
	}

	@Override
	public ToDoubleFunction<DrtZone> calculate(double time,
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		int numAvailableVehicles = rebalancableVehiclesPerZone.values().stream().mapToInt(List::size).sum();

		ToDoubleFunction<DrtZone> currentDemandEstimator = demandEstimator.getExpectedDemand(time,
				demandEstimationPeriod);
		Set<DrtZone> activeZones = zonalSystem.getZones()
				.values()
				.stream()
				.filter(zone -> currentDemandEstimator.applyAsDouble(zone) > 0)
				.collect(toSet());

		// TODO enable different methods for real time target generation by adding
		// switch and corresponding parameter entry in the parameter file

		log.debug("Perform Adaptive Real Time Rebalancing now: " + numAvailableVehicles + " vehicles available");

		// First implementation: Simply evenly distribute the rebalancable (i.e. idling
		// and have enough service time) across the network
		if (activeZones.isEmpty()) {
			log.debug(
					"There is no active zones at this time period. No vehicles will be assigned to rebalance task at this period");
			return zone -> 0;
		}

		double targetValue = (double)numAvailableVehicles / activeZones.size();
		return zone -> activeZones.contains(zone) ? targetValue : 0;
	}
}
