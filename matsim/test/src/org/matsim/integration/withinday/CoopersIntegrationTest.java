/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.integration.withinday;

import org.matsim.config.Config;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.LinkSensorManager;
import org.matsim.withinday.coopers.CoopersControler;


/**
 * Several integration tests for the withinday replanning implementation used for the Coopers project.
 * The tested output is the number of cars guided to an alternative route if an accident occurs.
 * This depends on the control strategy, the travel time prediction model and the complexity of the
 * network, items which are covered by the different test methods.
 * All tests use the ControlInputSB prediction model.
 * Tests for real time
 * 
 * @author dgrether
 * @see org.matsim.integration.withinday.CoopersBerlinIntegrationTest for tests with a large scale network 
 * that are using the ControlInputMB prediction model too.
 */
public class CoopersIntegrationTest extends MatsimTestCase {

	/**
	 * @see org.matsim.testcases.MatsimTestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	
	public void testBasicFunctionalityNoControl() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "config.xml");
		String netFileName = this.getClassInputDirectory() + "basicTestNetwork.xml";
		config.network().setInputFile(netFileName);
		config.network().setTimeVariantNetwork(true);
		String changeEventsFile = this.getClassInputDirectory() + "basicTestNetworkChangeEvents.xml";
		config.network().setChangeEventInputFile(changeEventsFile);
		
		config.plans().setInputFile(this.getClassInputDirectory() + "basicTestPlansOneRoute.xml.gz");

		config.withinday().setTrafficManagementConfiguration(this.getInputDirectory() + "trafficManagementConfiguration.xml");
		
		config.controler().setOutputDirectory(this.getOutputDirectory());
		
		CoopersControler controler = new CoopersControler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		
		LinkSensorManager lsm = new LinkSensorManager();
	  lsm.addLinkSensor("5");
	  lsm.addLinkSensor("7");
	  controler.getEvents().addHandler(lsm);
		
		controler.run();
		
		//as no control is used all vehicles have to stay at their initial route 
		assertEquals(17, lsm.getLinkTraffic("5"));
		assertEquals(0, lsm.getLinkTraffic("7"));		
	}

	
	public void testBasicFunctionalityBangBangControler() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "config.xml");
		String netFileName = this.getClassInputDirectory() + "basicTestNetwork.xml";
		config.network().setInputFile(netFileName);
		config.network().setTimeVariantNetwork(true);
		String changeEventsFile = this.getClassInputDirectory() + "basicTestNetworkChangeEvents.xml";
		config.network().setChangeEventInputFile(changeEventsFile);
		
		config.plans().setInputFile(this.getClassInputDirectory() + "basicTestPlansOneRoute.xml.gz");

		config.withinday().setTrafficManagementConfiguration(this.getInputDirectory() + "trafficManagementConfiguration.xml");
		
		config.controler().setOutputDirectory(this.getOutputDirectory());
		
		CoopersControler controler = new CoopersControler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		
		LinkSensorManager lsm = new LinkSensorManager();
	  lsm.addLinkSensor("5");
	  lsm.addLinkSensor("7");
	  controler.getEvents().addHandler(lsm);
		
		controler.run();
		//10 cars have to be guided to the alternative route
		assertEquals(7, lsm.getLinkTraffic("5"));
		assertEquals(10, lsm.getLinkTraffic("7"));		
	}

	
	public void testExtendedFunctionalityInflow() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "config.xml");
		String netFileName = this.getClassInputDirectory() + "extendedTestNetwork.xml";
		config.network().setInputFile(netFileName);
		config.network().setTimeVariantNetwork(true);
		String changeEventsFile = this.getClassInputDirectory() + "extendedTestNetworkChangeEvents.xml";
		config.network().setChangeEventInputFile(changeEventsFile);

		config.plans().setInputFile(this.getInputDirectory() + "plansInflow.xml.gz");

		config.withinday().setTrafficManagementConfiguration(this.getClassInputDirectory() + "extendedTestsTrafficManagementConfiguration.xml");
		
		config.controler().setOutputDirectory(this.getOutputDirectory());
		
		CoopersControler controler = new CoopersControler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		
		LinkSensorManager lsm = new LinkSensorManager();
	  lsm.addLinkSensor("10");
	  lsm.addLinkSensor("20");
	  controler.getEvents().addHandler(lsm);
		
		controler.run();
		int count10 = lsm.getLinkTraffic("10");
		int count20 = lsm.getLinkTraffic("20");
		System.out.println("traffic link 10: " + count10);
		System.out.println("traffic link 20: " + count20);
		assertEquals(2051, count10);
		assertEquals(5948, count20);
	}
	
	public void testExtendedFunctionalityOutflow() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "config.xml");
		String netFileName = this.getClassInputDirectory() + "extendedTestNetwork.xml";
		config.network().setInputFile(netFileName);
		config.network().setTimeVariantNetwork(true);
		String changeEventsFile = this.getClassInputDirectory() + "extendedTestNetworkChangeEvents.xml";
		config.network().setChangeEventInputFile(changeEventsFile);

		config.plans().setInputFile(this.getInputDirectory() + "plansOutflow.xml.gz");

		config.withinday().setTrafficManagementConfiguration(this.getClassInputDirectory() + "extendedTestsTrafficManagementConfiguration.xml");
		
		config.controler().setOutputDirectory(this.getOutputDirectory());
		
		CoopersControler controler = new CoopersControler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		
		LinkSensorManager lsm = new LinkSensorManager();
	  lsm.addLinkSensor("10");
	  lsm.addLinkSensor("20");
	  controler.getEvents().addHandler(lsm);
		
		controler.run();
		int count10 = lsm.getLinkTraffic("10");
		int count20 = lsm.getLinkTraffic("20");
		System.out.println("traffic link 10: " + count10);
		System.out.println("traffic link 20: " + count20);
		assertEquals(3411, count10);
		assertEquals(5587, count20);
	}
	
	public void testExtendedFunctionalityDistribution() {
		Config config = this.loadConfig(this.getClassInputDirectory() + "config.xml");
		String netFileName = this.getClassInputDirectory() + "extendedTestNetwork.xml";
		config.network().setInputFile(netFileName);
		
		config.network().setTimeVariantNetwork(true);
		String changeEventsFile = this.getClassInputDirectory() + "extendedTestNetworkChangeEvents.xml";
		config.network().setChangeEventInputFile(changeEventsFile);
		config.plans().setInputFile(this.getInputDirectory() + "plansDistribution.xml.gz");

		config.withinday().setTrafficManagementConfiguration(this.getClassInputDirectory() + "extendedTestsTrafficManagementConfiguration.xml");
		
		config.controler().setOutputDirectory(this.getOutputDirectory());
		
		CoopersControler controler = new CoopersControler(config);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(0);
		
		LinkSensorManager lsm = new LinkSensorManager();
	  lsm.addLinkSensor("10");
	  lsm.addLinkSensor("20");
	  controler.getEvents().addHandler(lsm);
		
		controler.run();
		int count10 = lsm.getLinkTraffic("10");
		int count20 = lsm.getLinkTraffic("20");
		System.out.println("traffic link 10: " + count10);
		System.out.println("traffic link 20: " + count20);
		assertEquals(2538, count10);
		assertEquals(5458, count20);		
	}
	
}
