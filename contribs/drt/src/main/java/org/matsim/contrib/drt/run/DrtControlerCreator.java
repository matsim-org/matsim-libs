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

import java.util.function.Function;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author jbischoff
 * @author michalm (Michal Maciejewski)
 */
public final class DrtControlerCreator {

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
		DrtConfigs.adjustDrtConfig(DrtConfigGroup.get(config), config.planCalcScore());

		config.addConfigConsistencyChecker(new DrtConfigConsistencyChecker());
		config.checkConsistency();

		Scenario scenario = scenarioLoader.apply(config);

		Controler controler = new Controler(scenario);
		addDrtAsSingleDvrpModeToControler(controler);
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}

	public static void addDrtAsSingleDvrpModeToControler(Controler controler) {
		addDrtWithoutDvrpModuleToControler(controler);
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(
				DvrpQSimComponents.activateModes(DrtConfigGroup.get(controler.getConfig()).getMode()));
	}

	public static void addDrtWithoutDvrpModuleToControler(Controler controler) {
		controler.addOverridingModule(new DrtModule());
	}
}
