/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.raptor;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.minibus.performance.raptor.RaptorTransitRouterProvider;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.testcases.MatsimTestUtils;

public class SameOriginDestinationRaptorTest {
	
	
	@Rule
	public MatsimTestUtils helper = new MatsimTestUtils();


	@Test
	public void sameFromAndToTransitStopTest() {
		String config = "test/input/org/matsim/contrib/minibus/example-scenario/raptorFixMinimalExample/config_raptor.xml";
		
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(config));
		scenario.getConfig().controler().setLastIteration(1); // more iterations are not required to check this.
		scenario.getConfig().controler().setOutputDirectory(helper.getOutputDirectory());
		
		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(TransitRouter.class).toProvider(RaptorTransitRouterProvider.class);
			}
		});
		controler.run();
	}
}
