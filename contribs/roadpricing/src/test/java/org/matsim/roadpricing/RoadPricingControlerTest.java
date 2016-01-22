/* *********************************************************************** *
 * project: org.matsim.*
 * RoadPricingControlerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.roadpricing;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests the integration of the roadpricing-package into the Controler.
 *
 * @author mrieser
 */
public class RoadPricingControlerTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
	public void testPaidTollsEndUpInScores() {
		// first run basecase
		Config config = utils.loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(0);
		config.plans().setInputFile("test/scenarios/equil/plans1.xml");
		config.controler().setOutputDirectory(utils.getOutputDirectory() + "/basecase/");
		config.controler().setWritePlansInterval(0);
		Controler controler1 = new Controler(config);
        controler1.getConfig().controler().setCreateGraphs(false);
		controler1.getConfig().controler().setDumpDataAtEnd(false);
		controler1.getConfig().controler().setWriteEventsInterval(0);
		controler1.run();
        double scoreBasecase = controler1.getScenario().getPopulation().getPersons().get(Id.create("1", Person.class)).getPlans().get(0).getScore();

		// now run toll case
//        ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setUseRoadpricing(true);
        ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(utils.getInputDirectory() + "distanceToll.xml");
		config.controler().setOutputDirectory(utils.getOutputDirectory() + "/tollcase/");
		Controler controler2 = new Controler(config);
        controler2.setModules(new ControlerDefaultsWithRoadPricingModule());
        controler2.getConfig().controler().setCreateGraphs(false);
		controler2.getConfig().controler().setDumpDataAtEnd(false);
		controler2.getConfig().controler().setWriteEventsInterval(0);
		controler2.run();
        double scoreTollcase = controler2.getScenario().getPopulation().getPersons().get(Id.create("1", Person.class)).getPlans().get(0).getScore();

		// there should be a score difference
		Assert.assertEquals(3.0, scoreBasecase - scoreTollcase, MatsimTestUtils.EPSILON); // toll amount: 10000*0.00020 + 5000*0.00020
	}

}
