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
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripBasicAnalysisMain;
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

	private static final String configFile = "../../../runs-svn/vickreyPricing/input/config_vickrey.xml";
	private static final String outputBaseDirectory = "../../../runs-svn/vickreyPricing/output_vickrey/";
	private static final PricingApproach pricingApproach = PricingApproach.V10;
	
	private enum PricingApproach {
        NoPricing, IntervalBasedMarginalCostPricing, IntervalBasedAverageCostPricing, V3, V9, V10
	}
		
	public static void main(String[] args) throws IOException {		
		
		if (args.length > 0) {
			throw new RuntimeException("Not implemented. Aborting...");
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
		
		} else if (pricingApproach.equals(PricingApproach.IntervalBasedMarginalCostPricing)) {
			log.info(">>> Congestion Pricing (Interval based marginal cost approach)");
			
			if (config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate() == 0.) {
				log.warn("The monetary distance rate is 0. The randomized router won't work properly...");
			}
			
			final Builder factory = new Builder(TransportMode.car, config.planCalcScore());
			factory.setSigma(3.0);
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			});
			
//			controler.addControlerListener(new IntervalBasedCongestionPricing(scenario, DelayInternalizationApproach.MaximumDelay, 5 * 60., 10));
			controler.addControlerListener(new IntervalBasedCongestionPricing(scenario, DelayInternalizationApproach.LastAgentsDelay, 5 * 60., 10));
			
		} else if (pricingApproach.equals(PricingApproach.IntervalBasedAverageCostPricing)) {
			log.info(">>> Congestion Pricing (Interval based average cost approach)");
			
			if (config.planCalcScore().getModes().get(TransportMode.car).getMonetaryDistanceRate() == 0.) {
				log.warn("The monetary distance rate is 0. The randomized router won't work properly...");
			}
			
			final Builder factory = new Builder(TransportMode.car, config.planCalcScore());
			factory.setSigma(3.0);
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
					this.bindCarTravelDisutilityFactory().toInstance( factory );
				}
			});
			
			controler.addControlerListener(new IntervalBasedCongestionPricing(scenario, DelayInternalizationApproach.AverageDelay, 5 * 60., 10));
		
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
		
		} else {
			throw new RuntimeException("Unknown pricing approach: " + pricingApproach + ". Aborting...");
		}
		
		controler.run();
		
		log.info("Analyzing the final iteration...");
		PersonTripBasicAnalysisMain analysis = new PersonTripBasicAnalysisMain(scenario.getConfig().controler().getOutputDirectory());
		analysis.run();
	}
}

