/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
package org.matsim.contrib.roadpricing.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.roadpricing.RoadPricingConfigGroup;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class RunRoadPricingFromCode {
	private static final String TEST_CONFIG = "./contribs/roadpricing/test/input/org/matsim/contrib/roadpricing/AvoidTolledRouteTest/config.xml";

	public static void main(String[] args) { run(); }


	static void run(){
		/* Start with a known config file (with population, network, and scoring
		parameteres specified) and just remove the road pricing file. */
//		Config config = ConfigUtils.loadConfig(TEST_CONFIG, RoadPricingUtils.createConfigGroup());
//		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class).setTollLinksFile(null);
//		Config config = ConfigUtils.createConfig(RoadPricingUtils.createConfigGroup());
		Config config = ConfigUtils.createConfig();
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

		Scenario sc = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(sc);
		controler.run();
	}
}
