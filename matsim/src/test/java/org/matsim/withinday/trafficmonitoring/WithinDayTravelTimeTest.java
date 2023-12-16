/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayTravelTimeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmonitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.*;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author cdobler
 */
public class WithinDayTravelTimeTest {

	@RegisterExtension
	private MatsimTestUtils helper = new MatsimTestUtils();

	private Link link22;
	private double originalFreeSpeed22;

	@Test
	void testGetLinkTravelTime_fastCapacityUpdate() {
		testGetLinkTravelTime(true);
	}

	@Test
	void testGetLinkTravelTime_noFastCapacityUpdate() {
		testGetLinkTravelTime(false);
	}


	private void testGetLinkTravelTime(boolean isUsingFastCapacityUpdate) {

        Config config = ConfigUtils.loadConfig("test/scenarios/equil/config.xml");
		config.controller().setOutputDirectory(helper.getOutputDirectory()+"fastCapacityUpdate_"+isUsingFastCapacityUpdate);

		QSimConfigGroup qSimConfig = config.qsim();
		qSimConfig.setNumberOfThreads(2);
		qSimConfig.setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);

		config.controller().setLastIteration(0);

		config.controller().setCreateGraphs(false);
		config.controller().setDumpDataAtEnd(false);
		config.controller().setWriteEventsInterval(0);
		config.controller().setWritePlansInterval(0);

		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setRoutingAlgorithmType( ControllerConfigGroup.RoutingAlgorithmType.Dijkstra );

		config.network().setTimeVariantNetwork(true);

		// ---

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// ---

		// I in particular want to test if change events from before simulation start
		// are picked up (which they originally were not).  kai, dec'17
		Network network = scenario.getNetwork() ;

		link22 = network.getLinks().get(Id.createLinkId(22));
		originalFreeSpeed22 = link22.getFreespeed() ;
		Gbl.assertNotNull(link22);
		{
			NetworkChangeEvent event = new NetworkChangeEvent(5. * 3600.);
			ChangeValue freespeedChange = new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, 0.);
			event.setFreespeedChange(freespeedChange);
			NetworkUtils.addNetworkChangeEvent(network, event);
			event.addLink(link22);
		}
		{
			NetworkChangeEvent event = new NetworkChangeEvent(6. * 3600.);
			ChangeValue freespeedChange = new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, originalFreeSpeed22);
			event.setFreespeedChange(freespeedChange);
			NetworkUtils.addNetworkChangeEvent(network, event);
			event.addLink(link22);
		}

		final WithinDayTravelTime travelTime = new WithinDayTravelTime(scenario, null);
		final MobsimListener listener = new MobsimListenerForTests(scenario, travelTime);
		final FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();
		fosl.addSimulationListener(travelTime);
		fosl.addSimulationListener(listener);

		Controler controler = new Controler(scenario);
		controler.getEvents().addHandler(travelTime);
		controler.addControlerListener(new StartupListener() {
			@Override
			public void notifyStartup(StartupEvent event) {
				double t1 = 0.0;
				double t2 = 8.0;
				double t3 = 18.0;

				Id<Link> id = Id.create("6", Link.class);
				Link link = scenario.getNetwork().getLinks().get(id);
				link.setCapacity(500.0);	// reduce capacity

				// check free speed travel times - they should not be initialized yet
				assertEquals(Double.MAX_VALUE, travelTime.getLinkTravelTime(link, t1, null, null), 0);
				assertEquals(Double.MAX_VALUE, travelTime.getLinkTravelTime(link, t2, null, null), 0);
				assertEquals(Double.MAX_VALUE, travelTime.getLinkTravelTime(link, t3, null, null), 0);
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addMobsimListenerBinding().toInstance(fosl);
			}
		});

		controler.run();
	}

	/**
	 * Check travel times before and after a time step.
	 *
	 * @author cdobler
	 */
	private class MobsimListenerForTests implements MobsimInitializedListener, MobsimBeforeSimStepListener,
		MobsimAfterSimStepListener {

		private TravelTime travelTime;
		private final Link link ;
		private boolean isUsingFastCapacityUpdate;
		private int t1 = 6*3600;
		private int t2 = 6*3600 + 5*60;
		private int t3 = 6*3600 + 10*60;
		private int t4 = 6*3600 + 15*60;
		private int t5 = 6*3600 + 20*60;
		private int t6 = 6*3600 + 30*60;
		private int t7 = 6*3600 + 45*60;
		private int t8 = 7*3600;

		public MobsimListenerForTests(Scenario scenario, TravelTime travelTime) {
			this.travelTime = travelTime;
			this.isUsingFastCapacityUpdate = scenario.getConfig().qsim().isUsingFastCapacityUpdate();
			Id<Link> id = Id.create("6", Link.class);
			link = scenario.getNetwork().getLinks().get(id);
		}

		@Override
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			// check free speed travel times - they should be initialized now
			assertEquals(link.getLength()/link.getFreespeed(t1),
					travelTime.getLinkTravelTime(link, t1, null, null), 0);
			assertEquals(link.getLength()/link.getFreespeed(t2),
					travelTime.getLinkTravelTime(link, t2, null, null), 0);
			assertEquals(link.getLength()/link.getFreespeed(t3),
					travelTime.getLinkTravelTime(link, t3, null, null), 0);
			assertEquals(link.getLength()/link.getFreespeed(t4),
					travelTime.getLinkTravelTime(link, t4, null, null), 0);
			assertEquals(link.getLength()/link.getFreespeed(t5),
					travelTime.getLinkTravelTime(link, t5, null, null), 0);
			assertEquals(link.getLength()/link.getFreespeed(t6),
					travelTime.getLinkTravelTime(link, t6, null, null), 0);
			assertEquals(link.getLength()/link.getFreespeed(t7),
					travelTime.getLinkTravelTime(link, t7, null, null), 0);
			assertEquals(link.getLength()/link.getFreespeed(t8),
					travelTime.getLinkTravelTime(link, t8, null, null), 0);
		}

		@Override
		public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		    checkLinkTravelTimes(e.getSimulationTime());
		}

		@Override
		public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
            checkLinkTravelTimes(e.getSimulationTime());
		}

		private void checkLinkTravelTimes(double time) {
	        if (time == t1) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t1, null, null), 0);
			} else if (time == t2) {
				assertEquals(360.0, travelTime.getLinkTravelTime(link, t2, null, null), 0);
			} else if (time == t3) {
				assertEquals(467.6756756756757, travelTime.getLinkTravelTime(link, t3, null, null), 0);
			} else if (time == t4) {
				assertEquals(612.6282051282051, travelTime.getLinkTravelTime(link, t4, null, null), 0);
			} else if (time == t5) {
				assertEquals(690.62, travelTime.getLinkTravelTime(link, t5, null, null), 0);
			} else if (time == t6) {
				assertEquals(690.62, travelTime.getLinkTravelTime(link, t6, null, null), 0);
			} else if (time == t7) {
				assertEquals(967.1363636363636, travelTime.getLinkTravelTime(link, t7, null, null), 0);
			} else if (time == t8) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t8, null, null), 0);
			}
	        if ( time== 6*3600-1 ) {
				assertEquals(Double.POSITIVE_INFINITY,
						travelTime.getLinkTravelTime(link22,6*3600-1, null, null ), 0);
			} else if ( time==6*3600+1 ) {
				assertEquals(link22.getLength()/originalFreeSpeed22,
						travelTime.getLinkTravelTime(link22, 6 * 3600 - 1, null, null), 0);
			}
	    }
	}
}
