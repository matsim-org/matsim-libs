/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOnline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.julia.distribution.scoring;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import playground.julia.distribution.GeneratedEmissionsHandler;
import playground.julia.distribution.IntervalHandler;
import playground.julia.distribution.ResponsibilityScoringFunctionFactory;
import playground.julia.distribution.SpatialAveragingDistribution;
import playground.julia.exposure.ResponsibilityEvent;
import playground.vsp.emissions.example.EmissionControlerListener;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;

/**
 * @author benjamin
 *
 */
public class RunEmissionToolOnline {
	
	static String inputPath = "../../detailedEval/emissions/testScenario/input/";
	static String networkFile = inputPath + "network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";
	static String plansFile = inputPath + "mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
//	static String plansFile = inputPath + "mergedPopulation_All_10pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
	
	static String emissionInputPath = "../../detailedEval/emissions/hbefaForMatsim/";
	static String roadTypeMappingFile = emissionInputPath + "roadTypeMapping.txt";
	static String emissionVehicleFile = inputPath + "emissionVehicles_1pct.xml.gz";
//	static String emissionVehicleFile = inputPath + "emissionVehicles_10pct.xml.gz";
	
	static String averageFleetWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_vehcat_2005average.txt";
	static String averageFleetColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_vehcat_2005average.txt";
	
	static boolean isUsingDetailedEmissionCalculation = true;
	static String detailedWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_SubSegm_2005detailed.txt";
	static String detailedColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_SubSegm_2005detailed.txt";
	
	static String outputPath = "../../detailedEval/emissions/testScenario/output/";
	private static ArrayList<ResponsibilityEvent> allRevents;
	
	public static void main(String[] args) {
		Config config = new Config();
		config.addCoreModules();
		Controler controler = new Controler(config);
		
	// controler settings	
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		
	// controlerConfigGroup
		ControlerConfigGroup ccg = controler.getConfig().controler();
		ccg.setOutputDirectory(outputPath);
		ccg.setFirstIteration(0);
		ccg.setLastIteration(0);
		ccg.setMobsim("qsim");
		Set set = new HashSet();
		set.add(EventsFileFormat.xml);
		ccg.setEventsFileFormats(set);
//		ccg.setRunId("321");
		
	// qsimConfigGroup
		QSimConfigGroup qcg = controler.getConfig().qsim();
		qcg.setStartTime(0 * 3600.);
		qcg.setEndTime(30 * 3600.);
		qcg.setFlowCapFactor(0.1);
		qcg.setStorageCapFactor(0.3);
//		qcg.setFlowCapFactor(0.01);
//		qcg.setStorageCapFactor(0.03);
		qcg.setNumberOfThreads(1);
		qcg.setRemoveStuckVehicles(false);
		qcg.setStuckTime(10.0);
		
	// planCalcScoreConfigGroup

		PlanCalcScoreConfigGroup pcs = controler.getConfig().planCalcScore();
		Set<String> activities = new HashSet<String>();
		activities.add("unknown");
		activities.add("work");
		activities.add("pickup");
		activities.add("with adult");
		activities.add("other");
		activities.add("pvWork");
		activities.add("pvHome");
		activities.add("gvHome");
		activities.add("education");
		activities.add("business");
		activities.add("shopping");
		activities.add("private");
		activities.add("leisure");
		activities.add("sports");
		activities.add("home");
		activities.add("friends");
		
		for(String activity : activities){
			ActivityParams params = new ActivityParams(activity);
			params.setTypicalDuration(30 * 3600);
			pcs.addActivityParams(params);
		}

	// strategy
		StrategyConfigGroup scg = controler.getConfig().strategy();
		StrategySettings strategySettings = new StrategySettings(new IdImpl("1"));
		strategySettings.setModuleName("ChangeExpBeta");
		strategySettings.setProbability(1.0);
		scg.addStrategySettings(strategySettings);
		
	// network
		NetworkConfigGroup ncg = controler.getConfig().network();
		ncg.setInputFile(networkFile);
		
	// plans
		PlansConfigGroup pcg = controler.getConfig().plans();
		pcg.setInputFile(plansFile);
		
		
	// define emission tool input files	
		VspExperimentalConfigGroup vcg = controler.getConfig().vspExperimental() ;
		vcg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
		vcg.setEmissionVehicleFile(emissionVehicleFile);
		
		vcg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
		vcg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
		
		vcg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
		vcg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
		vcg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
		
	// TODO: the following does not work yet. Need to force controler to always write events in the last iteration.
		vcg.setWritingOutputEvents(false) ;
		EventHandler intervalHandler = new IntervalHandler();
		controler.getEvents().addHandler(intervalHandler);
		Map<Id, Integer> link2xbins = null;
		Map<Id, Integer> link2ybins = null;
		EventHandler generatedEmissionsHandler = new GeneratedEmissionsHandler(0.0, 3000., link2xbins, link2ybins, WarmPollutant.NO2, ColdPollutant.NO2);
		SpatialAveragingDistribution sad = new SpatialAveragingDistribution();
//		allRevents = sad.getResponsibilityEvents();
		ScoringFunctionFactory factory = new ResponsibilityScoringFunctionFactory(config, controler.getNetwork(), allRevents);
		controler.setScoringFunctionFactory(factory);
		controler.getEvents().addHandler(generatedEmissionsHandler);
		controler.addControlerListener(new EmissionControlerListener());
		controler.run();
	}
}