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

import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.fare.DrtFareHandler;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingModule;
import org.matsim.contrib.drt.prebooking.analysis.PrebookingModeAnalysisModule;
import org.matsim.contrib.drt.speedup.DrtSpeedUp;
import org.matsim.contrib.drt.stops.*;
import org.matsim.contrib.dvrp.fleet.FleetModule;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingNetworkModule;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.zone.skims.AdaptiveTravelTimeMatrixModule;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

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
		install(new DvrpModeRoutingNetworkModule(getMode(), drtCfg.useModeFilteredSubnetwork));
		bindModal(TravelTime.class).to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
		bindModal(TravelDisutilityFactory.class).toInstance(TimeAsTravelDisutility::new);

		install(new FleetModule(getMode(), drtCfg.vehiclesFile == null ?
				null :
				ConfigGroup.getInputFileURL(getConfig().getContext(), drtCfg.vehiclesFile),
				drtCfg.changeStartLinkToLastLinkInSchedule));
		install(new RebalancingModule(drtCfg));
		install(new DrtModeRoutingModule(drtCfg));

		if (drtCfg.getDrtFareParams().isPresent()) {
			var params = drtCfg.getDrtFareParams().get();
			bindModal(DrtFareHandler.class).toInstance(new DrtFareHandler(getMode(), params));
			addEventHandlerBinding().to(modalKey(DrtFareHandler.class));
		}

		drtCfg.getDrtSpeedUpParams().ifPresent(drtSpeedUpParams -> {
			bindModal(DrtSpeedUp.class).toProvider(modalProvider(
					getter -> new DrtSpeedUp(getMode(), drtSpeedUpParams, getConfig().controller(),
							getter.get(Network.class), getter.getModal(FleetSpecification.class),
							getter.getModal(DrtEventSequenceCollector.class)))).asEagerSingleton();
			addControlerListenerBinding().to(modalKey(DrtSpeedUp.class));
		});

		bindModal(PassengerStopDurationProvider.class).toProvider(modalProvider(getter -> {
			return StaticPassengerStopDurationProvider.of(drtCfg.stopDuration, 0.0);
		}));

		bindModal(DefaultStopTimeCalculator.class).toProvider(modalProvider(getter -> {
			return new DefaultStopTimeCalculator(drtCfg.stopDuration);
		})).in(Singleton.class);

		if (drtCfg.getPrebookingParams().isEmpty()) {
			bindModal(StopTimeCalculator.class).toProvider(modalProvider(getter -> {
				return new DefaultStopTimeCalculator(drtCfg.stopDuration);
			})).in(Singleton.class);
		} else {
			bindModal(StopTimeCalculator.class).toProvider(modalProvider(getter -> {
				PassengerStopDurationProvider provider = getter.getModal(PassengerStopDurationProvider.class);
				return new MinimumStopDurationAdapter(new PrebookingStopTimeCalculator(provider), drtCfg.stopDuration);
			}));

			install(new PrebookingModeAnalysisModule(getMode()));
		}

		install(new AdaptiveTravelTimeMatrixModule(drtCfg.mode));
	}
}
