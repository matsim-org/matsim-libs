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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.MatsimTestUtils;

public class ModuleTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test(expected = RuntimeException.class)
    @Ignore
    public void testControlerWithoutRoadPricingDoesntWork() {
        Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
        Controler controler = new Controler(config);
        controler.run();
        // config has a roadpricing config group, but controler does not know about
        // road pricing.
    }

    @Test
    public void testControlerWithRoadPricingWorks() {
        Config config = utils.loadConfig(utils.getClassInputDirectory() + "/config.xml");
        Controler controler = new Controler(config);
        controler.setModules(new ControlerDefaultsWithRoadPricingModule());
        controler.run();
    }

}
