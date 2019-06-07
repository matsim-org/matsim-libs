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

package org.matsim.contrib.av.maxspeed;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.trafficmonitoring.TravelTimeUtils;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleType;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpTravelTimeWithMaxSpeedLimitModule extends AbstractModule {
	private final VehicleType vehicleType;

	public DvrpTravelTimeWithMaxSpeedLimitModule(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	@Inject
	private DvrpConfigGroup dvrpCfg;

	@Override
	public void install() {
		bind(VehicleType.class).annotatedWith(Names.named(VrpAgentSourceQSimModule.DVRP_VEHICLE_TYPE))
				.toInstance(vehicleType);
		bind(QSimFreeSpeedTravelTimeWithMaxSpeedLimit.class).asEagerSingleton();
		addTravelTimeBinding(DvrpTravelTimeModule.DVRP_INITIAL).to(QSimFreeSpeedTravelTimeWithMaxSpeedLimit.class);
		String mobsimMode = dvrpCfg.getMobsimMode();
		addTravelTimeBinding(DvrpTravelTimeModule.DVRP_OBSERVED).
				toProvider(new Provider<TravelTime>() {
					@Inject
					Injector injector;
					@Inject
					QSimFreeSpeedTravelTimeWithMaxSpeedLimit qSimFreeSpeedTravelTimeWithMaxSpeedLimit;

					@Override
					public TravelTime get() {
						return TravelTimeUtils.maxOfTravelTimes(qSimFreeSpeedTravelTimeWithMaxSpeedLimit,
								injector.getInstance(Key.get(TravelTime.class, Names.named(mobsimMode))));
					}
				});
	}
}

