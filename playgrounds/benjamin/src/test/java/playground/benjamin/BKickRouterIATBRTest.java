/* *********************************************************************** *
 * project: org.matsim.*
 * BKickRouterTestIATBR.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.benjamin;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.testcases.MatsimTestCase;

import playground.benjamin.income2.BKickIncome2Controler;









/**
 * Tests the routing of the BKickIncomeControler2
 * @author dgrether
 *
 */
public class BKickRouterIATBRTest extends MatsimTestCase {

	/*package*/ final static Id id1 = new IdImpl("1");
	/*package*/ final static Id id2 = new IdImpl("2");
	/*package*/ final static Id id3 = new IdImpl("3");
	
		
	
	public void testGeneralizedCostRouting() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "configRouterTestIATBR.xml");
		config.controler().setOutputDirectory(this.getOutputDirectory());
		String netFileName = this.getClassInputDirectory() + "network.xml";
		config.network().setInputFile(netFileName);
		config.plans().setInputFile(this.getClassInputDirectory() + "plansRouterTest.xml");
		//hh loading
		config.scenario().setUseHouseholds(true);
		config.households().setInputFile(this.getClassInputDirectory() + "households.xml");

		BKickIncome2Controler controler = new BKickIncome2Controler(config);
	  controler.setCreateGraphs(false);
		final EventHandler handler = new EventHandler();
		
		controler.addControlerListener(new StartupListener() {
			public void notifyStartup(final StartupEvent event) {
				event.getControler().getEvents().addHandler(handler);
//				event.getControler().getEvents().addHandler(new LogOutputEventHandler());
			}
		});
		
		controler.run();
		assertTrue("Person 2 should be routed on link 3", handler.link3Ok);
//		assertTrue("Person 1 should be routed on link 8", handler.link8Ok);
//		assertTrue("Person 3 should be routed on link 10", handler.link10Ok);
	}
	
	
	private static class EventHandler implements LinkEnterEventHandler{
		
		boolean link3Ok = false;
		boolean link8Ok = false;
		boolean link10Ok = false;
		
		//links 3, 8, 10 
		public void handleEvent(LinkEnterEvent e) {
			if (e.getLinkId().equals(new IdImpl("3")) && e.getPersonId().equals(new IdImpl("2"))) {
				link3Ok = true;
			}
			else if (e.getLinkId().equals(new IdImpl("8")) && e.getPersonId().equals(new IdImpl("1"))) {
				link8Ok = true;
			}
			else if (e.getLinkId().equals(new IdImpl("10")) && e.getPersonId().equals(new IdImpl("3"))) {
				link10Ok = true;
			}
		}

		public void reset(int iteration) {}
		
	}
	
	
}