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
package playground.ikaddoura.decongestion;


import java.io.IOException;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripBasicAnalysisRun;
import playground.ikaddoura.decongestion.DecongestionConfigGroup.IntegralApproach;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.handler.DelayAnalysis;
import playground.ikaddoura.decongestion.handler.IntervalBasedTolling;
import playground.ikaddoura.decongestion.handler.IntervalBasedTollingAll;
import playground.ikaddoura.decongestion.handler.PersonVehicleTracker;
import playground.ikaddoura.decongestion.routing.TollTimeDistanceTravelDisutilityFactory;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollSetting;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollingPID;

/**
 * Starts an interval-based decongestion pricing simulation run.
 * 
 * @author ikaddoura
 *
 */
public class DecongestionRun {

	private static final Logger log = Logger.getLogger(DecongestionRun.class);

	private static String configFile;
	private static String outputBaseDirectory;
	
	public static void main(String[] args) throws IOException {
		log.info("Starting simulation run with the following arguments:");
		
		if (args.length > 0) {

			configFile = args[0];		
			log.info("config file: "+ configFile);
			
			outputBaseDirectory = args[1];		
			log.info("output directory: "+ outputBaseDirectory);

		} else {
			configFile = "../../../runs-svn/vickrey-decongestion/input/config.xml";
			outputBaseDirectory = "../../../runs-svn/vickrey-decongestion/output-NEW-1000.output_plans-asInput/";
		}
		
		DecongestionRun main = new DecongestionRun();
		main.run();
		
	}

	private void run() throws IOException {

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setMsa(true);
		decongestionSettings.setTOLL_BLEND_FACTOR(1.0);
		decongestionSettings.setKd(0.);
		decongestionSettings.setKi(0.05);
		decongestionSettings.setKp(0.05);
		decongestionSettings.setIntegralApproach(IntegralApproach.UnusedHeadway);
		decongestionSettings.setIntegralApproachUnusedHeadwayFactor(1000.0);
		
		decongestionSettings.setTOLL_ADJUSTMENT(1.0);
		decongestionSettings.setINITIAL_TOLL(1.0);
		
		decongestionSettings.setTOLERATED_AVERAGE_DELAY_SEC(20.);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(0.8);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(0.0);
		decongestionSettings.setUPDATE_PRICE_INTERVAL(1);
		
		Config config = ConfigUtils.loadConfig(configFile);
		config.addModule(decongestionSettings);
		
		double weight = Double.NEGATIVE_INFINITY;
		for (StrategySettings settings : config.strategy().getStrategySettings()) {
			if (settings.getStrategyName().equals("TimeAllocationMutator")) {
				weight = settings.getWeight();
			}
		}
		
		String outputDirectory = outputBaseDirectory +
				"iter" + config.controler().getLastIteration() +
				"_plans" + config.strategy().getMaxAgentPlanMemorySize() +
				"_scoreMSA" + config.planCalcScore().getFractionOfIterationsToStartScoreMSA() +
				"_disInnovStrat" + config.strategy().getFractionOfIterationsToDisableInnovation() +
				"_timeBin" + config.travelTimeCalculator().getTraveltimeBinSize() +
				"_brain" + config.planCalcScore().getBrainExpBeta() +
				"_timeWeight" + weight +
				"_timeRange" + config.timeAllocationMutator().getMutationRange();
		
		// General pricing settings
		outputDirectory = outputDirectory
					+ "_priceUpdate" + decongestionSettings.getUPDATE_PRICE_INTERVAL() + "_it"
					+ "_toleratedDelay" + decongestionSettings.getTOLERATED_AVERAGE_DELAY_SEC()
					+ "_start" + decongestionSettings.getFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT()
					+ "_end" + decongestionSettings.getFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT();
		
		// BangBang
//		outputDirectory = outputDirectory + 
//						"_init" + decongestionSettings.getINITIAL_TOLL() +
//						"_adj" + decongestionSettings.getTOLL_ADJUSTMENT();
			
//		// PID
		outputDirectory = outputDirectory +
						"_Kp" + decongestionSettings.getKp() +
						"_Ki" + decongestionSettings.getKi() +
						"_Kd" + decongestionSettings.getKd() +
						"_tollMSA" + decongestionSettings.isMsa() + 
						"_blendFactor" + decongestionSettings.getTOLL_BLEND_FACTOR() + 
						"_integral" + decongestionSettings.getIntegralApproach() + 
						"_alpha" + decongestionSettings.getIntegralApproachAverageAlpha() + 
						"_factor" + decongestionSettings.getIntegralApproachUnusedHeadwayFactor() +
						"_TEST2";
					
		log.info("Output directory: " + outputDirectory);
		
		config.controler().setOutputDirectory(outputDirectory + "/");
				
		// ---
		
		final Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		// #############################################################
		
		// congestion toll computation
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.bind(DecongestionInfo.class).asEagerSingleton();
				this.bind(DecongestionTollingPID.class).asEagerSingleton();
				
				this.bind(DecongestionTollSetting.class).to(DecongestionTollingPID.class);
				this.bind(IntervalBasedTolling.class).to(IntervalBasedTollingAll.class);
				
				this.bind(IntervalBasedTollingAll.class).asEagerSingleton();
				this.bind(DelayAnalysis.class).asEagerSingleton();
				this.bind(PersonVehicleTracker.class).asEagerSingleton();
								
				this.addEventHandlerBinding().to(DecongestionTollingPID.class);
				this.addEventHandlerBinding().to(IntervalBasedTollingAll.class);
				this.addEventHandlerBinding().to(DelayAnalysis.class);
				this.addEventHandlerBinding().to(PersonVehicleTracker.class);
				
				this.addControlerListenerBinding().to(DecongestionControlerListener.class);

			}
		});
		
		// toll-adjusted routing
		
		final TollTimeDistanceTravelDisutilityFactory travelDisutilityFactory = new TollTimeDistanceTravelDisutilityFactory();
		travelDisutilityFactory.setSigma(0.);
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				this.bindCarTravelDisutilityFactory().toInstance( travelDisutilityFactory );
			}
		});	
		
		// #############################################################
	
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists);
//      controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        controler.run();
        
        PersonTripBasicAnalysisRun analysis = new PersonTripBasicAnalysisRun(outputDirectory);
		analysis.run();
	}
}

