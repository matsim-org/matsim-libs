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
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import org.junit.Assert;

import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;

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
		
		// baseCase
		String configFile1 = testUtils.getPackageInputDirectory() + "CNTest/config1.xml";
		CNControler cnControler1 = new CNControler();
		cnControler1.run(null, configFile1, false, false, 0.);
	
		// c
		String configFile2 = testUtils.getPackageInputDirectory() + "CNTest/config2.xml";
		CNControler cnControler2 = new CNControler();
		cnControler2.run(null, configFile2, true, false, 0.);
			
		// n
		String configFile3 = testUtils.getPackageInputDirectory() + "CNTest/config3.xml";
		CNControler cnControler3 = new CNControler();
		cnControler3.run(null, configFile3, false, true, 0.);
		
		// cn
		String configFile4 = testUtils.getPackageInputDirectory() + "CNTest/config4.xml";
		CNControler cnControler4 = new CNControler();
		cnControler4.run(null, configFile4, true, true, 0.);
		
		// analyze output events file
		LinkDemandEventHandler handler1 = analyzeEvents(configFile1); // base case
		LinkDemandEventHandler handler2 = analyzeEvents(configFile2); // c
		LinkDemandEventHandler handler3 = analyzeEvents(configFile3); // n
		LinkDemandEventHandler handler4 = analyzeEvents(configFile4); // cn	
		
		// test the direct elasticity
		
		// the demand on the noise sensitive route should go down in case of noise pricing (n)
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler3) < getNoiseSensitiveRouteDemand(handler1));

		// the demand on the bottleneck link should go down in case of congestion pricing (c)
		Assert.assertEquals(true, getBottleneckDemand(handler2) < getBottleneckDemand(handler1));
		
		// test the cross elasticity
		
		// the demand on the noise sensitive route should go up in case of congestion pricing (c)
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler2) > getNoiseSensitiveRouteDemand(handler1));

		// the demand on the bottleneck link should go up in case of noise pricing (n)
		Assert.assertEquals(true, getBottleneckDemand(handler3) > getBottleneckDemand(handler1));

		// test the simultaneous pricing elasticity - this is very scenario specific
		
		// the demand on the long and uncongested route should go up in case of simultaneous congestion and noise pricing (cn)
		Assert.assertEquals(true, getLongUncongestedDemand(handler1) < getBottleneckDemand(handler4));

		// in this setup the demand goes up on the bottleneck link in case of simultaneous congestion and noise pricing (cn)
		Assert.assertEquals(true, getBottleneckDemand(handler4) > getBottleneckDemand(handler1));
		
		// in this setup the demand goes down on the noise sensitive route in case of simultaneous congestion and noise pricing (cn)
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler4) < getNoiseSensitiveRouteDemand(handler1));

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
				new Builder( TransportMode.car, controler.getConfig().planCalcScore() ),
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

	private LinkDemandEventHandler analyzeEvents(String configFile) {

		Config config = ConfigUtils.loadConfig(configFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
				
		LinkDemandEventHandler handler = new LinkDemandEventHandler(scenario.getNetwork());
		eventsManager.addHandler(handler);
		
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(scenario.getConfig().controler().getOutputDirectory() + "/ITERS/it.10/10.events.xml.gz");
		
		return handler;
	}
		
}
