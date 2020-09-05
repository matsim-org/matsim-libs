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
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.EqualVehicleDensityZonalDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.FleetSizeWeightedByActivityEndsDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.FleetSizeWeightedByPopulationShareDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.PreviousIterationDRTDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.demandestimator.ZonalDemandEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.targetcalculator.EqualRebalancableVehicleDistributionTargetCalculator;
import org.matsim.contrib.drt.optimizer.rebalancing.targetcalculator.LinearRebalancingTargetCalculator;
import org.matsim.contrib.drt.optimizer.rebalancing.targetcalculator.RebalancingTargetCalculator;
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
								getter.getModal(DrtZonalSystem.class), getter.getModal(Fleet.class),
								getter.getModal(RelocationCalculator.class), params))).asEagerSingleton();

				switch (strategyParams.getRebalancingTargetCalculatorType()) {
					case LinearRebalancingTarget:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(
								getter -> new LinearRebalancingTargetCalculator(
										getter.getModal(ZonalDemandEstimator.class), strategyParams)))
								.asEagerSingleton();
						break;

					case EqualRebalancableVehicleDistribution:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(
								getter -> new EqualRebalancableVehicleDistributionTargetCalculator(
										getter.getModal(ZonalDemandEstimator.class),
										getter.getModal(DrtZonalSystem.class)))).asEagerSingleton();
						break;

					default:
						throw new IllegalArgumentException("Unsupported rebalancingTargetCalculatorType="
								+ strategyParams.getZonalDemandEstimatorType());
				}

				bindModal(RelocationCalculator.class).toProvider(modalProvider(
						getter -> new AggregatedMinCostRelocationCalculator(
								getter.getModal(DrtZoneTargetLinkSelector.class)))).asEagerSingleton();
			}
		});

		switch (strategyParams.getZonalDemandEstimatorType()) {
			case PreviousIterationDemand:
				bindModal(PreviousIterationDRTDemandEstimator.class).toProvider(modalProvider(
						getter -> new PreviousIterationDRTDemandEstimator(getter.getModal(DrtZonalSystem.class),
								drtCfg))).asEagerSingleton();
				bindModal(ZonalDemandEstimator.class).to(modalKey(PreviousIterationDRTDemandEstimator.class));
				addEventHandlerBinding().to(modalKey(PreviousIterationDRTDemandEstimator.class));
				break;
			case FleetSizeWeightedByActivityEnds:
				bindModal(FleetSizeWeightedByActivityEndsDemandEstimator.class).toProvider(modalProvider(
						getter -> new FleetSizeWeightedByActivityEndsDemandEstimator(
								getter.getModal(DrtZonalSystem.class), getter.getModal(FleetSpecification.class),
								drtCfg))).asEagerSingleton();
				bindModal(ZonalDemandEstimator.class).to(
						modalKey(FleetSizeWeightedByActivityEndsDemandEstimator.class));
				addEventHandlerBinding().to(modalKey(FleetSizeWeightedByActivityEndsDemandEstimator.class));
				break;
			case EqualVehicleDensity:
				bindModal(ZonalDemandEstimator.class).toProvider(modalProvider(
						getter -> new EqualVehicleDensityZonalDemandEstimator(getter.getModal(DrtZonalSystem.class),
								getter.getModal(FleetSpecification.class)))).asEagerSingleton();
				break;
			case FleetSizeWeightedByPopulationShare:
				bindModal(ZonalDemandEstimator.class).toProvider(modalProvider(
						getter -> new FleetSizeWeightedByPopulationShareDemandEstimator(
								getter.getModal(DrtZonalSystem.class), getter.get(Population.class),
								getter.getModal(FleetSpecification.class)))).asEagerSingleton();
				break;
			default:
				throw new IllegalArgumentException(
						"Unsupported zonalDemandEstimatorType=" + strategyParams.getZonalDemandEstimatorType());
		}
	}
}
