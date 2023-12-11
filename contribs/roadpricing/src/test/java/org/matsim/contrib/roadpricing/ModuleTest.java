/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ModuleTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.contrib.roadpricing;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ModuleTest {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testControlerWithoutRoadPricingDoesntWork() {
		assertThrows(RuntimeException.class, () -> {
			Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
			Controler controler = new Controler(config);
			controler.run();
			// config has a roadpricing config group, but controler does not know about
			// road pricing.
		});
		// config has a roadpricing config group, but controler does not know about
		// road pricing.
	}

	@Test
	void testControlerWithRoadPricingWorks() {
        Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
        Controler controler = new Controler(config);
//        controler.setModules(new RoadPricingModuleDefaults());
        controler.addOverridingModule(new RoadPricingModule());
        controler.run();
    }

	@Test
	void testControlerWithRoadPricingByScenarioWorks() {
        Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
	    controler.addOverridingModule( new RoadPricingModule() );
//        controler.setModules(new RoadPricingModuleDefaults());
        controler.run();
    }


	@Test
	void testControlerWithRoadPricingByScenarioWorksTwice() {
        Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
        config.controller().setOutputDirectory(utils.getOutputDirectory()+"/1");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Controler controler1 = new Controler(scenario);
//        controler1.setModules(new RoadPricingModuleDefaults());
	    controler1.addOverridingModule( new RoadPricingModule() );
        controler1.run();
        config.controller().setOutputDirectory(utils.getOutputDirectory()+"/2");
        Controler controler2 = new Controler(scenario);
	    controler2.addOverridingModule( new RoadPricingModule() );
//        controler2.setModules(new RoadPricingModuleDefaults());
        controler2.run();
    }

}
