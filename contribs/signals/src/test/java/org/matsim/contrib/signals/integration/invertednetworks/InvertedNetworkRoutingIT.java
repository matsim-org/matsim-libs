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
package org.matsim.contrib.signals.integration.invertednetworks;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.signals.router.InvertedNetworkRoutingModuleModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.testcases.MatsimTestUtils;

public class InvertedNetworkRoutingIT {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void testLanesInvertedNetworkRouting() {
		InvertedNetworkRoutingTestFixture f = new InvertedNetworkRoutingTestFixture(false, true, false);
		f.scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		Controler c = new Controler(f.scenario);
		c.addOverridingModule(new InvertedNetworkRoutingModuleModule());
		c.getConfig().controler().setDumpDataAtEnd(false);
		c.getConfig().controler().setCreateGraphs(false);
        final InvertedNetworkRoutingTestEventHandler testHandler = new InvertedNetworkRoutingTestEventHandler();
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(testHandler);
			}
		});
		c.run();
		Assert.assertTrue("No traffic on link", testHandler.hadTrafficOnLink25);
	}


	@Test
	public final void testModesInvertedNetworkRouting() {
		InvertedNetworkRoutingTestFixture f = new InvertedNetworkRoutingTestFixture(true, false, false);
		f.scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		Controler c = new Controler(f.scenario);
		c.addOverridingModule(new InvertedNetworkRoutingModuleModule());
		c.getConfig().controler().setDumpDataAtEnd(false);
		c.getConfig().controler().setCreateGraphs(false);
        final InvertedNetworkRoutingTestEventHandler testHandler = new InvertedNetworkRoutingTestEventHandler();
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(testHandler);
			}
		});
		c.run();
		Assert.assertTrue("No traffic on link", testHandler.hadTrafficOnLink25);
	}

	@Test
	public final void testModesNotInvertedNetworkRouting() {
		InvertedNetworkRoutingTestFixture f = new InvertedNetworkRoutingTestFixture(true, false, false);
		f.scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		f.scenario.getConfig().controler().setLinkToLinkRoutingEnabled(false);
		f.scenario.getConfig().travelTimeCalculator().setCalculateLinkToLinkTravelTimes(false);
		Controler c = new Controler(f.scenario);
		c.addOverridingModule(new InvertedNetworkRoutingModuleModule());
		c.getConfig().controler().setDumpDataAtEnd(false);
		c.getConfig().controler().setCreateGraphs(false);
        final InvertedNetworkRoutingTestEventHandler testHandler = new InvertedNetworkRoutingTestEventHandler();
		c.addControlerListener(new StartupListener(){
			@Override
			public void notifyStartup(StartupEvent event) {
				event.getServices().getEvents().addHandler(testHandler);
			}
		});
		c.run();
		Assert.assertTrue("No traffic on link", testHandler.hadTrafficOnLink25);
	}
}
