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
import java.text.SimpleDateFormat;
import java.util.Date;

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

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripBasicAnalysisMain;
import playground.ikaddoura.decongestion.Decongestion;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionConfigGroup.TollingApproach;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.intervalBasedCongestionPricing.IntervalBasedCongestionPricing;
import playground.ikaddoura.intervalBasedCongestionPricing.data.CongestionInfo.DelayInternalizationApproach;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV9;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * Starts an interval-based congestion pricing simulation run.
 * 
 * @author ikaddoura
 *
 */
public class PricingRunner {

	private static final Logger log = Logger.getLogger(PricingRunner.class);

	private static String configFile = "../../../runs-svn/vickreyPricing/input/config_vickrey_B.xml";
	private static String outputBaseDirectory = "../../../runs-svn/vickreyPricing/output_vickrey_B/";
	private static PricingApproach pricingApproach = PricingApproach.IntervalBasedMaximumDelayPricing;
	
	private enum PricingApproach {
        NoPricing,
        IntervalBasedMaximumDelayPricing, IntervalBasedAverageDelayPricing,
        V3, V7, V8, V9, V10,
        DecongestionNoPricing, DecongestionV0a, DecongestionV0b, DecongestionV1a, DecongestionV1b, DecongestionV2a, DecongestionV2b, DecongestionV4a, DecongestionV4b
	}
		
	public static void main(String[] args) throws IOException {		
		
		if (args.length > 0) {
			outputBaseDirectory = args[0];		
			log.info("output base directory: "+ outputBaseDirectory);
			
			configFile = args[1];		
			log.info("config file: "+ configFile);
			
			String pricingApproachString = args[2];
			if (pricingApproachString.equals(PricingApproach.NoPricing.toString())) {
				pricingApproach = PricingApproach.NoPricing;
			} else if (pricingApproachString.equals(PricingApproach.V3.toString())) {
				pricingApproach = PricingApproach.V3;
			} else if (pricingApproachString.equals(PricingApproach.V7.toString())) {
				pricingApproach = PricingApproach.V7;
			} else if (pricingApproachString.equals(PricingApproach.V8.toString())) {
				pricingApproach = PricingApproach.V8;
			} else if (pricingApproachString.equals(PricingApproach.V9.toString())) {
				pricingApproach = PricingApproach.V9;
			} else if (pricingApproachString.equals(PricingApproach.V10.toString())) {
				pricingApproach = PricingApproach.V10;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionNoPricing.toString())) {
				pricingApproach = PricingApproach.DecongestionNoPricing;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionV0a.toString())) {
				pricingApproach = PricingApproach.DecongestionV0a;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionV0b.toString())) {
				pricingApproach = PricingApproach.DecongestionV0b;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionV1a.toString())) {
				pricingApproach = PricingApproach.DecongestionV1a;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionV1b.toString())) {
				pricingApproach = PricingApproach.DecongestionV1b;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionV2a.toString())) {
				pricingApproach = PricingApproach.DecongestionV2a;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionV2b.toString())) {
				pricingApproach = PricingApproach.DecongestionV2b;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionV4a.toString())) {
				pricingApproach = PricingApproach.DecongestionV4a;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionV4b.toString())) {
				pricingApproach = PricingApproach.DecongestionV4b;
			} else {
				throw new RuntimeException("Unknown pricing approach: " + pricingApproachString);
			}
			
			log.info("pricing approach: " + pricingApproach);
		}

		PricingRunner main = new PricingRunner();
		main.run();
	}

	private void run() {

		Config config = ConfigUtils.loadConfig(configFile);
		
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd_HH-mm-ss ");
		Date currentTime = new Date();
		String dateTime = formatter.format(currentTime);
		String outputDirectory = outputBaseDirectory + "output_" + dateTime + "_" + pricingApproach.toString() + "/";
		log.info("Setting output directory to " + outputDirectory);
		
		config.controler().setOutputDirectory(outputDirectory);
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setCreateGraphs(true);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);

		if (pricingApproach.equals(PricingApproach.NoPricing)) {
			log.info(">>> No pricing. Starting a default MATSim run...");
		
		} else if (pricingApproach.equals(PricingApproach.IntervalBasedMaximumDelayPricing)) {
			log.info(">>> Congestion Pricing (Interval based marginal cost approach)");
			
			if (config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate() == 0.) {
				log.warn("The monetary distance rate is 0. The randomized router won't work properly...");
			}
			
			final RandomizingTimeDistanceTravelDisutilityFactory factory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore());
			factory.setSigma(3.0);
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			});
			
			controler.addControlerListener(new IntervalBasedCongestionPricing(scenario, DelayInternalizationApproach.MaximumDelay, 15 * 60., 10));
			
		} else if (pricingApproach.equals(PricingApproach.IntervalBasedAverageDelayPricing)) {
			log.info(">>> Congestion Pricing (Interval based average cost approach)");
			
			if (config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate() == 0.) {
				log.warn("The monetary distance rate is 0. The randomized router won't work properly...");
			}
			
			final RandomizingTimeDistanceTravelDisutilityFactory factory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config.planCalcScore());
			factory.setSigma(3.0);
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			});
			
			controler.addControlerListener(new IntervalBasedCongestionPricing(scenario, DelayInternalizationApproach.AverageDelay, 15 * 60., 10));
		
		} else if (pricingApproach.equals(PricingApproach.V9)) {
			log.info(">>> Congestion Pricing (V9)");

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});

			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV9(controler.getEvents(), controler.getScenario())));
		
		} else if (pricingApproach.equals(PricingApproach.V10)) {
			log.info(">>> Congestion Pricing (V10)");

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});

			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV10(controler.getEvents(), controler.getScenario())));
		
		} else if (pricingApproach.equals(PricingApproach.V3)) {
			log.info(">>> Congestion Pricing (V3)");

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});

			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV3(controler.getEvents(), controler.getScenario())));
		} else if (pricingApproach.equals(PricingApproach.DecongestionNoPricing)) {
			log.info(">>> Decongestion No Pricing");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.NoPricing);
			decongestionSettings.setTOLL_ADJUSTMENT(0.0);
			decongestionSettings.setUPDATE_PRICE_INTERVAL(10000);
			decongestionSettings.setTOLERATED_AVERAGE_DELAY_SEC(99999999999.);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionV0a)) {
			log.info(">>> Decongestion V0a");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.V0);
			decongestionSettings.setTOLL_ADJUSTMENT(0.0);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionV0b)) {
			log.info(">>> Decongestion V0b");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.V0);
			decongestionSettings.setTOLL_ADJUSTMENT(0.5);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionV1a)) {
			log.info(">>> Decongestion V1a");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.V1);
			decongestionSettings.setTOLL_ADJUSTMENT(0.0);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionV1b)) {
			log.info(">>> Decongestion V1b");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.V1);
			decongestionSettings.setTOLL_ADJUSTMENT(0.5);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionV2a)) {
			log.info(">>> Decongestion V2a");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.V2);
			decongestionSettings.setTOLL_ADJUSTMENT(0.0);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionV2b)) {
			log.info(">>> Decongestion V2b");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.V2);
			decongestionSettings.setTOLL_ADJUSTMENT(0.5);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionV4a)) {
			log.info(">>> Decongestion V4a");			
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.V4);
			decongestionSettings.setTOLL_ADJUSTMENT(0.0);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionV4b)) {
			log.info(">>> Decongestion V4b");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.V4);
			decongestionSettings.setTOLL_ADJUSTMENT(0.5);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
			
		} else {
			throw new RuntimeException("Unknown pricing approach: " + pricingApproach + ". Aborting...");
		}
		
		controler.run();
		
		log.info("Analyzing the final iteration...");
		PersonTripBasicAnalysisMain analysis = new PersonTripBasicAnalysisMain(scenario.getConfig().controler().getOutputDirectory());
		analysis.run();
	}
}

