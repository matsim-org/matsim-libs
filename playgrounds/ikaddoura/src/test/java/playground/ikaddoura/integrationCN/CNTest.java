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
package playground.ikaddoura.integrationCN;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import org.junit.Assert;

import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author ikaddoura
 *
 */

public class CNTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Ignore
	@Test
	public final void test1(){

		String configFile = testUtils.getPackageInputDirectory() + "CNTest/config1.xml";
		CNControler cnControler = new CNControler();

		// baseCase
		String outputDirectory1 = testUtils.getOutputDirectory() + "bc";
		cnControler.run(outputDirectory1, configFile, false, false, 0.);
	
		// c
		String outputDirectory2 = testUtils.getOutputDirectory() + "c";
		cnControler.run(outputDirectory2, configFile, true, false, 0.);
			
		// n
		String outputDirectory3 = testUtils.getOutputDirectory() + "n";
		cnControler.run(outputDirectory3, configFile, false, true, 0.);
		
		// cn
		String outputDirectory4 = testUtils.getOutputDirectory() + "cn";
		cnControler.run(outputDirectory4, configFile, true, true, 0.);
		
		// analyze output events file
		LinkDemandEventHandler handler1 = analyzeEvents(outputDirectory1, configFile); // base case
		LinkDemandEventHandler handler2 = analyzeEvents(outputDirectory2, configFile); // c
		LinkDemandEventHandler handler3 = analyzeEvents(outputDirectory3, configFile); // n
		LinkDemandEventHandler handler4 = analyzeEvents(outputDirectory4, configFile); // cn	
		
		System.out.println("----------------------------------");
		System.out.println("Base case:");
		printResults(handler1);
		
		System.out.println("----------------------------------");
		System.out.println("Congestion pricing:");
		printResults(handler2);
		
		System.out.println("----------------------------------");
		System.out.println("Noise pricing:");
		printResults(handler3);
		
		System.out.println("----------------------------------");
		System.out.println("Congestion + Noise pricing:");
		printResults(handler4);
		
//		// no zero demand
//		Assert.assertEquals(true,
//				getBottleneckDemand(handler1) != 0 &&
//				getBottleneckDemand(handler2) != 0 &&
//				getBottleneckDemand(handler3) != 0 &&
//				getBottleneckDemand(handler4) != 0);
//		
//		// no zero demand
//		Assert.assertEquals(true,
//				getNoiseSensitiveRouteDemand(handler1) != 0 &&
//				getNoiseSensitiveRouteDemand(handler2) != 0 &&
//				getNoiseSensitiveRouteDemand(handler3) != 0 &&
//				getNoiseSensitiveRouteDemand(handler4) != 0);
//		
//		// test the direct elasticity
//		
//		// the demand on the noise sensitive route should go down in case of noise pricing (n)
//		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler3) < getNoiseSensitiveRouteDemand(handler1));
//
//		// the demand on the bottleneck link should go down in case of congestion pricing (c)
//		Assert.assertEquals(true, getBottleneckDemand(handler2) < getBottleneckDemand(handler1));
//		
//		// test the cross elasticity
//		
//		// the demand on the noise sensitive route should go up in case of congestion pricing (c)
//		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler2) > getNoiseSensitiveRouteDemand(handler1));
//
//		// the demand on the bottleneck link should go up in case of noise pricing (n)
//		Assert.assertEquals(true, getBottleneckDemand(handler3) > getBottleneckDemand(handler1));
//
//		// test the simultaneous pricing elasticity - this is very scenario specific
//		
//		// the demand on the long and uncongested route should go up in case of simultaneous congestion and noise pricing (cn)
////		Assert.assertEquals(true, getLongUncongestedDemand(handler1) > getBottleneckDemand(handler4)); // TODO
//
//		// in this setup the demand goes up on the bottleneck link in case of simultaneous congestion and noise pricing (cn)
//		Assert.assertEquals(true, getBottleneckDemand(handler4) > getBottleneckDemand(handler1));
//		
//		// in this setup the demand goes down on the noise sensitive route in case of simultaneous congestion and noise pricing (cn)
//		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler4) < getNoiseSensitiveRouteDemand(handler1));

	}
	
	private void printResults(LinkDemandEventHandler handler) {
		System.out.println("long but uncongested, low noise cost: " + getLongUncongestedDemand(handler));
		System.out.println("bottleneck, low noise cost: " + getBottleneckDemand(handler));
		System.out.println("high noise cost: " + getNoiseSensitiveRouteDemand(handler));
	}
	
	/*
	 * Just starts a randomized router.
	 * 
	 */
	@Ignore
	@Test
	public final void test2(){
		
		String configFile1 = testUtils.getPackageInputDirectory() + "CNTest/config1.xml";
		Controler controler = new Controler(configFile1);
		TollHandler tollHandler = new TollHandler(controler.getScenario());

		final CongestionTollTimeDistanceTravelDisutilityFactory factory = new CongestionTollTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, controler.getConfig().planCalcScore() ),
				tollHandler, controler.getConfig().planCalcScore()
			) ;
		factory.setSigma(3.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( factory );
			}
		}); 		
		
		controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
	}
	
	/*
	 * Tests if different travel disutility factories result in the same outcome.
	 * 
	 */
	@Ignore
	@Test
	public final void test3() {
		
		String configFile1 = testUtils.getPackageInputDirectory() + "CNTest/config1.xml";		
		
		// 1:
		
		Config config1 = ConfigUtils.loadConfig(configFile1);
		String outputDirectory1 = testUtils.getOutputDirectory() + "a";
		config1.controler().setOutputDirectory(outputDirectory1);

		Scenario scenario1 = ScenarioUtils.loadScenario(config1);
		Controler controler1 = new Controler(scenario1);
		
		TollHandler tollHandler1 = new TollHandler(scenario1);

		final CongestionTollTimeDistanceTravelDisutilityFactory factory1 = new CongestionTollTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, config1.planCalcScore() ),
				tollHandler1, config1.planCalcScore()
			) ;
		factory1.setSigma(0.);
		factory1.setBlendFactor(0.1);
		
		controler1.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( factory1 );
			}
		}); 		
		
		controler1.addControlerListener(new MarginalCongestionPricingContolerListener(controler1.getScenario(), tollHandler1, new CongestionHandlerImplV3(controler1.getEvents(), controler1.getScenario())));
		controler1.addOverridingModule(new OTFVisFileWriterModule());
		controler1.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler1.run();

		// 2: "deprecated way"
		
		Config config2 = ConfigUtils.loadConfig(configFile1);
		String outputDirectory2 = testUtils.getOutputDirectory() + "b";
		config2.controler().setOutputDirectory(outputDirectory2);

		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		Controler controler2 = new Controler(scenario2);
		
		TollHandler tollHandler2 = new TollHandler(scenario2);

		final TollDisutilityCalculatorFactory factory2 = new TollDisutilityCalculatorFactory(tollHandler2, config2.planCalcScore());
		controler2.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(factory2);
			}
		});
			
		controler2.addControlerListener(new MarginalCongestionPricingContolerListener(controler2.getScenario(), tollHandler2, new CongestionHandlerImplV3(controler2.getEvents(), controler2.getScenario())));
		controler2.addOverridingModule(new OTFVisFileWriterModule());
		controler2.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler2.run();

		// compare
				
		LinkDemandEventHandler handler1 = analyzeEvents(outputDirectory1, configFile1); // a
		System.out.println("Bottleneck demand - (a): " + getBottleneckDemand(handler1)); // a
		
		LinkDemandEventHandler handler2 = analyzeEvents(outputDirectory2, configFile1); // b
		System.out.println("Bottleneck demand - (b): " + getBottleneckDemand(handler2)); // b
		
		Assert.assertEquals("run a and b should result in the exact same outcome (without accounting for randomness!)", true, getBottleneckDemand(handler1) == getBottleneckDemand(handler2));

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

	private LinkDemandEventHandler analyzeEvents(String outputDirectory, String configFile) {

		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
				
		LinkDemandEventHandler handler = new LinkDemandEventHandler(scenario.getNetwork());
		eventsManager.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(outputDirectory + "/ITERS/it.10/10.events.xml.gz");
		
		return handler;
	}
		
}
