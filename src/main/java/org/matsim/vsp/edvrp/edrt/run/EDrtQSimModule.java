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

package org.matsim.vsp.edvrp.edrt.run;

import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.PrecalculablePathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.passenger.DrtRequestCreator;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.DrtScheduleTimingUpdater;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.passenger.PassengerEngineQSimModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentSourceQSimModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.vsp.edvrp.edrt.EDrtActionCreator;
import org.matsim.vsp.edvrp.edrt.optimizer.EDrtOptimizer;
import org.matsim.vsp.edvrp.edrt.optimizer.EDrtVehicleDataEntryFactory;
import org.matsim.vsp.edvrp.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.vsp.edvrp.edrt.scheduler.EmptyVehicleChargingScheduler;

import com.google.inject.Key;

/**
 * @author Michal Maciejewski (michalm)
 */
public class EDrtQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		String mode = DrtConfigGroup.get(getConfig()).getMode();
		install(new VrpAgentSourceQSimModule(mode));
		install(new PassengerEngineQSimModule(mode));

		DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), Drt.class);

		bind(DrtOptimizer.class).to(EDrtOptimizer.class).asEagerSingleton();
		bind(DefaultDrtOptimizer.class).asEagerSingleton();

		bind(EmptyVehicleChargingScheduler.class).asEagerSingleton();

		bind(DefaultUnplannedRequestInserter.class).asEagerSingleton();
		bind(UnplannedRequestInserter.class).to(DefaultUnplannedRequestInserter.class);
		bind(VehicleData.EntryFactory.class).toProvider(
				EDrtVehicleDataEntryFactory.EDrtVehicleDataEntryFactoryProvider.class).asEagerSingleton();

		bind(DrtTaskFactory.class).to(EDrtTaskFactoryImpl.class).asEagerSingleton();

		bind(EmptyVehicleRelocator.class).asEagerSingleton();
		bind(DrtScheduleInquiry.class).asEagerSingleton();
		bind(RequestInsertionScheduler.class).asEagerSingleton();
		bind(DrtScheduleTimingUpdater.class).asEagerSingleton();

		bind(ParallelPathDataProvider.class).asEagerSingleton();
		bind(PrecalculablePathDataProvider.class).to(ParallelPathDataProvider.class);

		DvrpMode dvrpMode = DvrpModes.mode(mode);
		bind(VrpOptimizer.class).annotatedWith(dvrpMode).to(DrtOptimizer.class);
		addQSimComponentBinding(dvrpMode).to(DrtOptimizer.class);
		addQSimComponentBinding(dvrpMode).to(ParallelPathDataProvider.class);
		addQSimComponentBinding(dvrpMode).to(DefaultUnplannedRequestInserter.class);
		bind(VrpAgentLogic.DynActionCreator.class).annotatedWith(dvrpMode)
				.to(EDrtActionCreator.class)
				.asEagerSingleton();
		bind(PassengerRequestCreator.class).annotatedWith(dvrpMode).to(DrtRequestCreator.class).asEagerSingleton();
		bind(PassengerEngine.class).annotatedWith(Drt.class).to(Key.get(PassengerEngine.class, dvrpMode));

	}

}
