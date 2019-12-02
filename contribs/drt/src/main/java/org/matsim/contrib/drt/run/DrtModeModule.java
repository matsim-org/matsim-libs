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
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.optimizer.rebalancing.NoRebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.DrtModeMinCostFlowRebalancingModule;
import org.matsim.contrib.drt.routing.ClosestAccessEgressFacilityFinder;
import org.matsim.contrib.drt.routing.DecideOnLinkAccessEgressFacilityFinder;
import org.matsim.contrib.drt.routing.DefaultDrtRouteUpdater;
import org.matsim.contrib.drt.routing.DrtRouteLegCalculator;
import org.matsim.contrib.drt.routing.DrtRouteUpdater;
import org.matsim.contrib.drt.routing.DrtRoutingModule;
import org.matsim.contrib.drt.routing.DrtRoutingModule.AccessEgressFacilityFinder;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.dvrp.fleet.FleetModule;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.FastAStarEuclideanFactory;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;

/**
 * @author michalm (Michal Maciejewski)
 */
public final class DrtModeModule extends AbstractDvrpModeModule {
	public enum Direction {ACCESS, EGRESS}

	private final DrtConfigGroup drtCfg;

	public DrtModeModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		DvrpModes.registerDvrpMode(binder(), getMode());
		install(DvrpRoutingNetworkProvider.createDvrpModeRoutingNetworkModule(getMode(),
				drtCfg.isUseModeFilteredSubnetwork()));
		bindModal(TravelDisutilityFactory.class).toInstance(TimeAsTravelDisutility::new);

		install(new FleetModule(getMode(), drtCfg.getVehiclesFileUrl(getConfig().getContext()),
				drtCfg.isChangeStartLinkToLastLinkInSchedule()));

		if (drtCfg.getMinCostFlowRebalancing().isPresent()) {
			install(new DrtModeMinCostFlowRebalancingModule(drtCfg));
		} else {
			bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
		}

		addRoutingModuleBinding(getMode()).toProvider(new DrtRoutingModuleProvider(drtCfg));// not singleton

		modalMapBinder(Direction.class, RoutingModule.class);//empty mapbinder for customising access/egress routing
		bindModal(DrtStopNetwork.class).toProvider(new DrtStopNetworkProvider(getConfig(), drtCfg)).asEagerSingleton();

		if (drtCfg.getOperationalScheme() == DrtConfigGroup.OperationalScheme.door2door) {
			bindModal(AccessEgressFacilityFinder.class).toProvider(
					modalProvider(getter -> new DecideOnLinkAccessEgressFacilityFinder(getter.getModal(Network.class))))
					.asEagerSingleton();
		} else {
			bindModal(AccessEgressFacilityFinder.class).toProvider(modalProvider(
					getter -> new ClosestAccessEgressFacilityFinder(drtCfg.getMaxWalkDistance(),
							getter.get(Network.class), getter.getModal(DrtStopNetwork.class)))).asEagerSingleton();
		}

		bindModal(DrtRouteUpdater.class).toProvider(new ModalProviders.AbstractProvider<DrtRouteUpdater>(getMode()) {
			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;

			@Inject
			private Population population;

			@Inject
			private Config config;

			@Override
			public DefaultDrtRouteUpdater get() {
				Network network = getModalInstance(Network.class);
				return new DefaultDrtRouteUpdater(drtCfg, network, travelTime,
						getModalInstance(TravelDisutilityFactory.class), population, config);
			}
		}).asEagerSingleton();

		addControlerListenerBinding().to(modalKey(DrtRouteUpdater.class));
	}

	private static class DrtRoutingModuleProvider extends ModalProviders.AbstractProvider<DrtRoutingModule> {
		@Inject
		@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
		private TravelTime travelTime;

		@Inject
		@Named(TransportMode.walk)
		private RoutingModule walkRouter;

		@Inject
		private Scenario scenario;

		private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = new FastAStarEuclideanFactory();

		private final DrtConfigGroup drtCfg;

		private DrtRoutingModuleProvider(DrtConfigGroup drtCfg) {
			super(drtCfg.getMode());
			this.drtCfg = drtCfg;
		}

		@Override
		public DrtRoutingModule get() {
			Map<Direction, RoutingModule> accessEgressRouters = getModalInstance(
					new TypeLiteral<Map<Direction, RoutingModule>>() {
					});

			DrtRouteLegCalculator drtRouteLegCalculator = new DrtRouteLegCalculator(drtCfg,
					getModalInstance(Network.class), leastCostPathCalculatorFactory, travelTime,
					getModalInstance(TravelDisutilityFactory.class), scenario);

			return new DrtRoutingModule(drtRouteLegCalculator,
					accessEgressRouters.getOrDefault(Direction.ACCESS, walkRouter),
					accessEgressRouters.getOrDefault(Direction.EGRESS, walkRouter),
					getModalInstance(AccessEgressFacilityFinder.class), drtCfg, scenario,
					getModalInstance(Network.class));
		}
	}

	private static class DrtStopNetworkProvider extends ModalProviders.AbstractProvider<DrtStopNetwork> {

		private final DrtConfigGroup drtCfg;
		private final Config config;

		private DrtStopNetworkProvider(Config config, DrtConfigGroup drtCfg) {
			super(drtCfg.getMode());
			this.drtCfg = drtCfg;
			this.config = config;
		}

		@Override
		public DrtStopNetwork get() {
			switch (drtCfg.getOperationalScheme()) {
				case door2door:
					return ImmutableMap::of;
				case stopbased:
					return createDrtStopNetworkFromTransitSchedule(config, drtCfg);
				case serviceAreaBased:
					return createDrtStopNetworkFromServiceArea(config, drtCfg, getModalInstance(Network.class));
				default:
					throw new RuntimeException("Unsupported operational scheme: " + drtCfg.getOperationalScheme());
			}
		}
	}

	private static DrtStopNetwork createDrtStopNetworkFromServiceArea(Config config, DrtConfigGroup drtCfg,
			Network drtNetwork) {
		final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(
				drtCfg.getDrtServiceAreaShapeFileURL(config.getContext()));
		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = drtNetwork.getLinks()
				.values()
				.stream()
				.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getToNode().getCoord(),
						preparedGeometries))
				.map(DrtStopFacilityImpl::createFromLink)
				.collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, f -> f));
		return () -> drtStops;
	}

	private static DrtStopNetwork createDrtStopNetworkFromTransitSchedule(Config config, DrtConfigGroup drtCfg) {
		URL url = drtCfg.getTransitStopsFileUrl(config.getContext());
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readURL(url);
		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = scenario.getTransitSchedule()
				.getFacilities()
				.values()
				.stream()
				.map(DrtStopFacilityImpl::createFromIdentifiableFacility)
				.collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, f -> f));
		return () -> drtStops;
	}
}
