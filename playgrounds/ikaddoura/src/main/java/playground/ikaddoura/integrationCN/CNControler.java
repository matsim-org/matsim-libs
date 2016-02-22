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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.noise.NoiseCalculationOnline;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseAllocationApproach;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.MutableScenario;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysisMain;
import playground.ikaddoura.analysis.vtts.VTTSHandler;
import playground.ikaddoura.analysis.vtts.VTTScomputation;
import playground.ikaddoura.router.VTTSCongestionTollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.router.VTTSTimeDistanceTravelDisutilityFactory;
import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.controler.CongestionAnalysisControlerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;

/**
 * 
 * @author ikaddoura
 *
 */

public class CNControler {
	private static final Logger log = Logger.getLogger(CNControler.class);

	private static String outputDirectory;
	private static String configFile;

	private static boolean congestionPricing;
	private static boolean noisePricing;
	
	private static double sigma;
	
	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
			
			outputDirectory = args[0];
			log.info("Output directory: " + outputDirectory);
			
			configFile = args[1];
			log.info("Config file: " + configFile);
			
			congestionPricing = Boolean.parseBoolean(args[2]);
			log.info("Congestion Pricing: " + congestionPricing);
			
			noisePricing = Boolean.parseBoolean(args[3]);
			log.info("Noise Pricing: " + noisePricing);
			
			sigma = Double.parseDouble(args[4]);
			log.info("Sigma: " + sigma);
			
		} else {
			
			outputDirectory = null;
			configFile = "/Users/ihab/Desktop/test/config.xml";
			congestionPricing = true;
			noisePricing = true;
			sigma = 0.;
		}
				
		CNControler cnControler = new CNControler();
		cnControler.run(outputDirectory, configFile, congestionPricing, noisePricing, sigma);
	}

	public void run(String outputDirectory, String configFile, boolean congestionPricing, boolean noisePricing, double sigma) {
				
		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup());
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
		final VTTSHandler vttsHandler = new VTTSHandler(controler.getScenario());

		// noise
		
		NoiseContext noiseContext = null;
		
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		
		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		noiseParameters.setConsideredActivitiesForReceiverPointGridArray(consideredActivitiesForReceiverPointGrid);
				
		noiseParameters.setReceiverPointGap(100.);
			
		String[] consideredActivitiesForDamages = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForDamages);
						
		noiseParameters.setNoiseAllocationApproach(NoiseAllocationApproach.MarginalCost);
				
		noiseParameters.setScaleFactor(10.);
		
		Set<Id<Link>> tunnelLinkIDs = new HashSet<Id<Link>>();
		tunnelLinkIDs.add(Id.create("108041", Link.class));
		tunnelLinkIDs.add(Id.create("108142", Link.class));
		tunnelLinkIDs.add(Id.create("108970", Link.class));
		tunnelLinkIDs.add(Id.create("109085", Link.class));
		tunnelLinkIDs.add(Id.create("109757", Link.class));
		tunnelLinkIDs.add(Id.create("109919", Link.class));
		tunnelLinkIDs.add(Id.create("110060", Link.class));
		tunnelLinkIDs.add(Id.create("110226", Link.class));
		tunnelLinkIDs.add(Id.create("110164", Link.class));
		tunnelLinkIDs.add(Id.create("110399", Link.class));
		tunnelLinkIDs.add(Id.create("96503", Link.class));
		tunnelLinkIDs.add(Id.create("110389", Link.class));
		tunnelLinkIDs.add(Id.create("110116", Link.class));
		tunnelLinkIDs.add(Id.create("110355", Link.class));
		tunnelLinkIDs.add(Id.create("92604", Link.class));
		tunnelLinkIDs.add(Id.create("92603", Link.class));
		tunnelLinkIDs.add(Id.create("25651", Link.class));
		tunnelLinkIDs.add(Id.create("25654", Link.class));
		tunnelLinkIDs.add(Id.create("112540", Link.class));
		tunnelLinkIDs.add(Id.create("112556", Link.class));
		tunnelLinkIDs.add(Id.create("5052", Link.class));
		tunnelLinkIDs.add(Id.create("5053", Link.class));
		tunnelLinkIDs.add(Id.create("5380", Link.class));
		tunnelLinkIDs.add(Id.create("5381", Link.class));
		tunnelLinkIDs.add(Id.create("106309", Link.class));
		tunnelLinkIDs.add(Id.create("106308", Link.class));
		tunnelLinkIDs.add(Id.create("26103", Link.class));
		tunnelLinkIDs.add(Id.create("26102", Link.class));
		tunnelLinkIDs.add(Id.create("4376", Link.class));
		tunnelLinkIDs.add(Id.create("4377", Link.class));
		tunnelLinkIDs.add(Id.create("106353", Link.class));
		tunnelLinkIDs.add(Id.create("106352", Link.class));
		tunnelLinkIDs.add(Id.create("103793", Link.class));
		tunnelLinkIDs.add(Id.create("103792", Link.class));
		tunnelLinkIDs.add(Id.create("26106", Link.class));
		tunnelLinkIDs.add(Id.create("26107", Link.class));
		tunnelLinkIDs.add(Id.create("4580", Link.class));
		tunnelLinkIDs.add(Id.create("4581", Link.class));
		tunnelLinkIDs.add(Id.create("4988", Link.class));
		tunnelLinkIDs.add(Id.create("4989", Link.class));
		tunnelLinkIDs.add(Id.create("73496", Link.class));
		tunnelLinkIDs.add(Id.create("73497", Link.class));
		noiseParameters.setTunnelLinkIDsSet(tunnelLinkIDs);
			
		if (noisePricing) {	
			noiseParameters.setInternalizeNoiseDamages(true);
		} else {
			noiseParameters.setInternalizeNoiseDamages(false);
		}
		
		noiseParameters.setWriteOutputIteration(10);
		
		noiseContext = new NoiseContext(controler.getScenario());

		// congestion
		
		final TollHandler congestionTollHandler;
		if (congestionPricing) {
			congestionTollHandler = new TollHandler(controler.getScenario());
		} else {
			congestionTollHandler = null;
		}
		
		// toll disutility calculation
		
		if (noisePricing && congestionPricing) {
			// simultaneous noise and congestion pricing
			
			// a router which accounts for the person- and trip-specific VTTS, congestion and noise toll payments
			final VTTSTollTimeDistanceTravelDisutilityFactory factory = new VTTSTollTimeDistanceTravelDisutilityFactory(
					new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore()),
					noiseContext,
					congestionTollHandler, config.planCalcScore()
				);
			factory.setSigma(sigma);
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			}); 
			
			// computation of person- and trip-specific VTTS
			controler.addControlerListener(new VTTScomputation(vttsHandler));	
			
			// computation of noise events + consideration in scoring
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			
			// computation of congestion events + consideration in scoring
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandler, new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario) controler.getScenario())));
		
		} else if (noisePricing && congestionPricing == false) {
			// only noise pricing
			
			// a router which accounts for the person- and trip-specific VTTS and noise toll payments
			final VTTSNoiseTollTimeDistanceTravelDisutilityFactory factory = new VTTSNoiseTollTimeDistanceTravelDisutilityFactory(
					new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore()),
					noiseContext, config.planCalcScore()
				);
			factory.setSigma(sigma);
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			});
			
			// computation of person- and trip-specific VTTS
			controler.addControlerListener(new VTTScomputation(vttsHandler));
			
			// computation of noise events + consideration in scoring
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			
			// computation of congestion events + NO consideration in scoring
			controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario) controler.getScenario())));
			
		} else if (noisePricing == false && congestionPricing) {
			// only congestion pricing
			
			// a router which accounts for the person- and trip-specific VTTS and congestion toll payments
			final VTTSCongestionTollTimeDistanceTravelDisutilityFactory factory = new VTTSCongestionTollTimeDistanceTravelDisutilityFactory(
					new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore()),
					congestionTollHandler, config.planCalcScore()
				);
			factory.setSigma(sigma);
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			}); 
			
			// computation of person- and trip-specific VTTS
			controler.addControlerListener(new VTTScomputation(vttsHandler));
						
			// computation of noise events + NO consideration in scoring
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			
			// computation of congestion events + consideration in scoring
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandler, new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario) controler.getScenario())));
			
		} else if (noisePricing == false && congestionPricing == false) {
			// base case
						
			// a router which accounts for the person- and trip-specific VTTS
			final VTTSTimeDistanceTravelDisutilityFactory factory = new VTTSTimeDistanceTravelDisutilityFactory(vttsHandler, config.planCalcScore());
			factory.setSigma(sigma);
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			});
			
			// computation of person- and trip-specific VTTS
			controler.addControlerListener(new VTTScomputation(vttsHandler));
			
			// computation of noise events + NO consideration in scoring	
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			
			// computation of congestion events + NO consideration in scoring
			controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario) controler.getScenario())));

		} else {
			throw new RuntimeException("This setup is not considered. Aborting...");
		}
		
		controler.addOverridingModule(new OTFVisFileWriterModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// analysis
		PersonTripAnalysisMain analysis = new PersonTripAnalysisMain(controler.getConfig().controler().getOutputDirectory());
		analysis.run();
	}
	
}
