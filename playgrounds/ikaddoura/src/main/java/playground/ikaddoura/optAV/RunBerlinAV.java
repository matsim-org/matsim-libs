/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.ikaddoura.optAV;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiOptimizerModules;
import org.matsim.contrib.taxi.run.TaxiOutputModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivityScheduling;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripNoiseAnalysisRun;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionConfigGroup.TollingApproach;
import playground.ikaddoura.decongestion.DecongestionControlerListener;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.handler.DelayAnalysis;
import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;
import playground.ikaddoura.moneyTravelDisutility.MoneyTimeDistanceTravelDisutilityFactory;

/**
* @author ikaddoura
*/

public class RunBerlinAV {
	
	private static final Logger log = Logger.getLogger(RunBerlinAV.class);

	private static String configFile;
	private static String outputDirectory;
	private static boolean analyzeNoise;
	private static boolean agentBasedActivityScheduling;
	
	private static boolean otfvis;
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			analyzeNoise = Boolean.getBoolean(args[2]);
			log.info("analyzeNoise: "+ analyzeNoise);
			
			agentBasedActivityScheduling = Boolean.getBoolean(args[3]);
			log.info("agentBasedActivityScheduling: "+ agentBasedActivityScheduling);
			
			otfvis = false;
			
		} else {
			configFile = "/Users/ihab/Documents/workspace/runs-svn/optAV/input/config_be_10pct_test.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/baseCase_berlinArea_av-trip-share-0.1_av-20000-test/";
			analyzeNoise = true;
			agentBasedActivityScheduling = false;
			otfvis = false;
		}
		
		RunBerlinAV runBerlinAV = new RunBerlinAV();
		runBerlinAV.run();
	}

	private void run() {
		
		Config config = ConfigUtils.loadConfig(configFile,
				new TaxiConfigGroup(),
				new DvrpConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		
		config.controler().setOutputDirectory(outputDirectory);
		DvrpConfigGroup.get(config).setMode(TaxiOptimizerModules.TAXI_MODE);

		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
				
		if (agentBasedActivityScheduling) {
			AgentSpecificActivityScheduling aa = new AgentSpecificActivityScheduling(controler);
			controler = aa.prepareControler(false);
		}
		
		// #############################
		// congestion pricing
		// #############################

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setTOLLING_APPROACH(TollingApproach.NoPricing);
		decongestionSettings.setKp(0.);
		decongestionSettings.setKi(0.);
		decongestionSettings.setKd(0.);
		decongestionSettings.setTOLERATED_AVERAGE_DELAY_SEC(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_START_PRICE_ADJUSTMENT(1.0);
		decongestionSettings.setFRACTION_OF_ITERATIONS_TO_END_PRICE_ADJUSTMENT(0.0);
		decongestionSettings.setMsa(false);
		decongestionSettings.setRUN_FINAL_ANALYSIS(false);
		decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
		log.info(decongestionSettings.toString());
			
		DecongestionInfo info = new DecongestionInfo(decongestionSettings);
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				
				this.bind(DecongestionInfo.class).toInstance(info);
				
				this.bind(DelayAnalysis.class).asEagerSingleton();				
				this.addEventHandlerBinding().to(DelayAnalysis.class);
				
				this.addControlerListenerBinding().to(DecongestionControlerListener.class);

			}
		});
		
		// #############################
		// taxi
		// #############################

		FleetImpl fleet = new FleetImpl();
		new VehicleReader(scenario.getNetwork(), fleet).readFile(taxiCfg.getTaxisFileUrl(config.getContext()).getFile());
		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().to(TaxiFareHandler.class).asEagerSingleton();
			}
		});
		controler.addOverridingModule(new TaxiOutputModule());
		controler.addOverridingModule(TaxiOptimizerModules.createDefaultModule(fleet));
        
		 final MoneyTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory = new MoneyTimeDistanceTravelDisutilityFactory(
					new RandomizingTimeDistanceTravelDisutilityFactory(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER,
							controler.getConfig().planCalcScore()));
			
			controler.addOverridingModule(new AbstractModule(){
				@Override
				public void install() {
													
					addTravelDisutilityFactoryBinding(DefaultTaxiOptimizerProvider.TAXI_OPTIMIZER).toInstance(dvrpTravelDisutilityFactory);
					
					this.bind(MoneyEventAnalysis.class).asEagerSingleton();
					this.addControlerListenerBinding().to(MoneyEventAnalysis.class);
					this.addEventHandlerBinding().to(MoneyEventAnalysis.class);
				}
			}); 

		// #############################
		// run
		// #############################

		if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());	
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// #############################
		// analysis
		// #############################
		
		String outputDirectory = config.controler().getOutputDirectory();
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
		
		if (analyzeNoise) {
			NoiseConfigGroup noiseParameters = (NoiseConfigGroup) scenario.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);
			noiseParameters.setInternalizeNoiseDamages(false);
			
			if (agentBasedActivityScheduling) {
				List<String> consideredActivitiesForSpatialFunctionality = new ArrayList<>();
				for (ActivityParams params : controler.getConfig().planCalcScore().getActivityParams()) {
					if (!params.getActivityType().contains("interaction")) {
						consideredActivitiesForSpatialFunctionality.add(params.getActivityType());
					}
				}
				String[] consideredActivitiesForSpatialFunctionalityArray = new String[consideredActivitiesForSpatialFunctionality.size()];
				noiseParameters.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForSpatialFunctionality.toArray(consideredActivitiesForSpatialFunctionalityArray));
			}

			log.info(noiseParameters.toString());
			NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
			noiseCalculation.run();	
			
			String outputFilePath = outputDirectory + "noise-analysis_it." + scenario.getConfig().controler().getLastIteration() + "/";
			ProcessNoiseImmissions process = new ProcessNoiseImmissions(outputFilePath + "immissions/", outputFilePath + "receiverPoints/receiverPoints.csv", noiseParameters.getReceiverPointGap());
			process.run();
			
			if (noiseParameters.isComputeNoiseDamages()) {
				final String[] labels = { "damages_receiverPoint" };
				final String[] workingDirectories = { outputFilePath + "damages_receiverPoint/" };

				MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
				merger.setReceiverPointsFile(outputFilePath + "receiverPoints/receiverPoints.csv");
				merger.setOutputDirectory(outputFilePath);
				merger.setTimeBinSize(noiseParameters.getTimeBinSizeNoiseComputation());
				merger.setWorkingDirectory(workingDirectories);
				merger.setLabel(labels);
				merger.run();
			}
			
			PersonTripNoiseAnalysisRun analysis = new PersonTripNoiseAnalysisRun(controler.getConfig().controler().getOutputDirectory(), outputFilePath + controler.getConfig().controler().getLastIteration() + ".events_NoiseImmission_Offline.xml.gz");
			analysis.run();
			
		} else {
			PersonTripNoiseAnalysisRun analysis = new PersonTripNoiseAnalysisRun(controler.getConfig().controler().getOutputDirectory());
			analysis.run();
		}
	}
}

