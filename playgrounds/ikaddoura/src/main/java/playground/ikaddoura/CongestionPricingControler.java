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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysisMain;
import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.analysis.vtts.VTTScomputation;
import playground.ikaddoura.analysis.welfare.WelfareAnalysisControlerListener;
import playground.ikaddoura.router.VTTSCongestionTollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.router.VTTSTimeDistanceTravelDisutilityFactory;
import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV7;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;

/**
 * @author ikaddoura
 *
 */
public class CongestionPricingControler {

	private static final Logger log = Logger.getLogger(CongestionPricingControler.class);

	static String outputDirectory;
	static String configFile;
	
	static String router; // standard, VTTSspecific	
	static String implementation; // V3, V7, V8, V9, noPricing
	static String VTTSapproach; // different, equal
	static double sigma;

	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {

			outputDirectory = args[0];		
			log.info("output directory: "+ outputDirectory);
			
			configFile = args[1];		
			log.info("config file: "+ configFile);
			
			VTTSapproach = args[2];
			log.info("approach: " + VTTSapproach);
			
			implementation = args[3];
			log.info("implementation: " + implementation);
			
			router = args[4];
			log.info("router: " + router);
			
			sigma = Double.parseDouble(args[5]);
			log.info("Sigma: " + sigma);

		} else {
			outputDirectory = null;
			configFile = "../../shared-svn/studies/ihab/test_siouxFalls/input/config.xml";
			VTTSapproach = "different";
			implementation = "V3";
			router = "standard";
			sigma = 0.;
		}

		CongestionPricingControler main = new CongestionPricingControler();
		main.run();
	}

	private void run() {

		Config config = ConfigUtils.loadConfig(configFile);
		if (outputDirectory == null) {
			if (config.controler().getOutputDirectory() == null || config.controler().getOutputDirectory() == "") {
				throw new RuntimeException("Either provide an output directory in the config file or the controler. Aborting...");
			} else {
				log.info("Using the output directory given in the config file...");
			}
			
		} else {
			if (config.controler().getOutputDirectory() == null || config.controler().getOutputDirectory() == "") {
				log.info("Using the output directory provided in the controler.");
			} else {
				log.warn("The output directory in the config file will overwritten by the directory provided in the controler.");
			}
			config.controler().setOutputDirectory(outputDirectory);
		}
		
		Controler controler = new Controler(config);

		if (implementation.equals("noPricing")) {
			
			final VTTSHandler vttsHandler = new VTTSHandler(controler.getScenario());

			if (router.equals("standard")) {

				final Builder factory = new Builder( TransportMode.car, config.planCalcScore() );
				factory.setSigma(sigma);
				controler.addOverridingModule(new AbstractModule(){
					@Override
					public void install() {
						this.bindCarTravelDisutilityFactory().toInstance( factory );
					}
				}); 
				
			} else if (router.equals("VTTSspecific")) {

				final VTTSTimeDistanceTravelDisutilityFactory factory = new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore());
				factory.setSigma(sigma);
				
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
			
		} else {
			
			final TollHandler tollHandler = new TollHandler(controler.getScenario());
			final VTTSHandler vttsHandler = new VTTSHandler(controler.getScenario());
			
			if (router.equals("standard")) {

				final CongestionTollTimeDistanceTravelDisutilityFactory factory = new CongestionTollTimeDistanceTravelDisutilityFactory(new Builder( TransportMode.car, config.planCalcScore() ), tollHandler, config.planCalcScore());
				factory.setSigma(sigma);
				
				controler.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
						bindCarTravelDisutilityFactory().toInstance(factory);
					}
				});
				
			} else if (router.equals("VTTSspecific")) {
				
				final VTTSCongestionTollTimeDistanceTravelDisutilityFactory factory = new VTTSCongestionTollTimeDistanceTravelDisutilityFactory(
						new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore()),
						tollHandler, config.planCalcScore()
					);
				factory.setSigma(sigma);
				
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
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario) controler.getScenario())));
			
			} else if (VTTSapproach.equals("equal") && implementation.equals("V3")) {
				controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario) controler.getScenario())));
			
			} else if (VTTSapproach.equals("different") && implementation.equals("V7")) {
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV7(controler.getEvents(), (MutableScenario) controler.getScenario())));
			
			} else if (VTTSapproach.equals("equal") && implementation.equals("V7")) {
				controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV7(controler.getEvents(), (MutableScenario) controler.getScenario())));
			
			} else if (VTTSapproach.equals("different") && implementation.equals("V8")) {
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV8(controler.getEvents(), (MutableScenario) controler.getScenario())));
			
			} else if (VTTSapproach.equals("equal") && implementation.equals("V8")) {
				controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV8(controler.getEvents(), (MutableScenario) controler.getScenario())));

			} else if (VTTSapproach.equals("different") && implementation.equals("V9")) {
				controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV9(controler.getEvents(), (MutableScenario) controler.getScenario())));
			
			} else if (VTTSapproach.equals("equal") && implementation.equals("V9")) {
				controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV9(controler.getEvents(), (MutableScenario) controler.getScenario())));
		
			} else {
				throw new RuntimeException("Not implemented. Aborting...");
			}

		}
		
		controler.addControlerListener(new WelfareAnalysisControlerListener(controler.getScenario()));
		
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// analysis
		PersonTripAnalysisMain analysis = new PersonTripAnalysisMain(controler.getConfig().controler().getOutputDirectory());
		analysis.run();

	}
}

