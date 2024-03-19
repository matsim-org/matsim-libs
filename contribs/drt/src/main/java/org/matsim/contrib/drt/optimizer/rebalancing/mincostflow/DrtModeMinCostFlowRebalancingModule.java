/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.PreviousIterationDrtDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.ZonalDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.targetcalculator.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

/**
 * @author michalm
 */
public class DrtModeMinCostFlowRebalancingModule extends AbstractDvrpModeModule {
	private final DrtConfigGroup drtCfg;

	public DrtModeMinCostFlowRebalancingModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		RebalancingParams params = drtCfg.getRebalancingParams().orElseThrow();
		MinCostFlowRebalancingStrategyParams strategyParams = (MinCostFlowRebalancingStrategyParams)params.getRebalancingStrategyParams();

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(RebalancingStrategy.class).toProvider(modalProvider(
						getter -> new MinCostFlowRebalancingStrategy(getter.getModal(RebalancingTargetCalculator.class),
								getter.getModal(ZoneSystem.class), getter.getModal(Fleet.class),
								getter.getModal(ZonalRelocationCalculator.class), params))).asEagerSingleton();

				switch (strategyParams.rebalancingTargetCalculatorType) {
					case EstimatedDemand:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(getter -> new DemandEstimatorAsTargetCalculator(
								getter.getModal(ZonalDemandEstimator.class), strategyParams.demandEstimationPeriod))).asEagerSingleton();
						break;

					case EqualRebalancableVehicleDistribution:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(getter -> new EqualRebalancableVehicleDistributionTargetCalculator(
								getter.getModal(ZonalDemandEstimator.class),
								getter.getModal(ZoneSystem.class), strategyParams.demandEstimationPeriod))).asEagerSingleton();
						break;

					case EqualVehicleDensity:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(
								getter -> new EqualVehicleDensityTargetCalculator(getter.getModal(ZoneSystem.class),
										getter.getModal(FleetSpecification.class)))).asEagerSingleton();
						break;

					case EqualVehiclesToPopulationRatio:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(
								getter -> new EqualVehiclesToPopulationRatioTargetCalculator(
										getter.getModal(ZoneSystem.class), getter.get(Population.class),
										getter.getModal(FleetSpecification.class)))).asEagerSingleton();
						break;

					default:
						throw new IllegalArgumentException("Unsupported rebalancingTargetCalculatorType="
								+ strategyParams.zonalDemandEstimatorType);
				}

				bindModal(ZonalRelocationCalculator.class).toProvider(modalProvider(
						getter -> new AggregatedMinCostRelocationCalculator(
								getter.getModal(DrtZoneTargetLinkSelector.class)))).asEagerSingleton();
			}
		});

		switch (strategyParams.zonalDemandEstimatorType) {
			case PreviousIterationDemand:
				bindModal(PreviousIterationDrtDemandEstimator.class).toProvider(modalProvider(
						getter -> new PreviousIterationDrtDemandEstimator(getter.getModal(ZoneSystem.class), drtCfg,
								strategyParams.demandEstimationPeriod))).asEagerSingleton();
				bindModal(ZonalDemandEstimator.class).to(modalKey(PreviousIterationDrtDemandEstimator.class));
				addEventHandlerBinding().to(modalKey(PreviousIterationDrtDemandEstimator.class));
				break;

			case None:
				break;

			default:
				throw new IllegalArgumentException(
						"Unsupported zonalDemandEstimatorType=" + strategyParams.zonalDemandEstimatorType);
		}
	}
}
