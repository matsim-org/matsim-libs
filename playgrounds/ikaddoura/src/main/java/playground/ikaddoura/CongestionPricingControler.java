/* *********************************************************************** *
 * project: org.matsim.*
 * TestControler.java
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

/**
 * 
 */
package playground.ikaddoura;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.contrib.otfvis.OTFVisModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioImpl;

import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.analysis.vtts.VTTScomputation;
import playground.ikaddoura.router.VTTSTollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.router.VTTSTravelTimeAndDistanceBasedTravelDisutilityFactory;
import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV7;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.RandomizedTollTimeDistanceTravelDisutilityFactory;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author ikaddoura
 *
 */
public class CongestionPricingControler {

	private static final Logger log = Logger.getLogger(CongestionPricingControler.class);

	static String configFile;
	
	static String router; // standard, randomized, VTTSspecific	
	static String implementation; // V3, V7, V8, noPricing
	static String VTTSapproach; // different, equal

	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {

			configFile = args[0];		
			log.info("config file: "+ configFile);
			
			VTTSapproach = args[1];
			log.info("approach: " + VTTSapproach);
			
			implementation = args[2];
			log.info("implementation: " + implementation);
			
			router = args[3];
			log.info("router: " + router);

		} else {
			configFile = "../../shared-svn/studies/ihab/test_siouxFalls/input/config.xml";
			VTTSapproach = "different";
			implementation = "V3";
			router = "standard";
		}

		CongestionPricingControler main = new CongestionPricingControler();
		main.run();
	}

	private void run() {

		Controler controler = new Controler(configFile);

		if (implementation.equals("noPricing")) {
			
			final VTTSHandler vttsHandler = new VTTSHandler(controler.getScenario());

			if (router.equals("standard")) {
				// nothing to do here
				
			} else if (router.equals("randomized")) {
				
				throw new RuntimeException("Not implemented. Aborting...");
				
			} else if (router.equals("VTTSspecific")) {

				final VTTSTravelTimeAndDistanceBasedTravelDisutilityFactory factory = new VTTSTravelTimeAndDistanceBasedTravelDisutilityFactory(vttsHandler);
				factory.setSigma(0.); // for now no randomness
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				}); 
				
				controler.addControlerListener(new VTTScomputation(vttsHandler));	
				
			} else {
				throw new RuntimeException("Not implemented. Aborting...");
			}
			
			controler.addOverridingModule(new OTFVisModule());
			controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			controler.run();
			
		} else {
			
			final TollHandler tollHandler = new TollHandler(controler.getScenario());
			final VTTSHandler vttsHandler = new VTTSHandler(controler.getScenario());
			
			if (router.equals("standard")) {
				
				final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
				
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
					}
				});
				
			} else if (router.equals("randomized")) {
				
				final RandomizedTollTimeDistanceTravelDisutilityFactory factory = new RandomizedTollTimeDistanceTravelDisutilityFactory(
						new TravelTimeAndDistanceBasedTravelDisutilityFactory(),
						tollHandler
					) ;
				factory.setSigma(3.);
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				}); 
				
			} else if (router.equals("VTTSspecific")) {
				
				final VTTSTollTimeDistanceTravelDisutilityFactory factory = new VTTSTollTimeDistanceTravelDisutilityFactory(
						new VTTSTravelTimeAndDistanceBasedTravelDisutilityFactory(vttsHandler),
						tollHandler
					);
				factory.setSigma(0.); // for now no randomness
				
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				}); 
				
				controler.addControlerListener(new VTTScomputation(vttsHandler));	
			
			} else {
				throw new RuntimeException("Not implemented. Aborting...");
			}

			if (VTTSapproach.equals("different") && implementation.equals("V3")) {
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl) controler.getScenario())));
			
			} else if (VTTSapproach.equals("equal") && implementation.equals("V3")) {
				controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl) controler.getScenario())));
			
			} else if (VTTSapproach.equals("different") && implementation.equals("V7")) {
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV7(controler.getEvents(), (ScenarioImpl) controler.getScenario())));
			
			} else if (VTTSapproach.equals("equal") && implementation.equals("V7")) {
				controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV7(controler.getEvents(), (ScenarioImpl) controler.getScenario())));
			
			} else if (VTTSapproach.equals("different") && implementation.equals("V8")) {
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV8(controler.getEvents(), (ScenarioImpl) controler.getScenario())));
							
			} else {
				throw new RuntimeException("Not implemented. Aborting...");
			}

			controler.addOverridingModule(new OTFVisModule());
			controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
			controler.run();
		}

	}
}

