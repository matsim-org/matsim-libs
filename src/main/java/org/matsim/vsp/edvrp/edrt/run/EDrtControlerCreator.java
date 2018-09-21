/* *********************************************************************** *
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
 * *********************************************************************** */

package org.matsim.vsp.edvrp.edrt.run;

import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.analysis.DrtAnalysisModule;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.PrecalculablePathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.passenger.DrtRequestCreator;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.DrtModule;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.DrtScheduleTimingUpdater;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vsp.edvrp.edrt.EDrtActionCreator;
import org.matsim.vsp.edvrp.edrt.optimizer.EDrtOptimizer;
import org.matsim.vsp.edvrp.edrt.optimizer.EDrtVehicleDataEntryFactory.EDrtVehicleDataEntryFactoryProvider;
import org.matsim.vsp.edvrp.edrt.optimizer.depot.NearestChargerAsDepot;
import org.matsim.vsp.edvrp.edrt.schedule.EDrtTaskFactoryImpl;
import org.matsim.vsp.edvrp.edrt.scheduler.EmptyVehicleChargingScheduler;

/**
 * @author michalm
 */
public class EDrtControlerCreator {

	public static Controler createControler(Config config, boolean otfvis) {
		DrtControlerCreator.adjustDrtConfig(config);
		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);
		Controler controler = new Controler(scenario);
		addEDrtToController(controler);
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}

	public static void addEDrtToController(Controler controler) {
		String mode = DrtConfigGroup.get(controler.getConfig()).getMode();
		controler.addQSimModule(EDrtControlerCreator.createModuleForQSimPlugin());
		controler.addOverridingModule(DvrpModule.createModule(mode,
				Arrays.asList(DrtOptimizer.class, DefaultUnplannedRequestInserter.class,
						ParallelPathDataProvider.class)));
		controler.addOverridingModule(new DrtModule());
		controler.addOverridingModule(new DrtAnalysisModule());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(DepotFinder.class).to(NearestChargerAsDepot.class);
			}
		});
	}

	public static AbstractQSimModule createModuleForQSimPlugin() {
		return new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
				DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(),
						DefaultDrtOptimizer.DRT_OPTIMIZER);

				bind(DrtOptimizer.class).to(EDrtOptimizer.class).asEagerSingleton();
				bind(VrpOptimizer.class).to(DrtOptimizer.class);
				bind(DefaultDrtOptimizer.class).asEagerSingleton();

				bind(EmptyVehicleChargingScheduler.class).asEagerSingleton();

				bind(DefaultUnplannedRequestInserter.class).asEagerSingleton();
				bind(UnplannedRequestInserter.class).to(DefaultUnplannedRequestInserter.class);
				bind(VehicleData.EntryFactory.class).toProvider(EDrtVehicleDataEntryFactoryProvider.class)
						.asEagerSingleton();

				bind(DrtTaskFactory.class).to(EDrtTaskFactoryImpl.class).asEagerSingleton();

				bind(EmptyVehicleRelocator.class).asEagerSingleton();
				bind(DrtScheduleInquiry.class).asEagerSingleton();
				bind(RequestInsertionScheduler.class).asEagerSingleton();
				bind(DrtScheduleTimingUpdater.class).asEagerSingleton();

				bind(DynActionCreator.class).to(EDrtActionCreator.class).asEagerSingleton();

				bind(PassengerRequestCreator.class).to(DrtRequestCreator.class).asEagerSingleton();

				bind(ParallelPathDataProvider.class).asEagerSingleton();
				bind(PrecalculablePathDataProvider.class).to(ParallelPathDataProvider.class);
			}
		};
	}
}
