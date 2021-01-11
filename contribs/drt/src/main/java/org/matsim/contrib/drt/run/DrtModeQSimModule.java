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

import org.matsim.contrib.drt.optimizer.DrtModeOptimizerQSimModule;
import org.matsim.contrib.drt.passenger.DrtRequestCreator;
import org.matsim.contrib.drt.speedup.DrtSpeedUp;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.TeleportingPassengerEngine.TeleportedRouteCalculator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtModeQSimModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtCfg;
	private final AbstractQSimModule optimizerQSimModule;

	public DrtModeQSimModule(DrtConfigGroup drtCfg) {
		this(drtCfg, new DrtModeOptimizerQSimModule(drtCfg));
	}

	public DrtModeQSimModule(DrtConfigGroup drtCfg, AbstractQSimModule optimizerQSimModule) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
		this.optimizerQSimModule = optimizerQSimModule;
	}

	@Override
	protected void configureQSim() {
		boolean teleportDrtUsers = drtCfg.getDrtSpeedUpParams().isPresent() && DrtSpeedUp.isTeleportDrtUsers(
				drtCfg.getDrtSpeedUpParams().get(), getConfig().controler(), getIterationNumber());
		if (teleportDrtUsers) {
			install(new PassengerEngineQSimModule(getMode(),
					PassengerEngineQSimModule.PassengerEngineType.TELEPORTING));
			bindModal(TeleportedRouteCalculator.class).toProvider(
					modalProvider(getter -> getter.getModal(DrtSpeedUp.class).createTeleportedRouteCalculator()))
					.asEagerSingleton();
		} else {
			install(new VrpAgentSourceQSimModule(getMode()));
			install(new PassengerEngineQSimModule(getMode()));
			install(optimizerQSimModule);
		}

		bindModal(PassengerRequestValidator.class).to(DefaultPassengerRequestValidator.class).asEagerSingleton();

		bindModal(PassengerRequestCreator.class).toProvider(new Provider<DrtRequestCreator>() {
			@Inject
			private EventsManager events;
			@Inject
			private MobsimTimer timer;

			@Override
			public DrtRequestCreator get() {
				return new DrtRequestCreator(getMode(), events, timer);
			}
		}).asEagerSingleton();
	}
}
