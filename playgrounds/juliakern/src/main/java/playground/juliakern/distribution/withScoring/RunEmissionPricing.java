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
package playground.juliakern.distribution.withScoring;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import playground.benjamin.internalization.EmissionCostModule;
import playground.juliakern.distribution.ResDisFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * @author benjamin
 *
 */
public class RunEmissionPricing {
	
	static String inputPath = "../../detailedEval/emissions/testScenario/input/";
	static String networkFile = inputPath + "network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml";
//	static String plansFile = inputPath + "mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4.xml.gz";
	static String plansFile = inputPath + "smallPopulation.xml";
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
	
	public static void main(String[] args) {
		
		
		Config config = new Config();
		config.addCoreModules();
		Controler controler = new Controler(config);
		
	// controler settings	
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(false);

        // controlerConfigGroup
		ControlerConfigGroup ccg = controler.getConfig().controler();
		ccg.setOutputDirectory(outputPath);
		ccg.setFirstIteration(0);
		ccg.setLastIteration(3);
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
		StrategySettings strategySettings = new StrategySettings(Id.create("1", StrategySettings.class));
		strategySettings.setStrategyName("ChangeExpBeta");
		strategySettings.setWeight(1.0);
		scg.addStrategySettings(strategySettings);
		StrategySettings strategySettingsR = new StrategySettings(Id.create("2", StrategySettings.class));
		strategySettingsR.setStrategyName("ReRoute");
		strategySettingsR.setWeight(1.0);
		strategySettingsR.setDisableAfter(5);
		scg.addStrategySettings(strategySettingsR);
		
	// network
		NetworkConfigGroup ncg = controler.getConfig().network();
		ncg.setInputFile(networkFile);
		
	// plans
		PlansConfigGroup pcg = controler.getConfig().plans();
		pcg.setInputFile(plansFile);
		
	// define emission tool input files	
        EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
        controler.getConfig().addModule(ecg);
        ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
        ecg.setEmissionVehicleFile(emissionVehicleFile);
        
        ecg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
        ecg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
        
        ecg.setUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
        ecg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
        ecg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
	// TODO: the following does not work yet. Need to force controler to always write events in the last iteration.
		VspExperimentalConfigGroup vcg = controler.getConfig().vspExperimental() ;
		vcg.setWritingOutputEvents(false) ;
		
		EmissionControlerListener ecl = new EmissionControlerListener(controler);
        System.out.println("network at run - no of links " + controler.getScenario().getNetwork().getLinks().size());
		controler.addControlerListener(ecl);
        controler.setScoringFunctionFactory(new ResponsibilityScoringFunctionFactory(ecl, controler.getScenario()));
		
		
		EmissionModule emissionModule = ecl.emissionModule;
		EmissionCostModule emissionCostModule = new EmissionCostModule(1.0);
		// add travel disutility
		final TravelDisutilityFactory travelCostCalculatorFactory = new ResDisFactory(ecl, emissionModule, emissionCostModule, config.planCalcScore());
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bindCarTravelDisutilityFactory().toInstance(travelCostCalculatorFactory);
			}
		});
		controler.run();
	}
}