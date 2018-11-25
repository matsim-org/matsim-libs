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

package org.matsim.contrib.drt.run;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.data.validator.DefaultDrtRequestValidator;
import org.matsim.contrib.drt.data.validator.DrtRequestValidator;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.depot.NearestStartLinkAsDepot;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MultiModeMinCostFlowRebalancingModule;
import org.matsim.contrib.drt.routing.ClosestAccessEgressStopFinder;
import org.matsim.contrib.drt.routing.DefaultDrtRouteUpdater;
import org.matsim.contrib.drt.routing.DrtRouteUpdater;
import org.matsim.contrib.drt.routing.DrtRoutingModule;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule.AccessEgressStopFinder;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.file.FleetProvider;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.run.AbstractMultiModeModule;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * @author michalm (Michal Maciejewski)
 */
public final class MultiModeDrtModule extends AbstractMultiModeModule {
	private final DrtConfigGroup drtCfg;

	public MultiModeDrtModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		bindModal(Fleet.class).toProvider(new FleetProvider(drtCfg.getVehiclesFile())).asEagerSingleton();

		bindModal(DrtRequestValidator.class).to(DefaultDrtRequestValidator.class).asEagerSingleton();
		bindModal(DepotFinder.class).toProvider(
				modalProvider(getter -> new NearestStartLinkAsDepot(getter.getModal(Fleet.class))));

		if (MinCostFlowRebalancingParams.isRebalancingEnabled(drtCfg.getMinCostFlowRebalancing())) {
			install(new MultiModeMinCostFlowRebalancingModule(drtCfg));
		} else {
			bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
		}

		bindModal(InsertionCostCalculator.PenaltyCalculator.class).to(drtCfg.isRequestRejection() ?
				InsertionCostCalculator.RejectSoftConstraintViolations.class :
				InsertionCostCalculator.DiscourageSoftConstraintViolations.class).asEagerSingleton();

		switch (drtCfg.getOperationalScheme()) {
			case door2door:
				addRoutingModuleBinding(drtCfg.getMode()).toProvider(
						new DrtRoutingModuleProvider(drtCfg));//not singleton
				break;

			case stopbased:
				bindModal(TransitSchedule.class).toInstance(
						DrtModule.readTransitSchedule(drtCfg.getTransitStopsFileUrl(getConfig().getContext())));

				bindModal(DrtRoutingModule.class).toProvider(new DrtRoutingModuleProvider(drtCfg));//not singleton

				addRoutingModuleBinding(drtCfg.getMode()).toProvider(modalProvider(
						getter -> new StopBasedDrtRoutingModule(getter.get(PopulationFactory.class),
								getter.getModal(DrtRoutingModule.class),
								getter.getNamed(RoutingModule.class, TransportMode.walk),
								getter.getModal(AccessEgressStopFinder.class), drtCfg)));//not singleton

				bindModal(AccessEgressStopFinder.class).toProvider(modalProvider(
						getter -> new ClosestAccessEgressStopFinder(getter.getModal(TransitSchedule.class), drtCfg,
								getter.get(PlansCalcRouteConfigGroup.class), getter.get(Network.class))))
						.asEagerSingleton();
				break;

			default:
				throw new IllegalStateException();
		}

		bindModal(DrtRouteUpdater.class).toProvider(new Provider<DrtRouteUpdater>() {
			@Inject
			@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
			private Network network;

			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;

			@Inject
			@Drt
			private TravelDisutilityFactory travelDisutilityFactory;

			@Inject
			private Population population;

			@Inject
			private Config config;

			@Override
			public DefaultDrtRouteUpdater get() {
				return new DefaultDrtRouteUpdater(drtCfg, network, travelTime, travelDisutilityFactory, population,
						config);
			}
		}).asEagerSingleton();

		addControlerListenerBinding().to(modalKey(DrtRouteUpdater.class));
	}

	private static class DrtRoutingModuleProvider extends ModalProviders.AbstractProvider<DrtRoutingModule> {
		private final DrtConfigGroup drtCfg;

		@Inject
		@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
		private Network network;

		@Inject
		@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
		private TravelTime travelTime;

		@Inject
		@Drt
		private TravelDisutilityFactory travelDisutilityFactory;

		@Inject
		private PopulationFactory populationFactory;

		@Inject
		@Named(TransportMode.walk)
		private RoutingModule walkRouter;

		private DrtRoutingModuleProvider(DrtConfigGroup drtCfg) {
			super(drtCfg.getMode());
			this.drtCfg = drtCfg;
		}

		@Override
		public DrtRoutingModule get() {
			return new DrtRoutingModule(drtCfg, network, travelTime, travelDisutilityFactory, populationFactory,
					walkRouter);
		}
	}
}
