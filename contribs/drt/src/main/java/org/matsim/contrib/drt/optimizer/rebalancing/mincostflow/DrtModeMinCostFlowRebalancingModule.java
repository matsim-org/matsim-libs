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

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.checkerframework.checker.units.qual.C;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.analysis.zonal.*;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategy.RebalancingTargetCalculator;
import org.matsim.contrib.drt.routing.DrtRouteUpdater;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;

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
		MinCostFlowRebalancingParams params = drtCfg.getMinCostFlowRebalancing().get();
		bindModal(DrtZonalSystem.class).toProvider(
				modalProvider(getter -> new DrtZonalSystem(getter.getModal(Network.class), params.getCellSize())));

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(RebalancingStrategy.class).toProvider(modalProvider(
						getter -> new MinCostFlowRebalancingStrategy(getter.getModal(RebalancingTargetCalculator.class),
								getter.getModal(DrtZonalSystem.class), getter.getModal(Fleet.class),
								getter.getModal(MinCostRelocationCalculator.class), params))).asEagerSingleton();

				bindModal(RebalancingTargetCalculator.class).toProvider(modalProvider(
						getter -> new LinearRebalancingTargetCalculator(getter.getModal(ZonalDemandAggregator.class),
								params))).asEagerSingleton();

				bindModal(MinCostRelocationCalculator.class).toProvider(modalProvider(
						getter -> new AggregatedMinCostRelocationCalculator(getter.getModal(DrtZonalSystem.class),
								getter.getModal(Network.class)))).asEagerSingleton();
			}
		});

		switch (params.getZonalDemandAggregatorType()) {
			case PreviousIterationZonalDemandAggregator:
				bindModal(ZonalDemandAggregator.class).toProvider(modalProvider(
						getter -> new PreviousIterationZonalDemandAggregator(getter.get(EventsManager.class),
								getter.getModal(DrtZonalSystem.class), drtCfg))).asEagerSingleton();
				break;
			case ActivityLocationBasedZonalDemandAggregator:
				//TODO sort this out. could not find another way to register the same aggregator instance as controler listener but storing it in a fielf of the provider.. tschlenther may '20

				ModalProviders.AbstractProvider<ActivityLocationBasedZonalDemandAggregator> provider = new ModalProviders.AbstractProvider<ActivityLocationBasedZonalDemandAggregator>(getMode()) {
					@Inject
					MatsimServices services;

					@Override
					public ActivityLocationBasedZonalDemandAggregator get() {
						return new ActivityLocationBasedZonalDemandAggregator(services,
									getModalInstance(DrtZonalSystem.class), drtCfg);
					}
				};

				bindModal(ZonalDemandAggregator.class).toProvider(provider).asEagerSingleton();

				break;
			case EqualVehicleDensityZonalDemandAggregator:
				bindModal(ZonalDemandAggregator.class).toProvider(modalProvider(
						getter -> new EqualVehicleDensityZonalDemandAggregator(getter.getModal(DrtZonalSystem.class),
								getter.getModal(FleetSpecification.class)))).asEagerSingleton();
				break;
		}

	}
}
