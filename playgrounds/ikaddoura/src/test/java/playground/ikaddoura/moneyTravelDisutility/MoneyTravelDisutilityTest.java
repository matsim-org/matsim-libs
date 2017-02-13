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
package playground.ikaddoura.moneyTravelDisutility;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.ikaddoura.PricingHandler;

/**
 * Simply plugging everything together and starting a run for an empty scenario.
 * 
 * @author ikaddoura
 *
 */
public class MoneyTravelDisutilityTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public final void test1() {

		double sigma = 0.;
		
		Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());
		scenario.getConfig().controler().setLastIteration(0);
		scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory() + "test1/");
		Controler controler = new Controler(scenario);
				
		// some arbitrary pricing
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				this.addEventHandlerBinding().to(PricingHandler.class);
			}
		});

		// money travel disutility
		final MoneyTimeDistanceTravelDisutilityFactory factory = new MoneyTimeDistanceTravelDisutilityFactory(
				new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()));
		
		factory.setSigma(sigma);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
								
				// travel disutility
				this.bindCarTravelDisutilityFactory().toInstance(factory);
				this.bind(MoneyEventAnalysis.class).asEagerSingleton();
				
				// person money event handler + controler listener
				this.addControlerListenerBinding().to(MoneyEventAnalysis.class);
				this.addEventHandlerBinding().to(MoneyEventAnalysis.class);
			}
		}); 

		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
	}
}
