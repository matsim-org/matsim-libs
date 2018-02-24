/* *********************************************************************** *
 * project: org.matsim.*
 * LanesIntegrationTest
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
package org.matsim.integration.invertednetworks;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.ActiveQSimBridge;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkLanesImpl;
import org.matsim.testcases.MatsimTestUtils;

import com.google.inject.Provider;

/**
 * Integration test for lanes and inverted network routing
 * @author dgrether
 *
 */
public class LanesIT {

	private static final Logger log = Logger.getLogger(LanesIT.class);

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void testLanes(){
		String configFilename = testUtils.getClassInputDirectory() + "config.xml";
		Config config = ConfigUtils.loadConfig(configFilename);
		config.network().setInputFile("network.xml");
		config.network().setLaneDefinitionsFile("testLaneDefinitions_v2.0.xml");
		config.plans().setInputFile("population.xml");
		config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "output");
		final int lastIteration = 50;
		config.controler().setLastIteration(lastIteration);
		config.controler().setCreateGraphs(false);
		config.vspExperimental().setWritingOutputEvents(false);
		Controler controler = new Controler(config);
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().to(TestListener.class);				
				addMobsimListenerBinding().toProvider(new Provider<MobsimInitializedListener>() {
					@Inject 
					ActiveQSimBridge activeQSimBridge;
					
					@Override
					public MobsimInitializedListener get() {
						return new MobsimInitializedListener() {
							@Override
							public void notifyMobsimInitialized(MobsimInitializedEvent e) {
								Assert.assertTrue(activeQSimBridge.getActiveQSim() instanceof QSim);
								QSim qsim = activeQSimBridge.getActiveQSim();
								NetsimLink link = qsim.getNetsimNetwork().getNetsimLink(Id.create("23", Link.class));
								Assert.assertTrue(link instanceof QLinkLanesImpl);
								QLinkLanesImpl link23 = (QLinkLanesImpl) link;
								Assert.assertNotNull(link23.getQueueLanes());
								//			for (QLane lane : link23.getQueueLanes()){
								//				Assert.assertNotNull(lane);
								//				log.error("lane " + lane.getId() + " flow " + lane.getSimulatedFlowCapacity());
								//				if (Id.create("1").equals(lane.getId())){
								//					log.error("lane 1 " + lane.getSimulatedFlowCapacity());
								//				}
								//				else if (Id.create("2").equals(lane.getId())){
								//					log.error("lane 2  " + lane.getSimulatedFlowCapacity());
								//				}
								//				else if (Id.create("3").equals(lane.getId())){
								//					log.error("lane 3 " + lane.getSimulatedFlowCapacity());
								//				}
								//			}
							}
						};
					}
				}).asEagerSingleton();
			}
		});
		controler.run();
	}

	private static class TestListener implements IterationEndsListener, ShutdownListener {

		private final TestHandler testHandler;

		@Inject
		TestListener(EventsManager eventsManager) {
			this.testHandler = new TestHandler(eventsManager);
		}

		double percent34;
		double percent35;
		double percent36;

		@Override
		public void notifyIterationEnds(IterationEndsEvent event) {
			log.error("Iteration: " + event.getIteration());
			percent34 = this.testHandler.getCount34() / 3600.0 * 100.0 ;
			percent35 = this.testHandler.getCount35() / 3600.0 * 100.0 ;
			percent36 = this.testHandler.getCount36() / 3600.0 * 100.0;
			log.error("Count 34: " + this.testHandler.getCount34() + " or " + percent34+ " %");
			log.error("Count 35: " + this.testHandler.getCount35() + " or " + percent35 + " %");
			log.error("Count 36: " + this.testHandler.getCount36() + " or " + percent36 + " %");
		}

		@Override
		public void notifyShutdown(ShutdownEvent event) {			
			log.info( "link34:" + percent34 );
			log.info( "link35:" + percent35 );
			log.info( "link36:" + percent36 );
			
			// The lanes attached to link 23 should distribute the 3600 veh/h capacity as follows:
			Assert.assertEquals("lane to link 34 should have approx. 600 veh/h, i.e. 16.6% of the total flow", 16.6, percent34, 1);
			Assert.assertEquals("lane to link 35 should have approx. 1200 veh/h, i.e. 33.3% of the total flow", 33.3, percent35, 1);
			Assert.assertEquals("lane to link 36 should have approx. 1800 veh/h, i.e. 50.0% of the total flow", 50.0, percent36, 1);
		}
	}


	private static class TestHandler implements LinkEnterEventHandler {

		private int count34 = 0;
		private int count35 = 0;
		private int count36 = 0;
		private Id<Link> id34 = Id.create("34", Link.class);
		private Id<Link> id35 = Id.create("35", Link.class);
		private Id<Link> id36 = Id.create("36", Link.class);

		public TestHandler(EventsManager eventsManager) {
			eventsManager.addHandler(this);
		}

		@Override
		public void handleEvent(LinkEnterEvent e) {
			if (e.getLinkId().equals(id34)){
				count34++;
			}
			else if (e.getLinkId().equals(id35)){
				count35++;
			}
			else if (e.getLinkId().equals(id36)){
				count36++;
			}
		}

		@Override
		public void reset(int iteration) {
			this.count34 = 0;
			this.count35 = 0;
			this.count36 = 0;
		}

		public int getCount34() {
			return count34;
		}

		public int getCount35() {
			return count35;
		}

		public int getCount36() {
			return count36;
		}


	}

}
