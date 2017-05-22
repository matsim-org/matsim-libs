/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.integrationCNE;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.integrationCNE.CNEIntegration.CongestionTollingApproach;
import playground.vsp.airPollution.exposure.GridTools;
import playground.vsp.airPollution.exposure.ResponsibilityGridTools;

/**
 * @author ikaddoura
 *
 */
public class CNETestIT {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 * Test for isolated congestion pricing, isolated noise pricing and simultaneous congestion and noise pricing.
	 *
	 */
	@Test
	public final void test1(){

		String configFile = testUtils.getPackageInputDirectory() + "CNETest/test1/config.xml";
		
		// baseCase
		CNEIntegration cneIntegration1 = new CNEIntegration(configFile, testUtils.getOutputDirectory() + "bc/");
		Controler controler1 = cneIntegration1.prepareControler();		
		LinkDemandEventHandler handler1 = new LinkDemandEventHandler(controler1.getScenario().getNetwork());
		controler1.getEvents().addHandler(handler1);
		controler1.getConfig().controler().setCreateGraphs(false);
		controler1.run();

		// congestion pricing
		CNEIntegration cneIntegration2 = new CNEIntegration(configFile, testUtils.getOutputDirectory() + "c/");
		cneIntegration2.setCongestionPricing(true);
		cneIntegration2.setCongestionTollingApproach(CongestionTollingApproach.QBPV3);
		Controler controler2 = cneIntegration2.prepareControler();
		final DecongestionConfigGroup decongestionConfigGroup2 = (DecongestionConfigGroup) controler2.getConfig().getModules().get(DecongestionConfigGroup.GROUP_NAME);
		decongestionConfigGroup2.setKp(99999.);
		LinkDemandEventHandler handler2 = new LinkDemandEventHandler(controler2.getScenario().getNetwork());
		controler2.getEvents().addHandler(handler2);
		controler2.getConfig().controler().setCreateGraphs(false);
		controler2.run();

		// noise pricing
		CNEIntegration cneIntegration3 = new CNEIntegration(configFile, testUtils.getOutputDirectory() + "n/");
		cneIntegration3.setNoisePricing(true);
		Controler controler3 = cneIntegration3.prepareControler();
		LinkDemandEventHandler handler3 = new LinkDemandEventHandler(controler3.getScenario().getNetwork());
		controler3.getEvents().addHandler(handler3);
		controler3.getConfig().controler().setCreateGraphs(false);
		controler3.run();
						
		// congestion + noise pricing
		CNEIntegration cneIntegration4 = new CNEIntegration(configFile, testUtils.getOutputDirectory() + "cn/");
		cneIntegration4.setCongestionPricing(true);
		cneIntegration4.setCongestionTollingApproach(CongestionTollingApproach.QBPV3);
		cneIntegration4.setNoisePricing(true);
		Controler controler4 = cneIntegration4.prepareControler();
		final DecongestionConfigGroup decongestionConfigGroup4 = (DecongestionConfigGroup) controler4.getConfig().getModules().get(DecongestionConfigGroup.GROUP_NAME);
		decongestionConfigGroup4.setKp(99999.);
		LinkDemandEventHandler handler4 = new LinkDemandEventHandler(controler4.getScenario().getNetwork());
		controler4.getEvents().addHandler(handler4);
		controler4.getConfig().controler().setCreateGraphs(false);
		controler4.run();
		
		System.out.println("----------------------------------");
		System.out.println("Base case:");
		printResults1(handler1);
		
		System.out.println("----------------------------------");
		System.out.println("Congestion pricing:");
		printResults1(handler2);
		
		System.out.println("----------------------------------");
		System.out.println("Noise pricing:");
		printResults1(handler3);
		
		System.out.println("----------------------------------");
		System.out.println("Congestion + noise pricing:");
		printResults1(handler4);
		
		// no zero demand on bottleneck link
		Assert.assertEquals(true,
				getBottleneckDemand(handler1) != 0 &&
				getBottleneckDemand(handler2) != 0 &&
				getBottleneckDemand(handler3) != 0 &&
				getBottleneckDemand(handler4) != 0);
				
		// the demand on the bottleneck link should go down in case of congestion pricing (c)
		Assert.assertEquals(true, getBottleneckDemand(handler2) < getBottleneckDemand(handler1));		
		
		// the demand on the noise sensitive route should go up in case of congestion pricing (c)
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler2) > getNoiseSensitiveRouteDemand(handler1));
		
		// the demand on the noise sensitive route should go down in case of noise pricing (n)
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler3) < getNoiseSensitiveRouteDemand(handler1));
		
		// the demand on the bottleneck link should go down in case of congestion + noise pricing (cn)
		Assert.assertEquals(true, getBottleneckDemand(handler4) < getBottleneckDemand(handler1));
		
		// the demand on the noise sensitive route should go down in case of congestion + noise pricing (cn)
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler4) < getNoiseSensitiveRouteDemand(handler1));
	
		// the demand on the long and uncongested route should go up in case of congestion and noise pricing (cn)
		Assert.assertEquals(true, getLongUncongestedDemand(handler4) > getLongUncongestedDemand(handler1));	
		
		// the demand on the bottleneck link should go down in case of congestion and noise pricing (cn) compared to noise pricing (n)
		Assert.assertEquals(true, getBottleneckDemand(handler4) < getBottleneckDemand(handler3));	
	}
	
	private void printResults1(LinkDemandEventHandler handler) {
		System.out.println("long but uncongested, low noise cost: " + getLongUncongestedDemand(handler));
		System.out.println("bottleneck, low noise cost: " + getBottleneckDemand(handler));
		System.out.println("high noise cost: " + (getNoiseSensitiveRouteDemand(handler)));
	}
	
	private int getNoiseSensitiveRouteDemand(LinkDemandEventHandler handler) {
		int noiseSensitiveRouteDemand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_7_8"))) {
			noiseSensitiveRouteDemand = handler.getLinkId2demand().get(Id.createLinkId("link_7_8"));
		}
		return noiseSensitiveRouteDemand;
	}

	private int getBottleneckDemand(LinkDemandEventHandler handler) {
		int bottleneckRouteDemand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_4_5"))) {
			bottleneckRouteDemand = handler.getLinkId2demand().get(Id.createLinkId("link_4_5"));
		}
		return bottleneckRouteDemand;
	}

	private int getLongUncongestedDemand(LinkDemandEventHandler handler) {
		int longUncongestedRouteDemand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_1_2"))) {
			longUncongestedRouteDemand = handler.getLinkId2demand().get(Id.createLinkId("link_1_2"));
		}
		return longUncongestedRouteDemand;
	}
	
	@Test
	public final void test2(){
		
		String configFile = testUtils.getPackageInputDirectory() + "CNETest/test2/config.xml";
		
		// air pollution pricing
		Integer noOfXCells = 30;
		Integer noOfYCells = 40;
		double xMin = -1200.00;
		double xMax = 10100.00;
		double yMin = -100.00;
		double yMax = 19050.00;
		Double timeBinSize = 3600.;
		int noOfTimeBins = 1;
		
		Scenario scenario1 = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile, new NoiseConfigGroup(), new EmissionsConfigGroup()));
		
		GridTools gt = new GridTools(scenario1.getNetwork().getLinks(),xMin, xMax, yMin, yMax, noOfXCells, noOfYCells);
		ResponsibilityGridTools rgt = new ResponsibilityGridTools(timeBinSize, noOfTimeBins, gt);
		
		// baseCase
		Controler controler1 = new Controler(scenario1);
		CNEIntegration cneIntegration1 = new CNEIntegration(controler1, gt, rgt);
		scenario1.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory() + "bc/");

		controler1 = cneIntegration1.prepareControler();		
		LinkDemandEventHandler handler1 = new LinkDemandEventHandler(controler1.getScenario().getNetwork());
		controler1.getEvents().addHandler(handler1);
		controler1.getConfig().controler().setCreateGraphs(false);
		controler1.run();		

		// e
		Scenario scenario2 = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile, new NoiseConfigGroup(), new EmissionsConfigGroup()));
		scenario2.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory() + "e/");
		Controler controler2 = new Controler(scenario2);

		CNEIntegration cneIntegration2 = new CNEIntegration(controler2, gt, rgt);
		cneIntegration2.setAirPollutionPricing(true);
		controler2 = cneIntegration2.prepareControler();

		LinkDemandEventHandler handler2 = new LinkDemandEventHandler(controler2.getScenario().getNetwork());
		controler2.getEvents().addHandler(handler2);
		controler2.getConfig().controler().setCreateGraphs(false);
		controler2.run();

		// noise pricing
		Scenario scenario3 = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile, new NoiseConfigGroup(), new EmissionsConfigGroup()));
		scenario3.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory() + "n/");
		Controler controler3 = new Controler(scenario3);
		CNEIntegration cneIntegration3 = new CNEIntegration(controler3, gt, rgt);
		cneIntegration3.setNoisePricing(true);
		controler3 = cneIntegration3.prepareControler();
		LinkDemandEventHandler handler3 = new LinkDemandEventHandler(controler3.getScenario().getNetwork());
		controler3.getEvents().addHandler(handler3);
		controler3.getConfig().controler().setCreateGraphs(false);
		controler3.run();
						
		// (congestion +) air pollution + noise pricing
		Scenario scenario4 = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(configFile, new NoiseConfigGroup(), new EmissionsConfigGroup(), new DecongestionConfigGroup()));
		DecongestionConfigGroup decongestionConfigGroup4 = (DecongestionConfigGroup) scenario4.getConfig().getModules().get(DecongestionConfigGroup.GROUP_NAME);
		decongestionConfigGroup4.setRUN_FINAL_ANALYSIS(false);
		scenario4.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory() + "cne/");
		Controler controler4 = new Controler(scenario4);
		CNEIntegration cneIntegration4 = new CNEIntegration(controler4, gt, rgt );
		cneIntegration4.setAirPollutionPricing(true);
		cneIntegration4.setNoisePricing(true);
		cneIntegration4.setCongestionPricing(true); // there is no congestion...
		controler4 = cneIntegration4.prepareControler();

		LinkDemandEventHandler handler4 = new LinkDemandEventHandler(controler4.getScenario().getNetwork());
		controler4.getEvents().addHandler(handler4);
		controler4.getConfig().controler().setCreateGraphs(false);
		controler4.run();
		
		// Print outs
		
		System.out.println("----------------------------------");
		System.out.println("Base case:");
		printResults2(handler1);
		
		System.out.println("----------------------------------");
		System.out.println("Air pollution pricing:");
		printResults2(handler2);
		
		System.out.println("----------------------------------");
		System.out.println("Noise pricing:");
		printResults2(handler3);
		
		System.out.println("----------------------------------");
		System.out.println("Simultaneous pricing:");
		printResults2(handler4);
						
		Assert.assertEquals("BC: all agents should use the short distance route.", 11, demand_highSpeed_lowN_highE(handler1));		
	
		Assert.assertEquals("E: fewer agents should use the high air pollution emission cost route.", true, demand_highSpeed_lowN_highE(handler2) < demand_highSpeed_lowN_highE(handler1));		
		Assert.assertEquals("E: most agents should use the medium distance low air pollution emission cost route.", true, demand_mediumSpeed_highN_lowE(handler2) > 6);		

		Assert.assertEquals("N: all agents should use the short distance + low n cost route.", 11, demand_highSpeed_lowN_highE(handler3));		
		
		Assert.assertEquals("N+E: more agents should use the long distance + low n cost  + low e cost route.", true, demand_lowSpeed_lowN_lowE(handler4) > demand_lowSpeed_lowN_lowE(handler1) 
				&& demand_lowSpeed_lowN_lowE(handler4) > (demand_lowSpeed_lowN_lowE(handler2)) && demand_lowSpeed_lowN_lowE(handler4) > (demand_lowSpeed_lowN_lowE(handler3)) );		
		Assert.assertEquals("N+E: most agents should use the long distance low air pollution emission + low noise cost route.", true, demand_lowSpeed_lowN_lowE(handler4) > 6);		
		
	}

	private void printResults2(LinkDemandEventHandler handler) {
		System.out.println("short distance + low N costs + high E costs: " + demand_highSpeed_lowN_highE(handler));
		System.out.println("long distance + low N costs + low E costs: " + demand_lowSpeed_lowN_lowE(handler));
		System.out.println("medium distance + high N costs + low E costs: " + demand_mediumSpeed_highN_lowE(handler));
	}

	private int demand_mediumSpeed_highN_lowE(LinkDemandEventHandler handler) {
		int demand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_7_8"))) {
			demand = handler.getLinkId2demand().get(Id.createLinkId("link_7_8"));
		}
		return demand;
	}

	private int demand_lowSpeed_lowN_lowE(LinkDemandEventHandler handler) {
		int demand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_3_6"))) {
			demand = handler.getLinkId2demand().get(Id.createLinkId("link_3_6"));
		}
		return demand;
	}

	private int demand_highSpeed_lowN_highE(LinkDemandEventHandler handler) {
		int demand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_1_2"))) {
			demand = handler.getLinkId2demand().get(Id.createLinkId("link_1_2"));
		}
		return demand;
	}
		
}
