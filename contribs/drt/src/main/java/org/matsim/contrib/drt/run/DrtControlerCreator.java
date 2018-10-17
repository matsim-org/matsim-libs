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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.analysis.DrtAnalysisModule;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.insertion.DefaultUnplannedRequestInserter;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;
import java.util.function.Function;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public final class DrtControlerCreator {
    private static final Logger LOGGER = Logger.getLogger(DrtControlerCreator.class);

	/**
	 * Creates a standard scenario and adds a DRT route factory to the default route factories.
	 *
	 * @param config
	 * @return
	 */
	public static Scenario createScenarioWithDrtRouteFactory(Config config) {
		Scenario scenario = ScenarioUtils.createScenario(config);
		addDrtRouteFactory(scenario);
		return scenario;
	}

	public static void addDrtRouteFactory(Scenario scenario) {

		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		if (routeFactories.getRouteClassForType(DrtRoute.ROUTE_TYPE).equals(Route.class)) {
			routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		}
	}

	/**
	 * Creates a controller in one step.
	 *
	 * @param config
	 * @param otfvis
	 * @return
	 */
	public static Controler createControler(Config config, boolean otfvis) {
		return createControler(config, otfvis, cfg -> {
			Scenario scenario = createScenarioWithDrtRouteFactory(cfg);
			ScenarioUtils.loadScenario(scenario);
			return scenario;
		});
	}

	/**
	 * Creates a controller in one step. Allows for customised scenario creation.
	 *
	 * @param config
	 * @param otfvis
	 * @param scenarioLoader
	 * @return
	 */
	public static Controler createControler(Config config, boolean otfvis, Function<Config, Scenario> scenarioLoader) {
		adjustDrtConfig(config);
		Scenario scenario = scenarioLoader.apply(config);
		addDrtRouteFactory(scenario);
		Controler controler = new Controler(scenario);
		addDrtAsSingleDvrpModeToControler(controler);
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}

	public static void addDrtAsSingleDvrpModeToControler(Controler controler) {
		addDrtWithoutDvrpModuleToControler(controler);
		controler.addOverridingModule(DvrpModule.createModule(DrtConfigGroup.get(controler.getConfig()).getMode(),
				Arrays.asList(DrtOptimizer.class, DefaultUnplannedRequestInserter.class,
						ParallelPathDataProvider.class)));
	}

	public static void addDrtWithoutDvrpModuleToControler(Controler controler) {
		controler.addQSimModule(new DrtQSimModule());
		controler.addOverridingModule(new DrtModule());
		controler.addOverridingModule(new DrtAnalysisModule());
	}

	public static void adjustDrtConfig(Config config) {
		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		DrtStageActivityType drtStageActivityType = new DrtStageActivityType(drtCfg.getMode());
		if (drtCfg.getOperationalScheme().equals(DrtConfigGroup.OperationalScheme.stopbased)) {
			if (config.planCalcScore().getActivityParams(drtStageActivityType.drtStageActivity) == null) {
				addDrtStageActivityParams(config, drtStageActivityType.drtStageActivity);
			}
		}
		if (!config.planCalcScore().getModes().containsKey(drtStageActivityType.drtWalk)) {
			addDrtWalkModeParams(config, drtStageActivityType.drtWalk);
		}

		config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
		config.checkConsistency();
	}

	private static void addDrtStageActivityParams(Config config, String stageActivityType) {
		ActivityParams params = new ActivityParams(stageActivityType);
		params.setTypicalDuration(1);
		params.setScoringThisActivityAtAll(false);
		config.planCalcScore()
				.getScoringParametersPerSubpopulation()
				.values()
				.forEach(k -> k.addActivityParams(params));
		config.planCalcScore().addActivityParams(params);
        LOGGER.info("drt interaction scoring parameters not set. Adding default values (activity will not be scored).");
	}

	private static void addDrtWalkModeParams(Config config, String drtWalkMode) {
		ModeParams drtWalk = new ModeParams(drtWalkMode);
		ModeParams walk = config.planCalcScore().getModes().get(TransportMode.walk);
		drtWalk.setConstant(walk.getConstant());
		drtWalk.setMarginalUtilityOfDistance(walk.getMarginalUtilityOfDistance());
		drtWalk.setMarginalUtilityOfTraveling(walk.getMarginalUtilityOfTraveling());
		drtWalk.setMonetaryDistanceRate(walk.getMonetaryDistanceRate());
		config.planCalcScore().getScoringParametersPerSubpopulation().values().forEach(k -> k.addModeParams(drtWalk));
        LOGGER.info("drt_walk scoring parameters not set. Adding default values (same as for walk mode).");

	}
}
