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

package org.matsim.contrib.edrt.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.optimizer.depot.DepotFinder;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.contrib.edrt.optimizer.depot.NearestChargerAsDepot;

/**
 * @author michalm
 */
public class EDrtControlerCreator {

	public static Controler createControler(Config config, boolean otfvis) {
		DrtConfigs.adjustDrtConfig(DrtConfigGroup.get(config), config.planCalcScore());
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
		DrtConfigGroup drtCfg = DrtConfigGroup.get(controler.getConfig());
		controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new EDrtModule());
		controler.addOverridingModule(new AbstractDvrpModeModule(drtCfg.getMode()) {
			@Override
			public void install() {
				bindModal(DepotFinder.class).to(NearestChargerAsDepot.class);
			}
		});
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(drtCfg.getMode()));
	}
}
