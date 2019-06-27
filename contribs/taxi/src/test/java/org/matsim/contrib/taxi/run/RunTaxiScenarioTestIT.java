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
import java.util.Collections;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class RunTaxiScenarioTestIT {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunMielecLowDemandLowSupply() {
		runMielec("plans_taxi_1.0.xml.gz", "taxis-25.xml");
	}

	@Test
	public void testRunMielecHighDemandLowSupply() {
		runMielec("plans_taxi_4.0.xml.gz", "taxis-25.xml");
	}

	@Test
	public void testRunWithRejection() {
		runMielecWithRejection("plans_taxi_4.0.xml.gz", "taxis-25.xml");
	}

	private void runMielec(String plansFile, String taxisFile) {
		URL configUrl = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_taxi_config.xml");
		TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
		Config config = ConfigUtils.loadConfig(configUrl, taxiCfg, new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.plans().setInputFile(plansFile);
		taxiCfg.setTaxisFile(taxisFile);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setDumpDataAtEnd(false);
		TaxiControlerCreator.createControlerWithSingleModeDrt(config, false).run();
	}

	private void runMielecWithRejection(String plansFile, String taxisFile) {
		URL configUrl = IOUtils.newUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_taxi_config.xml");
		TaxiConfigGroup taxiCfg = new TaxiConfigGroup();
		Config config = ConfigUtils.loadConfig(configUrl, taxiCfg, new DvrpConfigGroup(), new OTFVisConfigGroup());
		config.plans().setInputFile(plansFile);
		taxiCfg.setTaxisFile(taxisFile);
		taxiCfg.setBreakSimulationIfNotAllRequestsServed(false);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setDumpDataAtEnd(false);
		config.qsim().setEndTime(36. * 3600);
		Controler controler = TaxiControlerCreator.createControlerWithSingleModeDrt(config, false);

		controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(taxiCfg.getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PassengerRequestValidator.class).toInstance(
						req -> req.getPassengerId().toString().equals("0000009") ?
								Collections.singleton("REJECT_0000009") :
								Collections.emptySet());
			}
		});

		controler.run();

		Assert.assertEquals(-476.003472470419, controler.getScenario()
				.getPopulation()
				.getPersons()
				.get(Id.createPersonId("0000009"))
				.getSelectedPlan()
				.getScore(), utils.EPSILON);
	}
}
