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
package playground.ikaddoura.optAV;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.noise.NoiseCalculationOnline;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiQSimProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.analysis.linkDemand.LinkDemandEventHandler;
import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;

/**
 * @author ikaddoura
 *
 */
public class OptAVTestIT {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	/**
	 *
	 */
	@Test
	public final void test1(){

		String configFile = testUtils.getPackageInputDirectory() + "config.xml";
		final boolean otfvis = false;
		final boolean runBaseCase = true;
		 
		// ##################################################################
		// baseCase
		// ##################################################################
		
		Config config1 = ConfigUtils.loadConfig(configFile,
				new TaxiConfigGroup(),
				new OTFVisConfigGroup(),
				new TaxiFareConfigGroup(),
				new NoiseConfigGroup());
		
		config1.controler().setOutputDirectory(testUtils.getOutputDirectory() + "bc");

		TaxiConfigGroup taxiCfg1 = TaxiConfigGroup.get(config1);
		config1.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config1.checkConsistency();
		
		Scenario scenario = ScenarioUtils.loadScenario(config1);
		Controler controler1 = new Controler(scenario);
		
		// taxi

		FleetImpl fleet = new FleetImpl();
		new VehicleReader(scenario.getNetwork(), fleet).readFile(taxiCfg1.getTaxisFileUrl(config1.getContext()).getFile());
		
		controler1.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});
		controler1.addOverridingModule(new TaxiModule(fleet));
		controler1.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
		controler1.addOverridingModule(new DynQSimModule<>(TaxiQSimProvider.class));
				
		// analysis
		
		if (otfvis) controler1.addOverridingModule(new OTFVisLiveModule());	
		
		LinkDemandEventHandler handler1 = new LinkDemandEventHandler(controler1.getScenario().getNetwork());
		controler1.getEvents().addHandler(handler1);
		controler1.getConfig().controler().setCreateGraphs(false);

		// run
		
		if (runBaseCase) controler1.run();
		
		// ##################################################################
		// noise pricing
		// ##################################################################

		Config config2 = ConfigUtils.loadConfig(configFile,
				new TaxiConfigGroup(),
				new OTFVisConfigGroup(),
				new TaxiFareConfigGroup(),
				new NoiseConfigGroup());
		
		config2.controler().setOutputDirectory(testUtils.getOutputDirectory() + "n");
		
		TaxiConfigGroup taxiCfg2 = TaxiConfigGroup.get(config2);
		config2.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config2.checkConsistency();
		
		Scenario scenario2 = ScenarioUtils.loadScenario(config2);
		Controler controler2 = new Controler(scenario2);
		
		// taxi

		FleetImpl fleet2 = new FleetImpl();
		new VehicleReader(scenario2.getNetwork(), fleet2).readFile(taxiCfg2.getTaxisFileUrl(config2.getContext()).getFile());
		
		controler2.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});
		
		controler2.addOverridingModule(new TaxiModule(fleet2));
		controler2.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05)); // replace the travel time computation
		controler2.addOverridingModule(new DynQSimModule<>(OptAVQSimProvider.class));
		
//		final MoneyTimeDistanceTravelDisutilityFactory factory = new MoneyTimeDistanceTravelDisutilityFactory(
//				new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler2.getConfig().planCalcScore()));		
		
		controler2.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
												
//				// travel disutility factory
//				this.bindCarTravelDsisutilityFactory().toInstance(factory); // only the travel disutility for taxis / av should be affected!
				
				this.bind(MoneyEventAnalysis.class).asEagerSingleton();
				this.addControlerListenerBinding().to(MoneyEventAnalysis.class);
				this.addEventHandlerBinding().to(MoneyEventAnalysis.class);
			}
		}); 
		
		// noise pricing
		
		NoiseContext noiseContext = new NoiseContext(controler2.getScenario());
		controler2.addControlerListener(new NoiseCalculationOnline(noiseContext));
		
		// analysis
		
		LinkDemandEventHandler handler2 = new LinkDemandEventHandler(controler2.getScenario().getNetwork());
		controler2.getEvents().addHandler(handler2);
		controler2.getConfig().controler().setCreateGraphs(false);
        
		if (otfvis) controler2.addOverridingModule(new OTFVisLiveModule());
		
		// run
		
		controler2.run();
		
		// print outs
		
		System.out.println("----------------------------------");
		System.out.println("Base case:");
		printResults1(handler1);
	
		System.out.println("----------------------------------");
		System.out.println("Noise pricing:");
		printResults1(handler2);
		
		// the demand on the noise sensitive route should go down in case of noise pricing (n)
		Assert.assertEquals(true, getNoiseSensitiveRouteDemand(handler2) < getNoiseSensitiveRouteDemand(handler1));
	}
	
	private void printResults1(LinkDemandEventHandler handler) {
		System.out.println("long but uncongested, low noise cost: " + getLongUncongestedDemand(handler));
		System.out.println("high noise cost: " + (getNoiseSensitiveRouteDemand(handler)));
	}
	
	private int getNoiseSensitiveRouteDemand(LinkDemandEventHandler handler) {
		int noiseSensitiveRouteDemand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_7_8"))) {
			noiseSensitiveRouteDemand = handler.getLinkId2demand().get(Id.createLinkId("link_7_8"));
		}
		return noiseSensitiveRouteDemand;
	}
	
	private int getLongUncongestedDemand(LinkDemandEventHandler handler) {
		int longUncongestedRouteDemand = 0;
		if (handler.getLinkId2demand().containsKey(Id.createLinkId("link_1_2"))) {
			longUncongestedRouteDemand = handler.getLinkId2demand().get(Id.createLinkId("link_1_2"));
		}
		return longUncongestedRouteDemand;
	}
		
}
