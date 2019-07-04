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

import java.net.URL;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.DrtModeMinCostFlowRebalancingModule;
import org.matsim.contrib.drt.routing.ClosestAccessEgressStopFinder;
import org.matsim.contrib.drt.routing.DefaultDrtRouteUpdater;
import org.matsim.contrib.drt.routing.DrtRouteUpdater;
import org.matsim.contrib.drt.routing.DrtRoutingModule;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule;
import org.matsim.contrib.drt.routing.StopBasedDrtRoutingModule.AccessEgressStopFinder;
import org.matsim.contrib.dvrp.fleet.FleetModule;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author michalm (Michal Maciejewski)
 */
public final class DrtModeModule extends AbstractDvrpModeModule {
	private final DrtConfigGroup drtCfg;

	public DrtModeModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		DvrpModes.registerDvrpMode(binder(), getMode());

		bindModal(TravelDisutilityFactory.class).toInstance(TimeAsTravelDisutility::new);

		install(new FleetModule(getMode(), drtCfg.getVehiclesFileUrl(getConfig().getContext()),
				drtCfg.isChangeStartLinkToLastLinkInSchedule()));

		if (drtCfg.getMinCostFlowRebalancing().isPresent()) {
			install(new DrtModeMinCostFlowRebalancingModule(drtCfg));
		} else {
			bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
		}

		switch (drtCfg.getOperationalScheme()) {
			case door2door:
				addRoutingModuleBinding(getMode()).toProvider(new DrtRoutingModuleProvider(drtCfg));//not singleton
				break;

			case stopbased:
				bindModal(TransitSchedule.class).toInstance(readTransitSchedule());

				bindModal(DrtRoutingModule.class).toProvider(new DrtRoutingModuleProvider(drtCfg));//not singleton

				addRoutingModuleBinding(getMode()).toProvider(modalProvider(
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

		bindModal(DrtRouteUpdater.class).toProvider(new ModalProviders.AbstractProvider<DrtRouteUpdater>(getMode()) {
			@Inject
			@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
			private Network network;

			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;

			@Inject
			private Population population;

			@Inject
			private Config config;

			@Override
			public DefaultDrtRouteUpdater get() {
				return new DefaultDrtRouteUpdater(drtCfg, network, travelTime,
						getModalInstance(TravelDisutilityFactory.class), population, config);
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
		private PopulationFactory populationFactory;

		@Inject
		@Named(TransportMode.walk)
		private RoutingModule walkRouter;

		@Inject
		private Config config ;

		private DrtRoutingModuleProvider(DrtConfigGroup drtCfg) {
			super(drtCfg.getMode());
			this.drtCfg = drtCfg;
		}

		@Override
		public DrtRoutingModule get() {
//			if ( config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
//				return new DrtInclAccessEgressRoutingModule( drtCfg, network, travelTime, getModalInstance( TravelDisutilityFactory.class ),
//					  populationFactory, walkRouter );
//			} else{
				return new DrtRoutingModule( drtCfg, network, travelTime, getModalInstance( TravelDisutilityFactory.class ),
					  populationFactory, walkRouter );
//			}
		}
	}

	private TransitSchedule readTransitSchedule() {
		URL url = drtCfg.getTransitStopsFileUrl(getConfig().getContext());
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readURL(url);
		return scenario.getTransitSchedule();
	}

}
