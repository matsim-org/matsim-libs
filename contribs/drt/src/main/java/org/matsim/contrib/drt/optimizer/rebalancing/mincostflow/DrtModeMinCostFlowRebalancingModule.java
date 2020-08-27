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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.drt.analysis.zonal.EqualVehicleDensityZonalDemandAggregator;
import org.matsim.contrib.drt.analysis.zonal.FleetSizeWeightedByPopulationShareDemandAggregator;
import org.matsim.contrib.drt.analysis.zonal.PreviousIterationZonalDRTDemandAggregator;
import org.matsim.contrib.drt.analysis.zonal.TimeDependentActivityBasedZonalDemandAggregator;
import org.matsim.contrib.drt.analysis.zonal.ZonalDemandAggregator;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategy.RebalancingTargetCalculator;
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
								getter.getModal(MinCostRelocationCalculator.class), params))).asEagerSingleton();

				bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(
						getter -> new LinearRebalancingTargetCalculator(getter.getModal(ZonalDemandAggregator.class),
								strategyParams))).asEagerSingleton();

				bindModal(MinCostRelocationCalculator.class).toProvider(modalProvider(
						getter -> new AggregatedMinCostRelocationCalculator(getter.getModal(DrtZonalSystem.class),
								getter.getModal(Network.class)))).asEagerSingleton();
			}
		});

		switch (strategyParams.getZonalDemandAggregatorType()) {
			case PreviousIteration:
				bindModal(PreviousIterationZonalDRTDemandAggregator.class).toProvider(modalProvider(
						getter -> new PreviousIterationZonalDRTDemandAggregator(getter.getModal(DrtZonalSystem.class),
								drtCfg))).asEagerSingleton();
				bindModal(ZonalDemandAggregator.class).to(modalKey(PreviousIterationZonalDRTDemandAggregator.class));
				addEventHandlerBinding().to(modalKey(PreviousIterationZonalDRTDemandAggregator.class));
				break;
			case TimeDependentActivityBased:
				bindModal(TimeDependentActivityBasedZonalDemandAggregator.class).toProvider(modalProvider(
						getter -> new TimeDependentActivityBasedZonalDemandAggregator(
								getter.getModal(DrtZonalSystem.class), drtCfg))).asEagerSingleton();
				bindModal(ZonalDemandAggregator.class).to(
						modalKey(TimeDependentActivityBasedZonalDemandAggregator.class));
				addEventHandlerBinding().to(modalKey(TimeDependentActivityBasedZonalDemandAggregator.class));
				break;
			case EqualVehicleDensity:
				bindModal(ZonalDemandAggregator.class).toProvider(modalProvider(
						getter -> new EqualVehicleDensityZonalDemandAggregator(getter.getModal(DrtZonalSystem.class),
								getter.getModal(FleetSpecification.class)))).asEagerSingleton();
				break;
			case FleetSizeWeightedByPopulationShare:
				bindModal(ZonalDemandAggregator.class).toProvider(modalProvider(
						getter -> new FleetSizeWeightedByPopulationShareDemandAggregator(
								getter.getModal(DrtZonalSystem.class), getter.get(Population.class),
								getter.getModal(FleetSpecification.class)))).asEagerSingleton();
				break;
			default:
				throw new IllegalArgumentException("do not know what to do with ZonalDemandAggregatorType="
						+ strategyParams.getZonalDemandAggregatorType());
		}
	}
}
