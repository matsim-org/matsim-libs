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

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripBasicAnalysisMain;
import playground.ikaddoura.analysis.pngSequence2Video.MATSimVideoUtils;
import playground.ikaddoura.decongestion.Decongestion;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionConfigGroup.TollingApproach;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
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
	
	private enum PricingApproach {
        NoPricing,
        V3, V7, V8, V9, V10,
        DecongestionNoPricing,
        DecongestionBangBangA, DecongestionBangBangB,
        DecongestionP, DecongestionI, DecongestionD, DecongestionPID
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
			
			// unknown pricing approach
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
		
		} else if (pricingApproach.equals(PricingApproach.DecongestionNoPricing)) {
			log.info(">>> Decongestion No Pricing");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.NoPricing);
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setWRITE_OUTPUT_ITERATION(10);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionBangBangA)) {
			log.info(">>> Decongestion Bang Bang A");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setWRITE_OUTPUT_ITERATION(10);
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.BangBang);
			decongestionSettings.setINITIAL_TOLL(10.0);
			decongestionSettings.setTOLL_ADJUSTMENT(1.0);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();	
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionBangBangB)) {
			log.info(">>> Decongestion Bang Bang B");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setWRITE_OUTPUT_ITERATION(10);
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.BangBang);
			decongestionSettings.setINITIAL_TOLL(10.0);
			decongestionSettings.setTOLL_ADJUSTMENT(10.0);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();		
			
		} else if (pricingApproach.equals(PricingApproach.DecongestionP)) {
			log.info(">>> Decongestion P Controller");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setWRITE_OUTPUT_ITERATION(10);
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.PID);
			decongestionSettings.setKp(1.0);
			decongestionSettings.setKi(0.0);
			decongestionSettings.setKd(0.0);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
		
		} else if (pricingApproach.equals(PricingApproach.DecongestionI)) {
			log.info(">>> Decongestion I Controller");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setWRITE_OUTPUT_ITERATION(10);
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.PID);
			decongestionSettings.setKp(0.0);
			decongestionSettings.setKi(1.0);
			decongestionSettings.setKd(0.0);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();
		
		} else if (pricingApproach.equals(PricingApproach.DecongestionD)) {
			log.info(">>> Decongestion D Controller");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setWRITE_OUTPUT_ITERATION(10);
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.PID);
			decongestionSettings.setKp(0.0);
			decongestionSettings.setKi(0.0);
			decongestionSettings.setKd(1.0);
			
			final DecongestionInfo info = new DecongestionInfo(scenario, decongestionSettings);
			Decongestion decongestion = new Decongestion(info);
			controler = decongestion.getControler();	
		
		} else if (pricingApproach.equals(PricingApproach.DecongestionPID)) {
			log.info(">>> Decongestion PID Controller");
			
			final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
			decongestionSettings.setRUN_FINAL_ANALYSIS(false);
			decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
			decongestionSettings.setWRITE_OUTPUT_ITERATION(10);
			decongestionSettings.setTOLLING_APPROACH(TollingApproach.PID);
			decongestionSettings.setKp(1.0);
			decongestionSettings.setKi(1.0);
			decongestionSettings.setKd(1.0);
			
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
		
		try {
			MATSimVideoUtils.createLegHistogramVideo(controler.getConfig().controler().getOutputDirectory());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}

