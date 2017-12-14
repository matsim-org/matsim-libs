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

package org.matsim.roadpricing;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controller;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ModuleTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test(expected = RuntimeException.class)
    public void testControlerWithoutRoadPricingDoesntWork() {
        Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
        Controller controler = new Controller(config);
        controler.run();
        // config has a roadpricing config group, but controler does not know about
        // road pricing.
    }

    @Test
    public void testControlerWithRoadPricingWorks() {
        Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
        Controller controler = new Controller(config);
        controler.setModules(new ControlerDefaultsWithRoadPricingModule());
        controler.run();
    }

    @Test
    public void testControlerWithRoadPricingByScenarioWorks() {
        Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controller controler = new Controller(scenario);
        controler.setModules(new ControlerDefaultsWithRoadPricingModule());
        controler.run();
    }


    @Test
    public void testControlerWithRoadPricingByScenarioWorksTwice() {
        Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
        config.controler().setOutputDirectory(utils.getOutputDirectory()+"/1");
        Scenario scenario = ScenarioUtils.loadScenario(config);


        Controller controler1 = new Controller(scenario);
        controler1.setModules(new ControlerDefaultsWithRoadPricingModule());
        controler1.run();
        config.controler().setOutputDirectory(utils.getOutputDirectory()+"/2");
        Controller controler2 = new Controller(scenario);
        controler2.setModules(new ControlerDefaultsWithRoadPricingModule());
        controler2.run();
    }

}
