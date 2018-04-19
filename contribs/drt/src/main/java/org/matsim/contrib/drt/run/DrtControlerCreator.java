/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.drt.run;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.analysis.DrtAnalysisModule;
import org.matsim.contrib.drt.optimizer.DefaultDrtOptimizer;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.PrecalculatablePathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.passenger.DrtRequestCreator;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.drt.scheduler.DrtScheduleInquiry;
import org.matsim.contrib.drt.scheduler.DrtScheduleTimingUpdater;
import org.matsim.contrib.drt.scheduler.EmptyVehicleRelocator;
import org.matsim.contrib.drt.scheduler.RequestInsertionScheduler;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 *
 */
public final class DrtControlerCreator {

	public static Controler createControler(Config config, boolean otfvis) {
		adjustConfig(config);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		return addDrtToControler(controler, otfvis);
	}

	public static Controler createControler(Scenario scenario, boolean otfvis) {
		// yy I know that this one breaks the sequential loading of the building blocks, but I would like to be able
		// to modify the scenario before I pass it to the controler. kai, oct'17
		adjustConfig(scenario.getConfig());
		Controler controler = new Controler(scenario);
		return addDrtToControler(controler, otfvis);
	}

	public static Controler addDrtToControler(Controler controler, boolean otfvis) {
		controler.addOverridingModule(new DvrpModule(DrtControlerCreator::createModuleForQSimPlugin, Arrays
				.asList(DrtOptimizer.class, DefaultUnplannedRequestInserter.class, ParallelPathDataProvider.class)));
		controler.addOverridingModule(new DrtModule());
		controler.addOverridingModule(new DrtAnalysisModule());
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}

	public static void adjustConfig(Config config) {
		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		if (drtCfg.getOperationalScheme().equals(DrtConfigGroup.OperationalScheme.stopbased)) {
			if (config.planCalcScore().getActivityParams(DrtStageActivityType.DRT_STAGE_ACTIVITY) == null) {
				ActivityParams params = new ActivityParams(DrtStageActivityType.DRT_STAGE_ACTIVITY);
				params.setTypicalDuration(1);
				params.setScoringThisActivityAtAll(false);
				config.planCalcScore().getScoringParametersPerSubpopulation().values()
						.forEach(k -> k.addActivityParams(params));
				config.planCalcScore().addActivityParams(params);
				Logger.getLogger(DrtControlerCreator.class).info(
						"drt interaction scoring parameters not set. Adding default values (activity will not be scored).");
			}
			if (!config.planCalcScore().getModes().containsKey(DrtStageActivityType.DRT_WALK)) {
				ModeParams drtWalk = new ModeParams(DrtStageActivityType.DRT_WALK);
				ModeParams walk = config.planCalcScore().getModes().get(TransportMode.walk);
				drtWalk.setConstant(walk.getConstant());
				drtWalk.setMarginalUtilityOfDistance(walk.getMarginalUtilityOfDistance());
				drtWalk.setMarginalUtilityOfTraveling(walk.getMarginalUtilityOfTraveling());
				drtWalk.setMonetaryDistanceRate(walk.getMonetaryDistanceRate());
				config.planCalcScore().getScoringParametersPerSubpopulation().values()
						.forEach(k -> k.addModeParams(drtWalk));
				Logger.getLogger(DrtControlerCreator.class)
						.info("drt_walk scoring parameters not set. Adding default values (same as for walk mode).");
			}
		}

		config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
		config.checkConsistency();
	}

	public static com.google.inject.Module createModuleForQSimPlugin(Config config) {
		return new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
				DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(),
						DefaultDrtOptimizer.DRT_OPTIMIZER);

				bind(DrtOptimizer.class).to(DefaultDrtOptimizer.class).asEagerSingleton();
				bind(VrpOptimizer.class).to(DrtOptimizer.class);

				bind(DefaultUnplannedRequestInserter.class).asEagerSingleton();
				bind(UnplannedRequestInserter.class).to(DefaultUnplannedRequestInserter.class);
				bind(VehicleData.EntryFactory.class).to(VehicleDataEntryFactoryImpl.class).asEagerSingleton();

				bind(DrtTaskFactory.class).to(DrtTaskFactoryImpl.class).asEagerSingleton();

				bind(EmptyVehicleRelocator.class).asEagerSingleton();
				bind(DrtScheduleInquiry.class).asEagerSingleton();
				bind(RequestInsertionScheduler.class).asEagerSingleton();
				bind(DrtScheduleTimingUpdater.class).asEagerSingleton();

				bind(DynActionCreator.class).to(DrtActionCreator.class).asEagerSingleton();

				bind(PassengerRequestCreator.class).to(DrtRequestCreator.class).asEagerSingleton();

				bind(ParallelPathDataProvider.class).asEagerSingleton();
				bind(PrecalculatablePathDataProvider.class).to(ParallelPathDataProvider.class);
			}
		};
	}
}
