/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivitySchedulingConfigGroup;
import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivitySchedulingModule;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.old.PersonTripCongestionNoiseAnalysisRun;
import playground.ikaddoura.analysis.pngSequence2Video.MATSimVideoUtils;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionModule;
import playground.ikaddoura.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import playground.vsp.congestion.controler.AdvancedMarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.CongestionTollTimeDistanceTravelDisutilityFactory;

/**
* @author ikaddoura
*/

public class BerlinControler {
	
	private static final Logger log = Logger.getLogger(BerlinControler.class);
	
	private static String configFile;
	private static String outputDirectory;
	
	private static PricingApproach pricingApproach;
		
	private enum PricingApproach {
		NoPricing, DecongestionPricing, QBPV3, QBPV9, QBPV10
	}
	
	private static double blendFactorQCP;
	
	private static double sigma;
		
	public static void main(String[] args) throws IOException {

		if (args.length > 0) {
		
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);

			String congestionTollingApproachString = args[2];
			if (congestionTollingApproachString.equals(PricingApproach.NoPricing.toString())) {
				pricingApproach = PricingApproach.NoPricing;
			} else if (congestionTollingApproachString.equals(PricingApproach.QBPV3.toString())) {
				pricingApproach = PricingApproach.QBPV3;
			} else if (congestionTollingApproachString.equals(PricingApproach.QBPV9.toString())) {
				pricingApproach = PricingApproach.QBPV9;
			} else if (congestionTollingApproachString.equals(PricingApproach.QBPV10.toString())) {
				pricingApproach = PricingApproach.QBPV10;
			} else if (congestionTollingApproachString.equals(PricingApproach.DecongestionPricing.toString())) {
				pricingApproach = PricingApproach.DecongestionPricing;
			} else {
				throw new RuntimeException("Unknown congestion pricing approach. Aborting...");
			}
			log.info("pricingApproach: " + pricingApproach);
			
			blendFactorQCP = Double.valueOf(args[3]);
			log.info("blendFactorQCP: "+ blendFactorQCP);
			
			sigma = Double.valueOf(args[4]);
			log.info("sigma: "+ sigma);
			 
		} else {
			
			configFile = "../../../runs-svn/berlin-dz-time/input/config.xml";
			outputDirectory = "../../../runs-svn/berlin-dz-time/output/";
			pricingApproach = PricingApproach.NoPricing;
			blendFactorQCP = 0.1;
			sigma = 0.;
		}
		
		BerlinControler berlin = new BerlinControler();
		berlin.run();
	}

	private void run() throws IOException {
		
		Config config = ConfigUtils.loadConfig(configFile, new AgentSpecificActivitySchedulingConfigGroup(), new DecongestionConfigGroup());
		config.controler().setOutputDirectory(outputDirectory);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new AgentSpecificActivitySchedulingModule(scenario));
				
		if (pricingApproach.toString().equals(PricingApproach.NoPricing.toString())) {
			
			// no pricing
			
		} else if (pricingApproach.toString().equals(PricingApproach.DecongestionPricing.toString())) {
						
			// congestion toll computation
			
			controler.addOverridingModule(new DecongestionModule(scenario));
			
			// toll-adjusted routing
			
			final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
			travelDisutilityFactory.setSigma(sigma);
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
				}
			});	
				
		} else if (pricingApproach.toString().equals(PricingApproach.QBPV3.toString())) {
			
			final TollHandler congestionTollHandlerQBP = new TollHandler(controler.getScenario());
			final CongestionTollTimeDistanceTravelDisutilityFactory factory = new CongestionTollTimeDistanceTravelDisutilityFactory(
					new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()),
					congestionTollHandlerQBP, controler.getConfig().planCalcScore()
				);
			factory.setSigma(sigma);
			factory.setBlendFactor(blendFactorQCP );
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			}); 
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));
			
		} else if (pricingApproach.toString().equals(PricingApproach.QBPV9.toString())) {
			
			final TollHandler congestionTollHandlerQBP = new TollHandler(controler.getScenario());
			final CongestionTollTimeDistanceTravelDisutilityFactory factory = new CongestionTollTimeDistanceTravelDisutilityFactory(
					new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()),
					congestionTollHandlerQBP, controler.getConfig().planCalcScore()
				);
			factory.setSigma(sigma);
			factory.setBlendFactor(blendFactorQCP);

			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			}); 
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV9(controler.getEvents(), controler.getScenario())));
			
		} else if (pricingApproach.toString().equals(PricingApproach.QBPV10.toString())) {
			
			final TollHandler congestionTollHandlerQBP = new TollHandler(controler.getScenario());
			final CongestionTollTimeDistanceTravelDisutilityFactory factory = new CongestionTollTimeDistanceTravelDisutilityFactory(
					new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, controler.getConfig().planCalcScore()),
					congestionTollHandlerQBP, controler.getConfig().planCalcScore()
				);
			factory.setSigma(sigma);
			factory.setBlendFactor(blendFactorQCP);
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			}); 
			controler.addControlerListener(new AdvancedMarginalCongestionPricingContolerListener(controler.getScenario(), congestionTollHandlerQBP, new CongestionHandlerImplV10(controler.getEvents(), controler.getScenario())));
			
		} else {
			throw new RuntimeException("Unknown congestion pricing approach. Aborting...");
		}
			
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// analysis
		
		PersonTripCongestionNoiseAnalysisRun analysis = new PersonTripCongestionNoiseAnalysisRun(controler.getConfig().controler().getOutputDirectory());
		analysis.run();
		
		MATSimVideoUtils.createLegHistogramVideo(controler.getConfig().controler().getOutputDirectory());
		
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		String OUTPUT_DIR = controler.getConfig().controler().getOutputDirectory();
		for (int index = firstIt+1; index < lastIt; index ++){
			String dirToDel = OUTPUT_DIR+"/ITERS/it."+index;
			log.info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectoryRecursively(new File(dirToDel).toPath());
		}
	}

}

