/* *********************************************************************** *
 * project: org.matsim.*
 * InvertedNetworkRoutingTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.integration.invertednetworks;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.testcases.MatsimTestUtils;

public class InvertedNetworkRoutingIT {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	final void testLanesInvertedNetworkRouting() {
		InvertedNetworkRoutingTestFixture f = new InvertedNetworkRoutingTestFixture(false, true, false);
		f.scenario.getConfig().controller().setOutputDirectory(testUtils.getOutputDirectory());
		f.scenario.getConfig().travelTimeCalculator().setSeparateModes( false );
		Controler c = new Controler(f.scenario);
		c.getConfig().controller().setDumpDataAtEnd(false);
		c.getConfig().controller().setCreateGraphs(false);
		final InvertedNetworkRoutingTestEventHandler testHandler = new InvertedNetworkRoutingTestEventHandler();
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(testHandler);
			}
		});
		c.run();
		Assertions.assertTrue(testHandler.hadTrafficOnLink25, "No traffic on link");
	}


	@Test
	final void testModesInvertedNetworkRouting() {
		InvertedNetworkRoutingTestFixture f = new InvertedNetworkRoutingTestFixture(true, false, false);
		f.scenario.getConfig().controller().setOutputDirectory(testUtils.getOutputDirectory());
		f.scenario.getConfig().travelTimeCalculator().setSeparateModes( false );
		Controler c = new Controler(f.scenario);
		c.getConfig().controller().setDumpDataAtEnd(false);
		c.getConfig().controller().setCreateGraphs(false);
		final InvertedNetworkRoutingTestEventHandler testHandler = new InvertedNetworkRoutingTestEventHandler();
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(testHandler);
			}
		});
		c.run();
		Assertions.assertTrue(testHandler.hadTrafficOnLink25, "No traffic on link");
	}

	@Test
	final void testModesNotInvertedNetworkRouting() {
		InvertedNetworkRoutingTestFixture f = new InvertedNetworkRoutingTestFixture(true, false, false);
		f.scenario.getConfig().controller().setOutputDirectory(testUtils.getOutputDirectory());
		f.scenario.getConfig().controller().setLinkToLinkRoutingEnabled(false);
		f.scenario.getConfig().travelTimeCalculator().setCalculateLinkToLinkTravelTimes(false);
		Controler c = new Controler(f.scenario);
		//c.addOverridingModule(new InvertedNetworkRoutingGuiceModule());
		c.getConfig().controller().setDumpDataAtEnd(false);
		c.getConfig().controller().setCreateGraphs(false);
		final InvertedNetworkRoutingTestEventHandler testHandler = new InvertedNetworkRoutingTestEventHandler();
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(testHandler);
			}
		});
		c.run();
		Assertions.assertTrue(testHandler.hadTrafficOnLink25, "No traffic on link");
	}
}
