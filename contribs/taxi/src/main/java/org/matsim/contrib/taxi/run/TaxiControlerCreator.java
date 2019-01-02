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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TaxiControlerCreator {
	public static Controler createControler(Config config, boolean otfvis) {
		adjustTaxiConfig(config);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		addTaxiAsSingleDvrpModeToControler(controler);
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}

	public static void addTaxiAsSingleDvrpModeToControler(Controler controler) {
		addTaxiWithoutDvrpModuleToControler(controler);
		controler.addOverridingModule(DvrpModule.createModuleWithDefaultDvrpModeQSimModule(
				TaxiConfigGroup.get(controler.getConfig()).getMode()));
	}

	public static void addTaxiWithoutDvrpModuleToControler(Controler controler) {
		controler.addOverridingModule(new TaxiModule());
	}

	public static void adjustTaxiConfig(Config config) {
		//no special adjustments (in contrast to Drt)
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
	}
}
