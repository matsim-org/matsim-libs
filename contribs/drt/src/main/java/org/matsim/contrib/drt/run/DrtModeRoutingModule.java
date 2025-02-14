/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2022 by the members listed in the COPYING,        *
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
import java.util.Optional;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.drt.estimator.DrtEstimator;
import org.matsim.contrib.drt.estimator.EstimationRoutingModuleProvider;
import org.matsim.contrib.drt.optimizer.constraints.ConstraintSetChooser;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.routing.DefaultDrtRouteConstraintsCalculator;
import org.matsim.contrib.drt.routing.DefaultDrtRouteUpdater;
import org.matsim.contrib.drt.routing.DrtRouteConstraintsCalculator;
import org.matsim.contrib.drt.routing.DrtRouteCreator;
import org.matsim.contrib.drt.routing.DrtRouteUpdater;
import org.matsim.contrib.drt.routing.DrtStopFacility;
import org.matsim.contrib.drt.routing.DrtStopFacilityImpl;
import org.matsim.contrib.drt.routing.DrtStopNetwork;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.passenger.DvrpLoadFromTrip;
import org.matsim.contrib.dvrp.router.ClosestAccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.DecideOnLinkAccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.DefaultMainLegRouter;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingModule.DefaultMainLegRouterProvider;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.router.DvrpRoutingModuleProvider;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.modal.ModalProviders;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * This is a DRT-customised version of DvrpModeRoutingModule
 *
 * @author Michal Maciejewski (michalm)
 */
public class DrtModeRoutingModule extends AbstractDvrpModeModule {
	// AbstractDvrpModeModule AbstractModalModule, which provides bindModal et al.  AbstractDvrpModeModule gets rid of the generics of AbstractModalModule.

	private final DrtConfigGroup drtCfg;

	public DrtModeRoutingModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {

		switch (drtCfg.simulationType){
			case fullSimulation -> addRoutingModuleBinding(getMode()).toProvider(new DvrpRoutingModuleProvider(getMode()));// not singleton
			case estimateAndTeleport -> addRoutingModuleBinding(getMode()).toProvider(new EstimationRoutingModuleProvider(getMode()));// not singleton
		}
		// (this is the normal routing module binding)

		modalMapBinder(DvrpRoutingModuleProvider.Stage.class, RoutingModule.class).addBinding(
						DvrpRoutingModuleProvider.Stage.MAIN)
				.toProvider(new DefaultMainLegRouterProvider(getMode()));// not singleton
		// this seems to bind an enum.  maybe more heavyweight than necessary?

		bindModal(DefaultMainLegRouter.RouteCreator.class).toProvider(
				new DrtRouteCreatorProvider(drtCfg));// not singleton
		// this is used in DvrpModeRoutingModule (recruited by the DvrpRoutingModuleProvider above)

		bindModal(DrtStopNetwork.class).toProvider(new DrtStopNetworkProvider(getConfig(), drtCfg)).asEagerSingleton();
		// yyyy possibly not used for door2door; try to move inside the corresponding switch statement below.  kai, feb'24


		bindModal(DrtRouteConstraintsCalculator.class).toProvider(modalProvider(getter -> new DefaultDrtRouteConstraintsCalculator(
				drtCfg, getter.getModal(ConstraintSetChooser.class)))).in(Singleton.class);
		DrtOptimizationConstraintsSet optimizationConstraintsSet = drtCfg.addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet();
		bindModal(ConstraintSetChooser.class).toProvider(
				() -> (departureTime, accessActLink, egressActLink, person, tripAttributes)
						-> Optional.of(optimizationConstraintsSet)
		).in(Singleton.class);

		switch( drtCfg.operationalScheme ){
			case door2door -> bindModal( AccessEgressFacilityFinder.class ).toProvider(
												       modalProvider( getter -> new DecideOnLinkAccessEgressFacilityFinder( getter.getModal( Network.class ) ) ) )
										       .asEagerSingleton();
			case stopbased, serviceAreaBased -> {
				bindModal( AccessEgressFacilityFinder.class ).toProvider( modalProvider(
						getter -> new ClosestAccessEgressFacilityFinder(
								optimizationConstraintsSet.maxWalkDistance,
													     getter.get( Network.class ),
													     QuadTrees.createQuadTree( getter.getModal( DrtStopNetwork.class ).getDrtStops().values() ) ) ) )
									     .asEagerSingleton();
			}
			default -> throw new IllegalStateException( "Unexpected value: " + drtCfg.operationalScheme );
		}

		// this is, we think, updating the max travel time based on congested travel time and the alpha-beta thing:
		// (yyyy have same problem in Ride mode but do not address it there)
		bindModal(DrtRouteUpdater.class).toProvider(new ModalProviders.AbstractProvider<>(getMode(), DvrpModes::mode) {
			@Inject
			private Population population;

			@Inject
			private Config config;

			@Override
			public DefaultDrtRouteUpdater get() {
				var network = getModalInstance(Network.class);
				var routeCreatorProvider = getModalProvider(DefaultMainLegRouter.RouteCreator.class);
				DrtEstimator drtEstimator = drtCfg.simulationType == DrtConfigGroup.SimulationType.estimateAndTeleport?
					getModalInstance(DrtEstimator.class) : null;
				return new DefaultDrtRouteUpdater(drtCfg, network, population, config, routeCreatorProvider::get, drtEstimator);
			}
		}).asEagerSingleton();

		// this binds the above as a controler listener:
		addControlerListenerBinding().to(modalKey(DrtRouteUpdater.class));

	}

	private static class DrtRouteCreatorProvider extends ModalProviders.AbstractProvider<DvrpMode, DrtRouteCreator> {
		private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

		private final DrtConfigGroup drtCfg;

		private DrtRouteCreatorProvider(DrtConfigGroup drtCfg) {
			super(drtCfg.getMode(), DvrpModes::mode);
			this.drtCfg = drtCfg;
			leastCostPathCalculatorFactory = new SpeedyALTFactory();
		}

		@Override
		public DrtRouteCreator get() {
			var travelTime = getModalInstance(TravelTime.class);
			return new DrtRouteCreator(drtCfg, getModalInstance(Network.class), leastCostPathCalculatorFactory,
					travelTime, getModalInstance(TravelDisutilityFactory.class),
					getModalInstance(DrtRouteConstraintsCalculator.class), 
					getModalInstance(DvrpLoadFromTrip.class), getModalInstance(DvrpLoadType.class));
		}
	}

	private static class DrtStopNetworkProvider extends ModalProviders.AbstractProvider<DvrpMode, DrtStopNetwork> {

		private final DrtConfigGroup drtCfg;
		private final Config config;

		private DrtStopNetworkProvider(Config config, DrtConfigGroup drtCfg) {
			super(drtCfg.getMode(), DvrpModes::mode);
			this.drtCfg = drtCfg;
			this.config = config;
		}

		@Override
		public DrtStopNetwork get() {
			switch (drtCfg.operationalScheme) {
				case door2door:
					return ImmutableMap::of;
				case stopbased:
					return createDrtStopNetworkFromTransitSchedule(config, drtCfg);
				case serviceAreaBased:
					return createDrtStopNetworkFromServiceArea(config, drtCfg, getModalInstance(Network.class));
				default:
					throw new RuntimeException("Unsupported operational scheme: " + drtCfg.operationalScheme);
			}
		}
	}

	private static DrtStopNetwork createDrtStopNetworkFromServiceArea(Config config, DrtConfigGroup drtCfg,
			Network drtNetwork) {
		final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(
				ConfigGroup.getInputFileURL(config.getContext(), drtCfg.drtServiceAreaShapeFile));
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
		URL url = ConfigGroup.getInputFileURL(config.getContext(), drtCfg.transitStopFile);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readURL(url);
		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = scenario.getTransitSchedule()
				.getFacilities()
				.values()
				.stream()
				.map(DrtStopFacilityImpl::createFromFacility)
				.collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, f -> f));
		return () -> drtStops;
	}
}
