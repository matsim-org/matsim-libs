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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalcontrol.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalsystems.v20.SignalData;
import org.matsim.contrib.signals.model.Signal;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.integration.invertednetworks.InvertedNetworkRoutingTestEventHandler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author dgrether
 *
 */
public class InvertedNetworksSignalsIT {
	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	final void testSignalsInvertedNetworkRouting() {
		InvertedNetworkRoutingSignalsFixture f = new InvertedNetworkRoutingSignalsFixture(false, false, true);
		f.scenario.getConfig().controller().setOutputDirectory(testUtils.getOutputDirectory());
		Controler c = new Controler(f.scenario);
//		c.addOverridingModule(new SignalsModule());
		Signals.configure(c);
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
	final void testSignalsInvertedNetworkRoutingIterations() {
		InvertedNetworkRoutingSignalsFixture f = new InvertedNetworkRoutingSignalsFixture(false, false, true);
		f.scenario.getConfig().controller().setOutputDirectory(testUtils.getOutputDirectory());
		f.scenario.getConfig().controller().setLastIteration(1);
		SignalsData signalsData = (SignalsData) f.scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalPlanData signalPlan = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(Id.create(2, SignalSystem.class)).getSignalPlanData().get(Id.create(1, SignalPlan.class));
		signalPlan.setCycleTime(500);
		signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create(2, SignalGroup.class)).setOnset(0);
		signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create(2, SignalGroup.class)).setDropping(5);
		SignalData sd = signalsData.getSignalSystemsData().getSignalSystemData().get(Id.create(2, SignalSystem.class)).getSignalData().get(Id.create(1, Signal.class));
		sd.addTurningMoveRestriction(Id.create(23, Link.class));
		Controler c = new Controler(f.scenario);
//		c.addOverridingModule(new SignalsModule());
		Signals.configure( c );
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
