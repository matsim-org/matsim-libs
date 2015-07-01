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
package org.matsim.contrib.signals.integration.lanes;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.signals.router.InvertedNetworkTripRouterFactoryModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.qnetsimengine.NetsimLink;
import org.matsim.core.mobsim.qsim.qnetsimengine.QLinkLanesImpl;
import org.matsim.lanes.data.v11.LaneDefinitonsV11ToV20Converter;
import org.matsim.testcases.MatsimTestUtils;



/**
 * Integration test for lanes and inverted network routing
 * @author dgrether
 *
 */
public class LanesIntegrationTest {
	
	private static final Logger log = Logger.getLogger(LanesIntegrationTest.class);
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public void testLanes(){
		String configFilename = testUtils.getClassInputDirectory() + "config.xml";
		Config config = ConfigUtils.loadConfig(configFilename);
		config.network().setInputFile(testUtils.getClassInputDirectory() + "network.xml");
		String lanes11 = testUtils.getClassInputDirectory() + "lanes.xml";
		String lanes20 = testUtils.getOutputDirectory() + "testLaneDefinitions_v2.0.xml";
		new LaneDefinitonsV11ToV20Converter().convert(lanes11, lanes20, config.network().getInputFile());
		config.network().setLaneDefinitionsFile(lanes20);
		config.plans().setInputFile(testUtils.getClassInputDirectory() + "population.xml");
		config.controler().setOutputDirectory(testUtils.getOutputDirectory() + "output");
		config.controler().setWriteEventsInterval(100);
		config.controler().setWritePlansInterval(100);
		Controler controler = new Controler(config);
		controler.addOverridingModule(new InvertedNetworkTripRouterFactoryModule());
        controler.getConfig().controler().setCreateGraphs(false);
        TestListener listener = new TestListener();
		controler.addControlerListener(listener);
		controler.run();
		
		/**
		 * The lanes attached to link 23 should distribute the 3600 veh/h capacity 
		 * to link 34 with 600 veh/h (approx. 16.6 %) 
		 * to link 35 with 1200 veh/h (approx. 33.3 %) 
		 * to link 36 with 1800 veh/h to link 36 (approx. 50 %).
		 */
		Assert.assertTrue("", (15.5 < listener.percent34 && listener.percent34 < 17.5));
		Assert.assertTrue("", (32.5 < listener.percent35 && listener.percent35 < 34.5));
		Assert.assertTrue("", (49.0 < listener.percent36 && listener.percent36 < 51.0));
	}

	private static class TestListener implements StartupListener, IterationEndsListener, MobsimInitializedListener{

		private TestHandler testHandler = new TestHandler();
		double percent34;
		double percent35;
		double percent36;
		@Override
		public void notifyStartup(StartupEvent e) {
			e.getControler().getEvents().addHandler(testHandler);
			e.getControler().getMobsimListeners().add(this);
		}

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
		public void notifyMobsimInitialized(MobsimInitializedEvent e) {
			Assert.assertTrue(e.getQueueSimulation() instanceof QSim);
			QSim qsim = (QSim) e.getQueueSimulation();
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


	}
	
	
	private static class TestHandler implements LinkEnterEventHandler {

		private int count34 = 0;
		private int count35 = 0;
		private int count36 = 0;
		private Id<Link> id34 = Id.create("34", Link.class);
		private Id<Link> id35 = Id.create("35", Link.class);
		private Id<Link> id36 = Id.create("36", Link.class);
		
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
