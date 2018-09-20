/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package org.matsim.contrib.av.intermodal;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.intermodal.router.VariableAccessTransitRouterModule;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessModeConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.examples.TaxiDvrpModules;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author jbischoff
 */

/**
 *
 */
public class RunTaxiPTIntermodalExample {
	public static void main(String[] args) {
		new RunTaxiPTIntermodalExample().run(false);
	}

	public void run(boolean OTFVis) {
		Config config = ConfigUtils.loadConfig("intermodal/config.xml", new TaxiConfigGroup(), new DvrpConfigGroup());

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		// yyyy Could you please javadoc the following? EmissionsConfigGroup has an example how the explanatory strings
		// can be kept consistent between config file dump and javadoc. Thx. kai, jan'17
		VariableAccessConfigGroup vacfg = new VariableAccessConfigGroup();
		{
			VariableAccessModeConfigGroup taxi = new VariableAccessModeConfigGroup();
			taxi.setDistance(20000);
			taxi.setTeleported(false);
			taxi.setMode("taxi");
			vacfg.setAccessModeGroup(taxi);
		}
		{
			VariableAccessModeConfigGroup walk = new VariableAccessModeConfigGroup();
			walk.setDistance(1000);
			walk.setTeleported(true);
			walk.setMode("walk");
			vacfg.setAccessModeGroup(walk);
		}
		config.addModule(vacfg);

		config.transitRouter().setSearchRadius(15000);
		config.transitRouter().setExtensionRadius(0);

		OTFVisConfigGroup otfvis = new OTFVisConfigGroup();
		otfvis.setDrawNonMovingItems(true);
		config.addModule(otfvis);

		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();

		String mode = TaxiConfigGroup.get(config).getMode();

		// ---
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(TaxiDvrpModules.create(mode));

		controler.addOverridingModule(new TaxiModule());

		controler.addOverridingModule(new VariableAccessTransitRouterModule());
		if (OTFVis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		controler.run();
	}
}
