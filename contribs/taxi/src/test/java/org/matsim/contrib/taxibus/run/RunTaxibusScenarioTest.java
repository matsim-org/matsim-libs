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

package org.matsim.contrib.taxibus.run;

import org.junit.*;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.taxibus.run.configuration.TaxibusConfigGroup;
import org.matsim.contrib.taxibus.run.examples.*;
import org.matsim.core.config.*;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;

public class RunTaxibusScenarioTest {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunSharedTaxiExample() {
		String configFile = "./src/main/resources/taxibus_example/configShared.xml";
		Config config = ConfigUtils.loadConfig(configFile, new TaxibusConfigGroup(), new DvrpConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		new RunSharedTaxiExample().run(config, false);
	}

	@Test
	public void testRunTaxibusJspritExample() {
		String configFile = "./src/main/resources/taxibus_example/configClustered.xml";
		Config config = ConfigUtils.loadConfig(configFile, new TaxibusConfigGroup(), new DvrpConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		new RunTaxibusExample().run(config, false);
	}

	@Test
	public void testRunTaxibusClusteredExample() {
		String configFile = "./src/main/resources/taxibus_example/configJsprit.xml";
		Config config = ConfigUtils.loadConfig(configFile, new TaxibusConfigGroup(), new DvrpConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		new RunTaxibusExample().run(config, false);
	}
}
