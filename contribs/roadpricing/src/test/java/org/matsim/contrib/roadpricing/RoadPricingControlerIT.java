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

package org.matsim.contrib.roadpricing;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Tests the integration of the roadpricing-package into the Controler.
 *
 * @author mrieser
 */
public class RoadPricingControlerIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testPaidTollsEndUpInScores() {
		// first run basecase
		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil-extended"), "config.xml"));
		config.controller().setLastIteration(0);
		config.plans().setInputFile("plans1.xml");
		config.controller().setOutputDirectory(utils.getOutputDirectory() + "/basecase/");
		config.controller().setWritePlansInterval(0);
		Controler controler1 = new Controler(config);
		controler1.getConfig().controller().setCreateGraphs(false);
		controler1.getConfig().controller().setDumpDataAtEnd(false);
		controler1.getConfig().controller().setWriteEventsInterval(0);
		controler1.run();
		double scoreBasecase = controler1.getScenario().getPopulation().getPersons().get(Id.create("1", Person.class)).getPlans().get(0).getScore();

		// now run toll case
		//        ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setUseRoadpricing(true);
//		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile(IOUtils.newUrl(utils.inputResourcePath(), "distanceToll.xml").toString());
		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.GROUP_NAME, RoadPricingConfigGroup.class).setTollLinksFile( "distanceToll.xml" ) ;
		config.controller().setOutputDirectory(utils.getOutputDirectory() + "/tollcase/");
		Controler controler2 = new Controler(config);
		/* FIXME Check if the following is correct, jwj '19. What's the difference? */
//		controler2.setModules(new RoadPricingModuleDefaults());
		controler2.addOverridingModule( new RoadPricingModule() );
		controler2.getConfig().controller().setCreateGraphs(false);
		controler2.getConfig().controller().setDumpDataAtEnd(false);
		controler2.getConfig().controller().setWriteEventsInterval(0);
		controler2.run();
		double scoreTollcase = controler2.getScenario().getPopulation().getPersons().get(Id.create("1", Person.class)).getPlans().get(0).getScore();

		// there should be a score difference
		Assertions.assertEquals(3.0, scoreBasecase - scoreTollcase, MatsimTestUtils.EPSILON); // toll amount: 10000*0.00020 + 5000*0.00020
	}

}
