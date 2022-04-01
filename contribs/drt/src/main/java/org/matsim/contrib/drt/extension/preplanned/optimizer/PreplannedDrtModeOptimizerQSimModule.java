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

package org.matsim.contrib.drt.extension.preplanned.optimizer;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelTime;

/**
 * @author Michal Maciejewski (michalm)
 */
public class PreplannedDrtModeOptimizerQSimModule extends AbstractDvrpModeQSimModule {
	private final DrtConfigGroup drtCfg;

	public PreplannedDrtModeOptimizerQSimModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	protected void configureQSim() {
		addModalComponent(DrtOptimizer.class, modalProvider(getter -> new PreplannedDrtOptimizer(drtCfg,
				getter.getModal(PreplannedDrtOptimizer.PreplannedSchedules.class), getter.getModal(Network.class),
				getter.getModal(TravelTime.class), getter.getModal(TravelDisutilityFactory.class)
				.createTravelDisutility(getter.getModal(TravelTime.class)), getter.get(MobsimTimer.class),
				getter.getModal(DrtTaskFactory.class), getter.get(EventsManager.class), getter.getModal(Fleet.class))));

		bindModal(DrtTaskFactory.class).toInstance(new DrtTaskFactoryImpl());

		bindModal(VrpAgentLogic.DynActionCreator.class).toProvider(modalProvider(
				getter -> new DrtActionCreator(getter.getModal(PassengerHandler.class), getter.get(MobsimTimer.class),
						getter.get(DvrpConfigGroup.class)))).asEagerSingleton();

		bindModal(VrpOptimizer.class).to(modalKey(DrtOptimizer.class));
	}
}
