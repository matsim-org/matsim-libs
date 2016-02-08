/* *********************************************************************** *
 * project: org.matsim.*
 * InvertedNetworksSignalsTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.router.InvertedNetworkRoutingModuleModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dgrether
 *
 */
public class InvertedNetworksSignalsIT {
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void testSignalsInvertedNetworkRouting() {
		InvertedNetworkRoutingSignalsFixture f = new InvertedNetworkRoutingSignalsFixture(false, false, true);
		f.scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		Controler c = new Controler(f.scenario);
		c.addOverridingModule(new SignalsModule());
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
	public final void testSignalsInvertedNetworkRoutingIterations() {
		InvertedNetworkRoutingSignalsFixture f = new InvertedNetworkRoutingSignalsFixture(false, false, true);
		f.scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		f.scenario.getConfig().controler().setLastIteration(1);
		SignalsData signalsData = (SignalsData) f.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalPlanData signalPlan = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(Id.create(2, SignalSystem.class)).getSignalPlanData().get(Id.create(1, SignalPlan.class));
		signalPlan.setCycleTime(500);
		signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create(2, SignalGroup.class)).setOnset(0);
		signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create(2, SignalGroup.class)).setDropping(5);
		SignalData sd = signalsData.getSignalSystemsData().getSignalSystemData().get(Id.create(2, SignalSystem.class)).getSignalData().get(Id.create(1, Signal.class));
		sd.addTurningMoveRestriction(Id.create(23, Link.class));
		Controler c = new Controler(f.scenario);
		c.addOverridingModule(new SignalsModule());
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
