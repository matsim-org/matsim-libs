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

package org.matsim.contrib.taxi.run;

import static org.matsim.contrib.drt.run.DrtControlerCreator.createScenarioWithDrtRouteFactory;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TaxiControlerCreator {
	/**
	 * Creates a controller in one step.
	 *
	 * @param config
	 * @param otfvis
	 * @return
	 */
	public static Controler createControler(Config config, boolean otfvis) {
		Scenario scenario = createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new MultiModeTaxiModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeTaxiConfigGroup.get(config)));

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}
}
