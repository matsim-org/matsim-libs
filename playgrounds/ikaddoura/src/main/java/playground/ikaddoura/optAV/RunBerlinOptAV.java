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
import org.matsim.contrib.noise.NoiseCalculationOnline;
import org.matsim.contrib.noise.NoiseConfigGroup;
import org.matsim.contrib.noise.data.NoiseContext;
import org.matsim.contrib.noise.utils.MergeNoiseCSVFile;
import org.matsim.contrib.noise.utils.ProcessNoiseImmissions;
import org.matsim.contrib.taxi.run.TaxiConfigConsistencyChecker;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import playground.ikaddoura.analysis.detailedPersonTripAnalysis.PersonTripNoiseAnalysisMain;
import playground.ikaddoura.decongestion.DecongestionConfigGroup;
import playground.ikaddoura.decongestion.DecongestionConfigGroup.TollingApproach;
import playground.ikaddoura.decongestion.DecongestionControlerListener;
import playground.ikaddoura.decongestion.data.DecongestionInfo;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollSetting;
import playground.ikaddoura.decongestion.tollSetting.DecongestionTollingPID;
import playground.ikaddoura.moneyTravelDisutility.MoneyEventAnalysis;

/**
* @author ikaddoura
*/

public class RunBerlinOptAV {

	private final static String configFile = "/Users/ihab/Documents/workspace/runs-svn/optAV/input/config_be_10pct_optAV.xml";
			
	public static void main(String[] args) {
		run();
	}

	private static void run() {
		
		Config config = ConfigUtils.loadConfig(configFile,
				new TaxiConfigGroup(),
				new TaxiFareConfigGroup(),
				new NoiseConfigGroup());
		
		TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
		config.addConfigConsistencyChecker(new TaxiConfigConsistencyChecker());
		config.checkConsistency();
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
				
		// #############################
		// noise pricing
		// #############################

		controler.addControlerListener(new NoiseCalculationOnline(new NoiseContext(controler.getScenario())));
		
		// #############################
		// congestion pricing
		// #############################

		final DecongestionConfigGroup decongestionSettings = new DecongestionConfigGroup();
		decongestionSettings.setTOLLING_APPROACH(TollingApproach.PID);
		decongestionSettings.setKp(-12./3600.);
		decongestionSettings.setKi(0.);
		decongestionSettings.setKd(0.);
		decongestionSettings.setMsa(true);
		decongestionSettings.setRUN_FINAL_ANALYSIS(false);
		decongestionSettings.setWRITE_LINK_INFO_CHARTS(false);
		
		final DecongestionInfo info = new DecongestionInfo(controler.getScenario(), decongestionSettings);
		final DecongestionTollSetting tollSettingApproach = new DecongestionTollingPID(info);	
		
		final DecongestionControlerListener decongestion = new DecongestionControlerListener(info, tollSettingApproach);		
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addControlerListenerBinding().toInstance(decongestion);
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
		controler.addOverridingModule(new TaxiModule(fleet));
		controler.addOverridingModule(VrpTravelTimeModules.createTravelTimeEstimatorModule(0.05));
		controler.addOverridingModule(new DynQSimModule<>(OptAVQSimProvider.class));
		
		controler.addOverridingModule(new AbstractModule(){
			@Override
			public void install() {
												
				this.bind(MoneyEventAnalysis.class).asEagerSingleton();
				this.addControlerListenerBinding().to(MoneyEventAnalysis.class);
				this.addEventHandlerBinding().to(MoneyEventAnalysis.class);
			}
		}); 

		// #############################
		// run
		// #############################
		
		controler.run();
		
		// #############################
		// analysis
		// #############################
		
		String immissionsDir = controler.getConfig().controler().getOutputDirectory() + "/ITERS/it." + controler.getConfig().controler().getLastIteration() + "/immissions/";
		String receiverPointsFile = controler.getConfig().controler().getOutputDirectory() + "/receiverPoints/receiverPoints.csv";
		
		NoiseConfigGroup noiseParams = (NoiseConfigGroup) controler.getConfig().getModules().get(NoiseConfigGroup.GROUP_NAME);

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
		
		PersonTripNoiseAnalysisMain analysis = new PersonTripNoiseAnalysisMain(controler.getConfig().controler().getOutputDirectory());
		analysis.run();
	}
}

