/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.drt.run.example;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.data.validator.DrtRequestValidator;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.examples.RunDrtExample;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

/**
 * @author jbischoff
 */
public class RunDrtExampleIT {
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRunDrtExample() {
		String configFile = "./src/main/resources/drt_example/drtconfig_door2door.xml";
		Config config = ConfigUtils.loadConfig(configFile, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.plans().setInputFile("cb-drtplans_test.xml.gz");
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);
	}
	
	@Test
	public void testRunDrtExampleWithRejection() {
		String configFile = "./src/main/resources/drt_example/drtconfig_door2door.xml";
		Config config = ConfigUtils.loadConfig(configFile, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.plans().setInputFile("cb-drtplans_test.xml.gz");
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		
		DrtControlerCreator.adjustDrtConfig(config);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		
		Controler controler = new Controler(scenario);
		DrtControlerCreator.addDrtToControler(controler);
		controler.addOverridingModule(new AbstractModule() {	
			@Override
			public void install() {
				
				this.bind(DrtRequestValidator.class).toInstance(new DrtRequestValidator() {
					
					@Override
					public boolean validateDrtRequest(DrtRequest request) {
						if (request.getPassenger().getId().toString().equals("12052000_12052000_100")) {
							return false;
						} else {
							return true;
						}
					}
				});
			}
		});
		
		controler.run();
	}
	
	@Test
	public void testRunDrtStopbasedExample() {
		String configFile = "./src/main/resources/drt_example/drtconfig_stopbased.xml";
		Config config = ConfigUtils.loadConfig(configFile, new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
		config.plans().setInputFile("cb-drtplans_test.xml.gz");
		
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		RunDrtExample.run(config, false);
	}
}
