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
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.old.PersonTripBasicAnalysisRun;
import playground.ikaddoura.analysis.pngSequence2Video.MATSimVideoUtils;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV10;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.CongestionHandlerImplV7;
import playground.vsp.congestion.handlers.CongestionHandlerImplV8;
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

	private static String configFile = "../../../runs-svn/decongestion/input/config.xml";
	private static String outputBaseDirectory = "../../../runs-svn/decongestion/output/";
	
	private static PricingApproach pricingApproach = PricingApproach.DecongestionBangBangA;
	
	private static double parameterKp = Double.NEGATIVE_INFINITY;
	private static double parameterKi = Double.NEGATIVE_INFINITY;
	private static double parameterKd = Double.NEGATIVE_INFINITY;
	
	private enum PricingApproach {
        NoPricing,
        V3, V7, V8, V9, V10,
        DecongestionNoPricing,
        DecongestionBangBangA, DecongestionBangBangB,
        DecongestionP, DecongestionI, DecongestionD, DecongestionPID, DecongestionPIDwithParameters
	}
		
	public static void main(String[] args) throws IOException {		
		
		if (args.length > 0) {
			outputBaseDirectory = args[0];		
			log.info("output base directory: "+ outputBaseDirectory);
			
			configFile = args[1];		
			log.info("config file: "+ configFile);
			
			String pricingApproachString = args[2];
			
			// no pricing
			if (pricingApproachString.equals(PricingApproach.NoPricing.toString())) {
				pricingApproach = PricingApproach.NoPricing;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionNoPricing.toString())) {
				pricingApproach = PricingApproach.DecongestionNoPricing;
			
			// queue- and agent-based pricing
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
			
			// interval-based pricing
			} else if (pricingApproachString.equals(PricingApproach.DecongestionBangBangA.toString())) {
				pricingApproach = PricingApproach.DecongestionBangBangA;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionBangBangB.toString())) {
				pricingApproach = PricingApproach.DecongestionBangBangB;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionP.toString())) {
				pricingApproach = PricingApproach.DecongestionP;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionI.toString())) {
				pricingApproach = PricingApproach.DecongestionI;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionD.toString())) {
				pricingApproach = PricingApproach.DecongestionD;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionPID.toString())) {
				pricingApproach = PricingApproach.DecongestionPID;
			} else if (pricingApproachString.equals(PricingApproach.DecongestionPIDwithParameters.toString())) {
				pricingApproach = PricingApproach.DecongestionPIDwithParameters;
			
			// unknown pricing approach
			} else {
				throw new RuntimeException("Unknown pricing approach: " + pricingApproachString);
			}
			
			log.info("pricing approach: " + pricingApproach);
			
			if (args.length > 3) {
				parameterKp = Double.valueOf(args[3]);
				parameterKi = Double.valueOf(args[4]);
				parameterKd = Double.valueOf(args[5]);
				
				log.info("Kp: " + parameterKp);
				log.info("Ki: " + parameterKi);
				log.info("Kd: " + parameterKd);
			}			
						
		}

		PricingRunner main = new PricingRunner();
		main.run();
	}

	private void run() {

		Config config = ConfigUtils.loadConfig(configFile);
		
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd_HH-mm-ss");
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
				
		} else if (pricingApproach.equals(PricingApproach.V7)) {
			log.info(">>> Congestion Pricing (V7)");

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});

			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV7(controler.getEvents(), controler.getScenario())));
		
		} else if (pricingApproach.equals(PricingApproach.V8)) {
			log.info(">>> Congestion Pricing (V8)");

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, controler.getConfig().planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});

			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(), tollHandler, new CongestionHandlerImplV8(controler.getEvents(), controler.getScenario())));
			
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
			
		} else {
			throw new RuntimeException("Unknown pricing approach: " + pricingApproach + ". Aborting...");
		}
		
		controler.run();
		
		log.info("Analyzing the final iteration...");
		PersonTripBasicAnalysisRun analysis = new PersonTripBasicAnalysisRun(scenario.getConfig().controler().getOutputDirectory());
		analysis.run();
		
		try {
			MATSimVideoUtils.createLegHistogramVideo(controler.getConfig().controler().getOutputDirectory());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}

