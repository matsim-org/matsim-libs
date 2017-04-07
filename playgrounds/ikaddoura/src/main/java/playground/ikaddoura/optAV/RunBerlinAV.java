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
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.noise.NoiseCalculationOnline;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.run.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivitySchedulingConfigGroup;
import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivitySchedulingModule;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripAnalysisModule;
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
			
			analyzeNoise = Boolean.parseBoolean(args[2]);
			log.info("analyzeNoise: "+ analyzeNoise);
			
			agentBasedActivityScheduling = Boolean.parseBoolean(args[3]);
			log.info("agentBasedActivityScheduling: "+ agentBasedActivityScheduling);
			
			otfvis = false;
			
		} else {
			configFile = "/Users/ihab/Documents/workspace/runs-svn/optAV/input/config_be_10pct_test.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/baseCase_test_2taxiTrips_monetDistanceRate0.0002/";
			analyzeNoise = false;
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
				new NoiseConfigGroup(),
				new AgentSpecificActivitySchedulingConfigGroup());
		
		config.controler().setOutputDirectory(outputDirectory);
		DvrpConfigGroup.get(config).setMode(TaxiModule.TAXI_MODE);

		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
				
		if (agentBasedActivityScheduling) {
			AgentSpecificActivitySchedulingConfigGroup asasConfigGroup = (AgentSpecificActivitySchedulingConfigGroup) scenario.getConfig().getModules().get(AgentSpecificActivitySchedulingConfigGroup.GROUP_NAME);
			asasConfigGroup.setAdjustPopulation(false);
			controler.addOverridingModule(new AgentSpecificActivitySchedulingModule(scenario));
		}
		
		// #############################
		// noise analysis
		// #############################
		
		if (analyzeNoise) {
			NoiseConfigGroup noiseParams = (NoiseConfigGroup) controler.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);
			noiseParams.setInternalizeNoiseDamages(false);
			
			if (agentBasedActivityScheduling) {
				List<String> consideredActivitiesForSpatialFunctionality = new ArrayList<>();
				for (ActivityParams params : controler.getConfig().planCalcScore().getActivityParams()) {
					if (!params.getActivityType().contains("interaction")) {
						consideredActivitiesForSpatialFunctionality.add(params.getActivityType());
					}
				}
				String[] consideredActivitiesForSpatialFunctionalityArray = new String[consideredActivitiesForSpatialFunctionality.size()];
				noiseParams.setConsideredActivitiesForDamageCalculationArray(consideredActivitiesForSpatialFunctionality.toArray(consideredActivitiesForSpatialFunctionalityArray));			
			}

			log.info(noiseParams.toString());
			controler.addControlerListener(new NoiseCalculationOnline(new NoiseContext(controler.getScenario())));
		}
		
		// #############################
		// taxi
		// #############################

		controler.addOverridingModule(new TaxiOutputModule());
		controler.addOverridingModule(new TaxiModule());
        
        // #############################
        // travel disutility
        // #############################
		
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
		// welfare analysis
		// #############################

		controler.addOverridingModule(new PersonTripAnalysisModule());
			
		// #############################
		// run
		// #############################
			
		if (otfvis) controler.addOverridingModule(new OTFVisLiveModule());	
        controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		// #############################
		// post processing
		// #############################
		
		String outputDirectory = config.controler().getOutputDirectory();
		if (!outputDirectory.endsWith("/")) {
			outputDirectory = outputDirectory + "/";
		}
		
		if (analyzeNoise) {
			
			NoiseConfigGroup noiseParams = (NoiseConfigGroup) controler.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);
			
			String immissionsDir = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/";
			String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
			
			ProcessNoiseImmissions processNoiseImmissions = new ProcessNoiseImmissions(immissionsDir, receiverPointsFile, noiseParams.getReceiverPointGap());
			processNoiseImmissions.run();
			
			final String[] labels = { "immission", "consideredAgentUnits" , "damages_receiverPoint" };
			final String[] workingDirectories = { controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/consideredAgentUnits/" , controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration()  + "/damages_receiverPoint/" };
	
			MergeNoiseCSVFile merger = new MergeNoiseCSVFile() ;
			merger.setReceiverPointsFile(receiverPointsFile);
			merger.setOutputDirectory(controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/");
			merger.setTimeBinSize(noiseParams.getTimeBinSizeNoiseComputation());
			merger.setWorkingDirectory(workingDirectories);
			merger.setLabel(labels);
			merger.run();
			
		}
	}
}

