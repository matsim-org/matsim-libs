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

package taxibus.run;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;

import taxibus.run.configuration.TaxibusConfigGroup;
import taxibus.run.examples.RunTaxibusExample;

public class RunTaxibusScenarioIT {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Ignore
	@Test
	public void testRunTaxibusJspritExample() {
		String configFile = "./src/main/resources/taxibus_example/configClustered.xml";
		Config config = ConfigUtils.loadConfig(configFile, new TaxibusConfigGroup(), new DvrpConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		new RunTaxibusExample().run(config, false);
	}

	@Ignore
	@Test
	public void testRunTaxibusClusteredExample() {
		String configFile = "./src/main/resources/taxibus_example/configJsprit.xml";
		Config config = ConfigUtils.loadConfig(configFile, new TaxibusConfigGroup(), new DvrpConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		new RunTaxibusExample().run(config, false);
	}
}
