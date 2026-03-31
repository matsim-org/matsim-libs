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

import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
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

import java.util.Map;

import static org.matsim.contrib.drt.optimizer.rebalancing.RebalancingModule.REBALANCING_ZONE_SYSTEM;

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
						getter -> {
							ZoneSystem zoneSystem = getter.getModal(new TypeLiteral<Map<String, Provider<ZoneSystem>>>() {})
									.get(REBALANCING_ZONE_SYSTEM).get();
                            return new MinCostFlowRebalancingStrategy(getter.getModal(RebalancingTargetCalculator.class),
                                    zoneSystem, getter.getModal(Fleet.class),
                                    getter.getModal(ZonalRelocationCalculator.class), params);
                        })).asEagerSingleton();

				switch (strategyParams.getRebalancingTargetCalculatorType()) {
					case EstimatedDemand:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(getter -> new DemandEstimatorAsTargetCalculator(
								getter.getModal(ZonalDemandEstimator.class), strategyParams.getDemandEstimationPeriod()))).asEagerSingleton();
						break;

					case EstimatedRelativeDemand:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(getter -> {
							ZoneSystem zoneSystem = getter.getModal(new TypeLiteral<Map<String, Provider<ZoneSystem>>>() {})
								.get(REBALANCING_ZONE_SYSTEM).get();
							return new RelativeDemandEstimatorAsTargetCalculator(
								getter.getModal(ZonalDemandEstimator.class),
								zoneSystem, strategyParams.getDemandEstimationPeriod(),
								strategyParams.getTargetBeta()
							);
						})).asEagerSingleton();
						break;

					case EqualRebalancableVehicleDistribution:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(getter -> {
							ZoneSystem zoneSystem = getter.getModal(new TypeLiteral<Map<String, Provider<ZoneSystem>>>() {})
									.get(REBALANCING_ZONE_SYSTEM).get();
                            return new EqualRebalancableVehicleDistributionTargetCalculator(
                                    getter.getModal(ZonalDemandEstimator.class),
                                    zoneSystem, strategyParams.getDemandEstimationPeriod());
                        })).asEagerSingleton();
						break;

					case EqualVehicleDensity:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(
								getter -> {
									ZoneSystem zoneSystem = getter.getModal(new TypeLiteral<Map<String, Provider<ZoneSystem>>>() {})
											.get(REBALANCING_ZONE_SYSTEM).get();
                                    return new EqualVehicleDensityTargetCalculator(zoneSystem, getter.getModal(FleetSpecification.class));
                                })).asEagerSingleton();
						break;

					case EqualVehiclesToPopulationRatio:
						bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(
								getter -> {
									ZoneSystem zoneSystem = getter.getModal(new TypeLiteral<Map<String, Provider<ZoneSystem>>>() {})
											.get(REBALANCING_ZONE_SYSTEM).get();
                                    return new EqualVehiclesToPopulationRatioTargetCalculator(
                                            zoneSystem, getter.get(Population.class),
                                            getter.getModal(FleetSpecification.class));
                                })).asEagerSingleton();
						break;

					default:
						throw new IllegalArgumentException("Unsupported rebalancingTargetCalculatorType="
								+ strategyParams.getZonalDemandEstimatorType());
				}

				bindModal(ZonalRelocationCalculator.class).toProvider(modalProvider(
						getter -> new AggregatedMinCostRelocationCalculator(
								getter.getModal(DrtZoneTargetLinkSelector.class)))).asEagerSingleton();
			}
		});

		switch (strategyParams.getZonalDemandEstimatorType()) {
			case PreviousIterationDemand:
				bindModal(PreviousIterationDrtDemandEstimator.class).toProvider(modalProvider(
						getter -> {
							ZoneSystem zoneSystem = getter.getModal(new TypeLiteral<Map<String, Provider<ZoneSystem>>>() {})
									.get(REBALANCING_ZONE_SYSTEM).get();
                            return new PreviousIterationDrtDemandEstimator(zoneSystem, drtCfg,
                                    strategyParams.getDemandEstimationPeriod());
                        })).asEagerSingleton();
				bindModal(ZonalDemandEstimator.class).to(modalKey(PreviousIterationDrtDemandEstimator.class));
				addEventHandlerBinding().to(modalKey(PreviousIterationDrtDemandEstimator.class));
				addControllerListenerBinding().to(modalKey(PreviousIterationDrtDemandEstimator.class));
				break;

			case None:
				break;

			default:
				throw new IllegalArgumentException(
						"Unsupported zonalDemandEstimatorType=" + strategyParams.getZonalDemandEstimatorType());
		}
	}
}
