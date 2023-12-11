/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.roadpricing;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 *
 * Tests the integration of the roadpricing-package into the Controler.
 *
 * Compares a base case (no pricing) against a policy case (link-based pricing)
 * and tests if all agents adjust their routes in order to avoid the toll payments.
 *
 * @author ikaddoura
 *
 */
public class AvoidTolledRouteTest {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	final void test1(){

		String configFile = testUtils.getClassInputDirectory() + "/config.xml";

		// baseCase
		Config config1 = ConfigUtils.loadConfig( configFile, RoadPricingUtils.createConfigGroup());
		config1.controller().setOutputDirectory(testUtils.getOutputDirectory() + "bc");

		Scenario scenario1 = ScenarioUtils.loadScenario(config1);
		Controler controler1 = new Controler(scenario1);

		final Map<Id<Link>,Integer> linkId2demand1 = new HashMap<>();
		controler1.getEvents().addHandler(new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {
				linkId2demand1.clear();
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				if (linkId2demand1.containsKey(event.getLinkId())) {
					int agents = linkId2demand1.get(event.getLinkId());
					linkId2demand1.put(event.getLinkId(), agents + 1);

				} else {
					linkId2demand1.put(event.getLinkId(), 1);
				}
			}
		});

		controler1.getConfig().controller().setCreateGraphs(false);
		controler1.run();

		// link-based toll
		Config config2 = ConfigUtils.loadConfig( configFile, RoadPricingUtils.createConfigGroup());
		config2.controller().setOutputDirectory(testUtils.getOutputDirectory() + "cordon");

		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		Controler controler2 = new Controler(scenario2);
		controler2.addOverridingModule(new RoadPricingModule());

		final Map<Id<Link>,Integer> linkId2demand2 = new HashMap<Id<Link>, Integer>();
		controler2.getEvents().addHandler(new LinkLeaveEventHandler() {

			@Override
			public void reset(int iteration) {
				linkId2demand2.clear();
			}

			@Override
			public void handleEvent(LinkLeaveEvent event) {
				if (linkId2demand2.containsKey(event.getLinkId())) {
					int agents = linkId2demand2.get(event.getLinkId());
					linkId2demand2.put(event.getLinkId(), agents + 1);

				} else {
					linkId2demand2.put(event.getLinkId(), 1);
				}
			}
		});

		controler2.getConfig().controller().setCreateGraphs(false);
		controler2.run();

		Assertions.assertEquals(3, (Integer) linkId2demand1.get(Id.createLinkId("link_4_5")), 0, "Base Case: all agents should use the faster route.");
		Assertions.assertEquals(3, linkId2demand2.get(Id.createLinkId("link_1_2")), 0, "Pricing: all agents should use the slow but untolled route.");

	}
}


