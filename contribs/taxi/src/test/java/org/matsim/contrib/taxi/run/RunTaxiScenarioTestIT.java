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

package org.matsim.contrib.taxi.run;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunTaxiScenarioTestIT {
	@RegisterExtension
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRunMielecLowDemandLowSupply() {
		runMielec("plans_taxi_1.0.xml.gz", "taxis-25.xml");
	}

	@Test
	void testRunMielecHighDemandLowSupply() {
		runMielec("plans_taxi_4.0.xml.gz", "taxis-25.xml");
	}

	private void runMielec(String plansFile, String taxisFile) {
		URL configUrl = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_taxi_config.xml");
		Config config = ConfigUtils.loadConfig(configUrl, new MultiModeTaxiConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.plans().setInputFile(plansFile);
		TaxiConfigGroup.getSingleModeTaxiConfig(config).taxisFile = taxisFile;
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.controller().setDumpDataAtEnd(false);
		TaxiControlerCreator.createControler(config, false).run();
	}
}
