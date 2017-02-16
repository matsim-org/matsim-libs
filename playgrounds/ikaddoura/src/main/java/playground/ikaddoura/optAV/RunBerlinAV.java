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

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.scoring.TaxiFareHandler;
import org.matsim.contrib.dvrp.data.FleetImpl;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.contrib.dvrp.trafficmonitoring.VrpTravelTimeModules;
import org.matsim.contrib.dynagent.run.DynQSimModule;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.NoiseOfflineCalculation;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.contrib.taxi.run.TaxiQSimProvider;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripNoiseAnalysisRun;
import playground.ikaddoura.analysis.dynamicLinkDemand.DynamicLinkDemandAnalysisRun;
import playground.ikaddoura.analysis.linkDemand.LinkDemandAnalysisRun;

/**
* @author ikaddoura
*/

public class RunBerlinAV {

	private final static String configFile = "/Users/ihab/Documents/workspace/runs-svn/optAV/input/config_be_10pct_baseCase.xml";
	private final static boolean otfvis = false;
	
	public static void main(String[] args) {
		run();
	}

	private static void run() {
		
		Config config = ConfigUtils.loadConfig(configFile,
				new TaxiConfigGroup(),
				new TaxiFareConfigGroup(),
				new OTFVisConfigGroup(),
				new NoiseConfigGroup());
		
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
				
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
		controler.addOverridingModule(new TaxiModule(fleet));
		controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
		controler.addOverridingModule(new DynQSimModule<>(TaxiQSimProvider.class));

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
		
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModules().get(NoiseConfigGroup.GROUP_NAME);
				
		NoiseOfflineCalculation noiseCalculation = new NoiseOfflineCalculation(scenario, outputDirectory);
		noiseCalculation.run();	
		
		String outputFilePath = outputDirectory + "analysis_it." + scenario.getConfig().controler().getLastIteration() + "/";
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
		
		DynamicLinkDemandAnalysisRun analysis2 = new DynamicLinkDemandAnalysisRun(controler.getConfig().controler().getOutputDirectory());
		analysis2.run();
		
		LinkDemandAnalysisRun analysis3 = new LinkDemandAnalysisRun(controler.getConfig().controler().getOutputDirectory());
		analysis3.run();
	
	}
}

