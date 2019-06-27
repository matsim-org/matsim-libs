/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent.examples.random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dynagent.run.DynQSimConfigConsistencyChecker;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;
import org.matsim.vis.otfvis.OTFVisConfigGroup.ColoringScheme;

public class RunRandomDynAgentExample {
	public static void run(boolean otfvis) {
		String netFile = "./src/main/resources/grid_network.xml";
		run(netFile, otfvis);
	}

	public static void run(String netFile, boolean otfvis) {
		OTFVisConfigGroup otfvisConfig = new OTFVisConfigGroup();
		otfvisConfig.setColoringScheme(ColoringScheme.byId);
		otfvisConfig.setDrawNonMovingItems(true);

		Config config = ConfigUtils.createConfig(otfvisConfig);
		config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
		config.qsim().setSnapshotStyle(SnapshotStyle.queue);
		config.network().setInputFile(netFile);
		config.controler().setOutputDirectory("./test/output/");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setLastIteration(0);
		config.addConfigConsistencyChecker(new DynQSimConfigConsistencyChecker());

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingQSimModule(new DynQSimModule(RandomDynAgentSource.class));
		controler.configureQSimComponents(DynQSimModule::configureComponents);

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}

		controler.run();
	}

	public static void main(String[] args) {
		run(true);
	}
}
