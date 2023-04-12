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

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.analysis.DrtEventSequenceCollector;
import org.matsim.contrib.drt.fare.DrtFareHandler;
import org.matsim.contrib.drt.optimizer.insertion.DefaultIncrementalStopDurationEstimator;
import org.matsim.contrib.drt.optimizer.insertion.IncrementalStopDurationEstimator;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingModule;
import org.matsim.contrib.drt.schedule.StopDurationEstimator;
import org.matsim.contrib.drt.speedup.DrtSpeedUp;
import org.matsim.contrib.dvrp.fleet.FleetModule;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingNetworkModule;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Key;
import com.google.inject.name.Names;

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
					getter -> new DrtSpeedUp(getMode(), drtSpeedUpParams, getConfig().controler(),
							getter.get(Network.class), getter.getModal(FleetSpecification.class),
							getter.getModal(DrtEventSequenceCollector.class)))).asEagerSingleton();
			addControlerListenerBinding().to(modalKey(DrtSpeedUp.class));
		});

		bindModal(StopDurationEstimator.class).toInstance(
				(vehicle, dropoffRequests, pickupRequests) -> drtCfg.stopDuration);
		bindModal(IncrementalStopDurationEstimator.class).toInstance(
				new DefaultIncrementalStopDurationEstimator(drtCfg.stopDuration));
	}
}
