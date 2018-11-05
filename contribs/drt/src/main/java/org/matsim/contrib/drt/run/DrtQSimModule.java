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

import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.PrecalculablePathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.passenger.DrtRequestCreator;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.DrtScheduleTimingUpdater;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
		DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Drt.class);

		bind(DrtOptimizer.class).to(DefaultDrtOptimizer.class).asEagerSingleton();

		bind(DefaultUnplannedRequestInserter.class).asEagerSingleton();
		bind(UnplannedRequestInserter.class).to(DefaultUnplannedRequestInserter.class);
		bind(VehicleData.EntryFactory.class).to(VehicleDataEntryFactoryImpl.class).asEagerSingleton();

		bind(DrtTaskFactory.class).to(DrtTaskFactoryImpl.class).asEagerSingleton();

		bind(EmptyVehicleRelocator.class).asEagerSingleton();
		bind(DrtScheduleInquiry.class).asEagerSingleton();
		bind(RequestInsertionScheduler.class).asEagerSingleton();
		bind(DrtScheduleTimingUpdater.class).asEagerSingleton();

		bind(ParallelPathDataProvider.class).asEagerSingleton();
		bind(PrecalculablePathDataProvider.class).to(ParallelPathDataProvider.class);

		Named modeNamed = Names.named(DrtConfigGroup.get(getConfig()).getMode());
		bind(VrpOptimizer.class).annotatedWith(modeNamed).to(DrtOptimizer.class);
		bind(VrpAgentLogic.DynActionCreator.class).annotatedWith(modeNamed)
				.to(DrtActionCreator.class)
				.asEagerSingleton();
		bind(PassengerRequestCreator.class).annotatedWith(modeNamed).to(DrtRequestCreator.class).asEagerSingleton();
		bind(PassengerEngine.class).annotatedWith(Drt.class).to(Key.get(PassengerEngine.class, modeNamed));
	}
}
