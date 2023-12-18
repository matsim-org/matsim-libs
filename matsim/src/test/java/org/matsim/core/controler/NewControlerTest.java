
/* *********************************************************************** *
 * project: org.matsim.*
 * NewControlerTest.java
 *                                                                         *
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

 package org.matsim.core.controler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

 public class NewControlerTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	 @Test
	 void testInjectionBeforeControler() {
		Config config = testUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));

		// a scenario is created and none of the files are loaded;
		// facility file is provided in config and facilitySource is 'fromFile', the facilitySource must be changed. Amit Jan'18
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.none);

		config.controller().setLastIteration(1);
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());
		final Scenario scenario = ScenarioUtils.createScenario(config);
		com.google.inject.Injector injector = Injector.createInjector(config, new AbstractModule() {
			@Override
			public void install() {
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
			}
		});
		ControlerI controler = injector.getInstance(ControlerI.class);
		controler.run();
	}

}
