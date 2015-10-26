/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * ExampleWithinDayControllerTest.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2015 by the members listed in the COPYING, *
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

package org.matsim.withinday.controller;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

public class ExampleWithinDayControllerTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public void testRun() {
        Config config = utils.loadConfig("test/scenarios/equil/config.xml");
        config.controler().setLastIteration(1);
        Controler controler = new Controler(config);
        ExampleWithinDayController.configure(controler);
        controler.run();
    }

}
