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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.passenger.PassengerRequestCreator;
import org.matsim.contrib.dvrp.run.*;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic.DynActionCreator;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerProvider;
import org.matsim.contrib.taxi.optimizer.TaxiOptimizer;
import org.matsim.contrib.taxi.passenger.TaxiRequestCreator;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.vrpagent.TaxiActionCreator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.agentSpecificActivityScheduling.AgentSpecificActivityScheduling;
import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripNoiseAnalysisRun;

/**
* @author ikaddoura
*/

public class RunBerlinAV {
	
	private static final Logger log = Logger.getLogger(RunBerlinAV.class);

	private static String configFile;
	private static String outputDirectory;
	
	private static boolean analyzeNoise;
	
	private static boolean otfvis;
	
	public static void main(String[] args) {
		
		if (args.length > 0) {
			
			configFile = args[0];		
			log.info("configFile: "+ configFile);
			
			outputDirectory = args[1];
			log.info("outputDirectory: "+ outputDirectory);
			
			analyzeNoise = Boolean.getBoolean(args[2]);
			log.info("analyzeNoise: "+ analyzeNoise);
			
			otfvis = false;
			
		} else {
			configFile = "/Users/ihab/Documents/workspace/runs-svn/optAV/input/config_be_10pct.xml";
			outputDirectory = "/Users/ihab/Documents/workspace/runs-svn/optAV/output/baseCase_berlinArea_av-trip-share-0.1_av-20000/";
			analyzeNoise = true;
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
					
		DvrpConfigGroup.get(config).setMode(TaxiModule.TAXI_MODE);

		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
						
		AgentSpecificActivityScheduling aa = new AgentSpecificActivityScheduling(controler);
		controler = aa.prepareControler(false);
		
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
		controler.addOverridingModule(new TaxiModule());
        controler.addOverridingModule(new DvrpModule(fleet, new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				bind(TaxiOptimizer.class).toProvider(DefaultTaxiOptimizerProvider.class).asEagerSingleton();
				bind(VrpOptimizer.class).to(TaxiOptimizer.class);
				bind(DynActionCreator.class).to(TaxiActionCreator.class).asEagerSingleton();
				bind(PassengerRequestCreator.class).to(TaxiRequestCreator.class).asEagerSingleton();
			}
		}, TaxiOptimizer.class));
        
        final RandomizingTimeDistanceTravelDisutilityFactory dvrpTravelDisutilityFactory = 
        		new RandomizingTimeDistanceTravelDisutilityFactory(VrpTravelTimeModules.DVRP_ESTIMATED, controler.getConfig().planCalcScore());
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
				addTravelDisutilityFactoryBinding(VrpTravelTimeModules.DVRP_ESTIMATED).toInstance(dvrpTravelDisutilityFactory);
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
			NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModules().get(NoiseConfigGroup.GROUP_NAME);
			
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
			
			PersonTripNoiseAnalysisRun analysis1 = new PersonTripNoiseAnalysisRun(controler.getConfig().controler().getOutputDirectory(), outputFilePath + controler.getConfig().controler().getLastIteration() + ".events_NoiseImmission_Offline.xml.gz");
			analysis1.run();
			
		} else {
			PersonTripNoiseAnalysisRun analysis1 = new PersonTripNoiseAnalysisRun(controler.getConfig().controler().getOutputDirectory());
			analysis1.run();
		}
	}
}

