/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.taxibus.run.examples;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxibus.run.configuration.*;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.QSimConfigGroup.SnapshotStyle;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author jbischoff An example of a shared taxi system using DRT / taxibus infrastructure. A maximum of two passengers
 *         share a trip in this example.
 * 
 */
public class RunSharedTaxiExample {

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig("taxibus_example/configShared.xml", new TaxibusConfigGroup(),
				new DvrpConfigGroup());
		// set to "false", if you do not require OTFVis visualisation
		new RunSharedTaxiExample().run(config, true);

	}

	public void run(Config config, boolean otfvis) {
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setSnapshotStyle(SnapshotStyle.withHoles);

		OTFVisConfigGroup visConfig = ConfigUtils.addOrGetModule(config, OTFVisConfigGroup.GROUP_NAME,
				OTFVisConfigGroup.class);
		visConfig.setAgentSize(250);
		visConfig.setLinkWidth(5);
		visConfig.setDrawNonMovingItems(true);
		visConfig.setDrawTime(true);

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		new TaxibusControlerCreator(controler).initiateTaxibusses();

		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		controler.run();

	}

}
