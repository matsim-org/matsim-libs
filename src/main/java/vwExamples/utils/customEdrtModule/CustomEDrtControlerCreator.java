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

package vwExamples.utils.customEdrtModule;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.ev.EvModule;
import org.matsim.contrib.ev.dvrp.EvDvrpIntegrationModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author axer
 */
public class CustomEDrtControlerCreator {

	public static Controler createControler(Config config, boolean otfvis) {
		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		DrtConfigs.adjustDrtConfig(drtCfg, config.planCalcScore());

		Scenario scenario = DrtControlerCreator.createScenarioWithDrtRouteFactory(config);
		ScenarioUtils.loadScenario(scenario);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new CustomEDrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new EvModule());
		controler.addOverridingModule(new EvDvrpIntegrationModule());
		controler.configureQSimComponents(EvDvrpIntegrationModule.activateModes(drtCfg.getMode()));

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		return controler;
	}
}
