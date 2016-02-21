/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCollectorTest.java
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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author cdobler
 */
@RunWith(Parameterized.class)
public class TravelTimeCollectorTest extends MatsimTestCase {

	private final boolean isUsingFastCapacityUpdate ;
	
	public TravelTimeCollectorTest(boolean isUsingFastCapacityUpdate) {
		this.isUsingFastCapacityUpdate = isUsingFastCapacityUpdate;
	}
	
	@Parameters(name = "{index}: isUsingfastCapacityUpdate == {0}")
	public static Collection<Object> parameterObjects () {
		Object [] capacityUpdates = new Object [] { false, true };
		return Arrays.asList(capacityUpdates);
	}
	
	@Test
	public void testGetLinkTravelTime() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		QSimConfigGroup qSimConfig = config.qsim();
		qSimConfig.setNumberOfThreads(2);
		qSimConfig.setUsingFastCapacityUpdate(isUsingFastCapacityUpdate);
		config.controler().setLastIteration(0);

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final TravelTimeCollector travelTime = new TravelTimeCollector(scenario, null);
		MobsimListenerForTests listener = new MobsimListenerForTests(scenario, travelTime);
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
				assertEquals(Double.MAX_VALUE, travelTime.getLinkTravelTime(link, t1, null, null));
				assertEquals(Double.MAX_VALUE, travelTime.getLinkTravelTime(link, t2, null, null));
				assertEquals(Double.MAX_VALUE, travelTime.getLinkTravelTime(link, t3, null, null));
			}
		});
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addMobsimListenerBinding().toInstance(fosl);
			}
		});
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.getConfig().controler().setWriteEventsInterval(0);
		controler.getConfig().controler().setWritePlansInterval(0);
		
		// I do not know why but after injecting junit parameters, following is required else injector is not created and NPE is thrwon. amit Feb'16
		controler.getConfig().controler().setOutputDirectory("./output_fastCapacityUpdate_"+ this.isUsingFastCapacityUpdate+ "/");
		controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		
		controler.run();
	}

	/**
	 * Check travel times before and after a time step.
	 * 
	 * @author cdobler
	 */
	private static class MobsimListenerForTests implements MobsimInitializedListener, MobsimBeforeSimStepListener, 
		MobsimAfterSimStepListener {
		
		private TravelTime travelTime;
		private Link link = null;
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
			assertEquals(link.getLength()/link.getFreespeed(t1), travelTime.getLinkTravelTime(link, t1, null, null));
			assertEquals(link.getLength()/link.getFreespeed(t2), travelTime.getLinkTravelTime(link, t2, null, null));
			assertEquals(link.getLength()/link.getFreespeed(t3), travelTime.getLinkTravelTime(link, t3, null, null));
			assertEquals(link.getLength()/link.getFreespeed(t4), travelTime.getLinkTravelTime(link, t4, null, null));
			assertEquals(link.getLength()/link.getFreespeed(t5), travelTime.getLinkTravelTime(link, t5, null, null));
			assertEquals(link.getLength()/link.getFreespeed(t6), travelTime.getLinkTravelTime(link, t6, null, null));
			assertEquals(link.getLength()/link.getFreespeed(t7), travelTime.getLinkTravelTime(link, t7, null, null));
			assertEquals(link.getLength()/link.getFreespeed(t8), travelTime.getLinkTravelTime(link, t8, null, null));
		}

		@Override
		public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
//			System.out.println(travelTime.getLinkTravelTime(link, e.getSimulationTime()));
			if (e.getSimulationTime() == t1) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t1, null, null));
			} else if (e.getSimulationTime() == t2) {
				assertEquals(360.0, travelTime.getLinkTravelTime(link, t2, null, null));
			} else if (e.getSimulationTime() == t3) {
				if(this.isUsingFastCapacityUpdate) {
					assertEquals(467.97297297297297, travelTime.getLinkTravelTime(link, t3, null, null));
				} else {
					assertEquals(468.2162162162162, travelTime.getLinkTravelTime(link, t3, null, null));
				}
			} else if (e.getSimulationTime() == t4) {
//				assertEquals(616.4935064935065, travelTime.getLinkTravelTime(link, t4));
				if(this.isUsingFastCapacityUpdate) {
					assertEquals(612.9358974358975, travelTime.getLinkTravelTime(link, t4, null, null));
				} else {
					assertEquals(613.2051282051282, travelTime.getLinkTravelTime(link, t4, null, null));
				}
			} else if (e.getSimulationTime() == t5) {
//				assertEquals(822.1428571428571, travelTime.getLinkTravelTime(link, t5));
				if(this.isUsingFastCapacityUpdate) {
					assertEquals(690.92, travelTime.getLinkTravelTime(link, t5, null, null));
				} else {
					assertEquals(691.19, travelTime.getLinkTravelTime(link, t5, null, null));
				}
			} else if (e.getSimulationTime() == t6) {
//				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t6));
				if(this.isUsingFastCapacityUpdate) {
					assertEquals(690.92, travelTime.getLinkTravelTime(link, t6, null, null));
				} else {
					assertEquals(691.19, travelTime.getLinkTravelTime(link, t6, null, null));
				}
			} else if (e.getSimulationTime() == t7) {
//				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t7));
				if (this.isUsingFastCapacityUpdate) {
					assertEquals(967.4090909090909, travelTime.getLinkTravelTime(link, t7, null, null));
				} else {
					assertEquals(967.6818181818181, travelTime.getLinkTravelTime(link, t7, null, null));
				}
			} else if (e.getSimulationTime() == t8) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t8, null, null));
			}
		}

		@Override
		public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
			if (e.getSimulationTime() == t1) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t1, null, null));
			} else if (e.getSimulationTime() == t2) {
				assertEquals(360.0, travelTime.getLinkTravelTime(link, t2, null, null));
			} else if (e.getSimulationTime() == t3) {
				if(this.isUsingFastCapacityUpdate) {
					assertEquals(467.97297297297297, travelTime.getLinkTravelTime(link, t3, null, null));
				} else {
					assertEquals(468.2162162162162, travelTime.getLinkTravelTime(link, t3, null, null));
				}
			} else if (e.getSimulationTime() == t4) {
//				assertEquals(616.4935064935065, travelTime.getLinkTravelTime(link, t4));
				if(this.isUsingFastCapacityUpdate) {
					assertEquals(612.9358974358975, travelTime.getLinkTravelTime(link, t4, null, null));
				} else {
					assertEquals(613.2051282051282, travelTime.getLinkTravelTime(link, t4, null, null));
				}
			} else if (e.getSimulationTime() == t5) {
//				assertEquals(822.1428571428571, travelTime.getLinkTravelTime(link, t5));
				if(this.isUsingFastCapacityUpdate) {
					assertEquals(690.92, travelTime.getLinkTravelTime(link, t5, null, null));
				} else { 
					assertEquals(691.19, travelTime.getLinkTravelTime(link, t5, null, null));
				}
			} else if (e.getSimulationTime() == t6) {
//				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t6));
				if(this.isUsingFastCapacityUpdate) {
					assertEquals(690.92, travelTime.getLinkTravelTime(link, t6, null, null));
				} else { 
					assertEquals(691.19, travelTime.getLinkTravelTime(link, t6, null, null));
				}
			} else if (e.getSimulationTime() == t7) {
//				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t7));
				if(this.isUsingFastCapacityUpdate){
					assertEquals(967.4090909090909, travelTime.getLinkTravelTime(link, t7, null, null));
				} else {
					assertEquals(967.6818181818181, travelTime.getLinkTravelTime(link, t7, null, null));
				}
			} else if (e.getSimulationTime() == t8) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t8, null, null));
			}
		}
	}
	
}