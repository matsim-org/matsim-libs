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
import org.matsim.contrib.otfvis.OTFVisModule;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioImpl;

import playground.ikaddoura.noise2.NoiseCalculationOnline;
import playground.ikaddoura.noise2.NoiseParameters;
import playground.ikaddoura.noise2.data.GridParameters;
import playground.ikaddoura.noise2.data.NoiseAllocationApproach;
import playground.ikaddoura.noise2.data.NoiseContext;
import playground.ikaddoura.noise2.routing.NoiseTollDisutilityCalculatorFactory;
import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.controler.CongestionAnalysisControlerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * 
 * @author ikaddoura
 *
 */

public class CNControler {
	private static final Logger log = Logger.getLogger(CNControler.class);

	private static String configFile;

	private static boolean congestionPricing;
	private static boolean noisePricing;
	
	public static void main(String[] args) throws IOException {
		
		if (args.length > 0) {
			
			configFile = args[0];
			log.info("Config file: " + configFile);
			
			congestionPricing = Boolean.parseBoolean(args[1]);
			log.info("Congestion Pricing: " + congestionPricing);
			
			noisePricing = Boolean.parseBoolean(args[2]);
			log.info("Noise Pricing: " + noisePricing);
			
		} else {
			
			configFile = "/Users/ihab/Desktop/test/config.xml";
			congestionPricing = true;
			noisePricing = true;
		}
				
		CNControler noiseImmissionControler = new CNControler();
		noiseImmissionControler.run(configFile, congestionPricing, noisePricing);
	}

	public void run(String configFile, boolean congestionPricing, boolean noisePricing) {
		
		Controler controler = new Controler(configFile);
	
		// noise
		
		NoiseContext noiseContext = null;
		
		// grid parameters
		
		GridParameters gridParameters = new GridParameters();
		
		String[] consideredActivitiesForReceiverPointGrid = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		gridParameters.setConsideredActivitiesForReceiverPointGrid(consideredActivitiesForReceiverPointGrid);
				
		gridParameters.setReceiverPointGap(100.);
			
		String[] consideredActivitiesForDamages = {"home", "work", "educ_primary", "educ_secondary", "educ_higher", "kiga"};
		gridParameters.setConsideredActivitiesForSpatialFunctionality(consideredActivitiesForDamages);
				
		// noise parameters
		
		NoiseParameters noiseParameters = new NoiseParameters();
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
		noiseParameters.setTunnelLinkIDs(tunnelLinkIDs);
			
		if (noisePricing) {	
			noiseParameters.setInternalizeNoiseDamages(true);
		} else {
			noiseParameters.setInternalizeNoiseDamages(false);
		}
		
		noiseParameters.setWriteOutputIteration(10);
		
		noiseContext = new NoiseContext(controler.getScenario(), gridParameters, noiseParameters);

		// congestion
		
		TollHandler congestionTollHandler;
		if (congestionPricing) {
			congestionTollHandler = new TollHandler(controler.getScenario());
		} else {
			congestionTollHandler = null;
		}
		
		// toll disutility calculation
		
		if (noisePricing && congestionPricing) {
			// simultaneous noise and congestion pricing
			
			// router considers external congestion and noise cost
			final CNTollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new CNTollDisutilityCalculatorFactory(noiseContext, congestionTollHandler);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});
			
			// computation of noise events + consideration in scoring
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			
			// computation of congestion events + consideration in scoring
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandler, new CongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl) controler.getScenario())));
		
		} else if (noisePricing && congestionPricing == false) {
			// only noise pricing
			
			// router considers external noise cost
			final NoiseTollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new NoiseTollDisutilityCalculatorFactory(noiseContext);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});
			
			// computation of noise events + consideration in scoring
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			
			// computation of congestion events + NO consideration in scoring
			controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl) controler.getScenario())));
			
		} else if (noisePricing == false && congestionPricing) {
			// only congestion pricing
			
			// router considers external congestion cost
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(congestionTollHandler);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});
			
			// computation of noise events + NO consideration in scoring
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			
			// computation of congestion events + consideration in scoring
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandler, new CongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl) controler.getScenario())));
			
		} else if (noisePricing == false && congestionPricing == false) {
			// base case
			
			// default router
			
			// computation of noise events + NO consideration in scoring	
			controler.addControlerListener(new NoiseCalculationOnline(noiseContext));
			
			// computation of congestion events + NO consideration in scoring
			controler.addControlerListener(new CongestionAnalysisControlerListener(new CongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl) controler.getScenario())));

		} else {
			throw new RuntimeException("This setup is not considered. Aborting...");
		}
		
		controler.addOverridingModule(new OTFVisModule());
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
	}
	
}
