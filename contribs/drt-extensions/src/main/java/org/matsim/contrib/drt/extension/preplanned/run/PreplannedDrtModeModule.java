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

package org.matsim.contrib.drt.extension.preplanned.run;

import org.matsim.contrib.drt.fare.DrtFareHandler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtModeRoutingModule;
import org.matsim.contrib.drt.stops.DefaultStopTimeCalculator;
import org.matsim.contrib.drt.stops.StopTimeCalculator;
import org.matsim.contrib.dvrp.fleet.FleetModule;
import org.matsim.contrib.dvrp.load.DvrpLoadType;
import org.matsim.contrib.dvrp.passenger.DefaultDvrpLoadFromTrip;
import org.matsim.contrib.dvrp.passenger.DvrpLoadFromTrip;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingNetworkModule;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

import com.google.common.base.Preconditions;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author michalm (Michal Maciejewski)
 */
public final class PreplannedDrtModeModule extends AbstractDvrpModeModule {

	private final DrtConfigGroup drtCfg;

	public PreplannedDrtModeModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		DvrpModes.registerDvrpMode(binder(), getMode());
		install(new DvrpModeRoutingNetworkModule(getMode(), drtCfg.useModeFilteredSubnetwork));
		bindModal(TravelTime.class).to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
		bindModal(TravelDisutilityFactory.class).toInstance(TimeAsTravelDisutility::new);
		bindModal(StopTimeCalculator.class).toInstance(new DefaultStopTimeCalculator(drtCfg.stopDuration));

		bindModal(DvrpLoadFromTrip.class).toProvider(modalProvider(getter -> {
			DvrpLoadType loadType = getter.getModal(DvrpLoadType.class);
			return new DefaultDvrpLoadFromTrip(loadType, drtCfg.loadParams.defaultRequestDimension);
		})).asEagerSingleton();

		install(new FleetModule(getMode(), drtCfg.vehiclesFile == null ?
				null :
				ConfigGroup.getInputFileURL(getConfig().getContext(), drtCfg.vehiclesFile),
				drtCfg.changeStartLinkToLastLinkInSchedule, drtCfg.loadParams));

		Preconditions.checkArgument(drtCfg.getRebalancingParams().isEmpty(), "Rebalancing must not be enabled."
				+ " It would interfere with simulation of pre-calculated vehicle schedules."
				+ " Remove the rebalancing params from the drt config");

		install(new DrtModeRoutingModule(drtCfg));

		drtCfg.getDrtFareParams()
				.ifPresent(params -> addEventHandlerBinding().toInstance(new DrtFareHandler(getMode(), params)));

		Preconditions.checkArgument(drtCfg.getDrtSpeedUpParams().isEmpty());
	}
}
