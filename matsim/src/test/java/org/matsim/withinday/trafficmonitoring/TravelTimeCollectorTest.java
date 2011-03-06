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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.FixedOrderSimulationListener;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationInitializedListener;
import org.matsim.core.router.util.TravelTime;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.withinday.network.WithinDayLinkFactoryImpl;

public class TravelTimeCollectorTest extends MatsimTestCase {

	/**
	 * @author cdobler
	 */
	public void testGetLinkTravelTime() {
		
		// load config and use ParallelQSim with 2 Threads
		Config config = loadConfig("test/scenarios/equil/config.xml");
		QSimConfigGroup qSimConfig = new QSimConfigGroup();
		qSimConfig.setNumberOfThreads(2);
		config.addQSimConfigGroup(qSimConfig);
		config.controler().setLastIteration(0);
		
		Controler controler = new Controler(config);
		controler.getNetwork().getFactory().setLinkFactory(new WithinDayLinkFactoryImpl());
		ControlerListenerForTests listener = new ControlerListenerForTests();
		controler.addControlerListener(listener);
		controler.run();
	}
	
	/**
	 * A ControllerListener that creates and registers a TravelTimeCollector
	 * and a MobsimListenerForTests which executes the test cases.
	 * 
	 * @author cdobler
	 */
	private static class ControlerListenerForTests implements StartupListener {
		
		private double t1 = 0.0;
		private double t2 = 8.0;
		private double t3 = 18.0;
		
		@Override
		public void notifyStartup(StartupEvent event) {
			Controler controler = event.getControler();
			Scenario scenario = controler.getScenario();
			EventsManager eventsManager = controler.getEvents();
			
			TravelTimeCollector travelTime = new TravelTimeCollectorFactory().createTravelTimeCollector(scenario);
			MobsimListenerForTests listener = new MobsimListenerForTests(scenario, travelTime);
			eventsManager.addHandler(travelTime);
			FixedOrderSimulationListener fosl = new FixedOrderSimulationListener();
			fosl.addSimulationListener(travelTime);
			fosl.addSimulationListener(listener);
			controler.getQueueSimulationListener().add(fosl);
			
			Id id = scenario.createId("6");
			Link link = scenario.getNetwork().getLinks().get(id);
			link.setCapacity(500.0);	// reduce capacity
			
			// check free speed travel times - they should not be initialized yet
			assertEquals(0.0, travelTime.getLinkTravelTime(link, t1));
			assertEquals(0.0, travelTime.getLinkTravelTime(link, t2));
			assertEquals(0.0, travelTime.getLinkTravelTime(link, t3));
		}
	}
	
	/**
	 * Check travel times before and after a time step.
	 * 
	 * @author cdobler
	 */
	private static class MobsimListenerForTests implements SimulationInitializedListener, SimulationBeforeSimStepListener, 
		SimulationAfterSimStepListener {
		
		private TravelTime travelTime;
		private Link link = null;
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
			
			Id id = scenario.createId("6");
			link = scenario.getNetwork().getLinks().get(id);
		}
		
		@Override
		public void notifySimulationInitialized(SimulationInitializedEvent e) {
			// check free speed travel times - they should be initialized now
			assertEquals(link.getLength()/link.getFreespeed(t1), travelTime.getLinkTravelTime(link, t1));
			assertEquals(link.getLength()/link.getFreespeed(t2), travelTime.getLinkTravelTime(link, t2));
			assertEquals(link.getLength()/link.getFreespeed(t3), travelTime.getLinkTravelTime(link, t3));
			assertEquals(link.getLength()/link.getFreespeed(t4), travelTime.getLinkTravelTime(link, t4));
			assertEquals(link.getLength()/link.getFreespeed(t5), travelTime.getLinkTravelTime(link, t5));
			assertEquals(link.getLength()/link.getFreespeed(t6), travelTime.getLinkTravelTime(link, t6));
			assertEquals(link.getLength()/link.getFreespeed(t7), travelTime.getLinkTravelTime(link, t7));
			assertEquals(link.getLength()/link.getFreespeed(t8), travelTime.getLinkTravelTime(link, t8));
		}

		@Override
		public void notifySimulationBeforeSimStep(SimulationBeforeSimStepEvent e) {
//			System.out.println(travelTime.getLinkTravelTime(link, e.getSimulationTime()));
			if (e.getSimulationTime() == t1) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t1));
			} else if (e.getSimulationTime() == t2) {
				assertEquals(360.0, travelTime.getLinkTravelTime(link, t2));
			} else if (e.getSimulationTime() == t3) {
				assertEquals(468.2162162162162, travelTime.getLinkTravelTime(link, t3));
			} else if (e.getSimulationTime() == t4) {
				assertEquals(616.4935064935065, travelTime.getLinkTravelTime(link, t4));
			} else if (e.getSimulationTime() == t5) {
				assertEquals(822.1428571428571, travelTime.getLinkTravelTime(link, t5));
			} else if (e.getSimulationTime() == t6) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t6));
			} else if (e.getSimulationTime() == t7) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t7));
			} else if (e.getSimulationTime() == t8) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t8));
			}
		}

		@Override
		public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
			if (e.getSimulationTime() == t1) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t1));
			} else if (e.getSimulationTime() == t2) {
				assertEquals(360.0, travelTime.getLinkTravelTime(link, t2));
			} else if (e.getSimulationTime() == t3) {
				assertEquals(468.2162162162162, travelTime.getLinkTravelTime(link, t3));
			} else if (e.getSimulationTime() == t4) {
				assertEquals(616.4935064935065, travelTime.getLinkTravelTime(link, t4));
			} else if (e.getSimulationTime() == t5) {
				assertEquals(822.1428571428571, travelTime.getLinkTravelTime(link, t5));
			} else if (e.getSimulationTime() == t6) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t6));
			} else if (e.getSimulationTime() == t7) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t7));
			} else if (e.getSimulationTime() == t8) {
				assertEquals(359.9712023038157, travelTime.getLinkTravelTime(link, t8));
			}
		}
	}
	
}