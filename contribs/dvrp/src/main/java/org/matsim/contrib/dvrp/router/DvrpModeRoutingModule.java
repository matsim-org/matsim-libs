/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.router;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.core.modal.ModalProviders;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpModeRoutingModule extends AbstractDvrpModeModule {
	private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	public DvrpModeRoutingModule(String mode, LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
		super(mode);
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
	}

	@Override
	public void install() {
		addRoutingModuleBinding(getMode()).toProvider(new DvrpRoutingModuleProvider(getMode()));// not singleton

		modalMapBinder(DvrpRoutingModuleProvider.Stage.class, RoutingModule.class).addBinding(
						DvrpRoutingModuleProvider.Stage.MAIN)
				.toProvider(new DefaultMainLegRouterProvider(getMode()));// not singleton

		bindModal(DefaultMainLegRouter.RouteCreator.class).toProvider(
				new GenericRouteCreatorProvider(getMode(), leastCostPathCalculatorFactory));

		bindModal(DvrpRoutingModule.AccessEgressFacilityFinder.class).toProvider(
						modalProvider(getter -> new DecideOnLinkAccessEgressFacilityFinder(getter.getModal(Network.class))))
				.asEagerSingleton();
	}

	public static class DefaultMainLegRouterProvider extends ModalProviders.AbstractProvider<DvrpMode, RoutingModule> {
		@Inject
		@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
		private TravelTime travelTime;

		@Inject
		private Scenario scenario;

		public DefaultMainLegRouterProvider(String mode) {
			super(mode, DvrpModes::mode);
		}

		@Override
		public RoutingModule get() {
			return new DefaultMainLegRouter(getMode(), getModalInstance(Network.class),
					scenario.getPopulation().getFactory(), getModalInstance(DefaultMainLegRouter.RouteCreator.class));
		}
	}

	private static class GenericRouteCreatorProvider
			extends ModalProviders.AbstractProvider<DvrpMode, GenericRouteCreator> {
		@Inject
		@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
		private TravelTime travelTime;

		private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

		private GenericRouteCreatorProvider(String mode,
				LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
			super(mode, DvrpModes::mode);
			this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
		}

		@Override
		public GenericRouteCreator get() {
			return new GenericRouteCreator(leastCostPathCalculatorFactory, getModalInstance(Network.class), travelTime,
					getModalInstance(TravelDisutilityFactory.class));
		}
	}
}
