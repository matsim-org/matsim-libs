/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.ControlerConfigGroup.EventsFileFormat;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.julia.distribution.DistributionConfig;
import playground.julia.exposure.EmActivity;
import playground.julia.distribution.IntervalHandler;
import playground.julia.distribution.ResponsibilityCostModule;
import playground.julia.distribution.ResponsibilityTravelDisutilityCalculatorFactory;

import playground.vsp.emissions.EmissionModule;
import playground.vsp.emissions.example.EmissionControlerListener;

public class RunInterSample {

	/**
	 * @param args
	 */
		
		static String configFile;
		static String emissionCostFactor ="1.0";
		static String emissionEfficiencyFactor = "1.0";
		static String considerCO2Costs = "true";
		
		static String emissionInputPath = "../../runs-svn/detEval/kuhmo/input/hbefaForMatsim/";
		private static String roadTypeMappingFile = emissionInputPath + "roadTypeMapping.txt";;
		static String averageFleetWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_vehcat_2005average.txt";
		static String averageFleetColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_vehcat_2005average.txt";
		
		static boolean isUsingDetailedEmissionCalculation = false;
		static String detailedWarmEmissionFactorsFile = emissionInputPath + "EFA_HOT_SubSegm_2005detailed.txt";
		static String detailedColdEmissionFactorsFile = emissionInputPath + "EFA_ColdStart_SubSegm_2005detailed.txt";
		private static String emissionVehicleFile ="../../runs-svn/detEval/kuhmo/input/emissionVehicles_1pct.xml.gz";
		private static String emissionEventOutputFile = "output/sampleV2/emissions.xml";
		

		public static void main(String[] args) {
			//		configFile = "../../detailedEval/internalization/munich1pct/input/config_munich_1pct.xml";
			

			Config config = ConfigUtils.createConfig();
			config.addCoreModules();
//			MatsimConfigReader confReader = new MatsimConfigReader(config);
//			confReader.readFile(configFile);
			
			
			
			DistributionConfig distConfig= new DistributionConfig();
			config.network().setInputFile(distConfig.getNetworkFile());
			config.plans().setInputFile(distConfig.getPlansFile());
			
			Controler controler = new Controler(config);
			Scenario scenario = controler.getScenario();
			controler.getConfig().controler().setLastIteration(5);
			
			// controlerConfigGroup
			ControlerConfigGroup ccg = controler.getConfig().controler();
			ccg.setOutputDirectory("output/sampleV2/");
			ccg.setFirstIteration(0);
			ccg.setMobsim("qsim");
			Set<EventsFileFormat> set = new HashSet<EventsFileFormat>();
			set.add(EventsFileFormat.xml);
			ccg.setEventsFileFormats(set);
			
			
			EventsManager eventsManager = EventsUtils.createEventsManager();
			
			
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
			
			
			// define emission tool input files	
			VspExperimentalConfigGroup vcg = controler.getConfig().vspExperimental() ;
			vcg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
			vcg.setEmissionVehicleFile(emissionVehicleFile);
			
			vcg.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
			vcg.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
			
			vcg.setIsUsingDetailedEmissionCalculation(isUsingDetailedEmissionCalculation);
			vcg.setDetailedWarmEmissionFactorsFile(detailedWarmEmissionFactorsFile);
			vcg.setDetailedColdEmissionFactorsFile(detailedColdEmissionFactorsFile);
			vcg.setWritingOutputEvents(false);
			///----
			
			// emission module
			EmissionModule emissionModule = new EmissionModule(scenario);
			emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
			
			emissionModule.createLookupTables();
			emissionModule.createEmissionHandler();
			
			EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile );
			emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

			//EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));

			Double timeBinSize = distConfig.getTimeBinSize();
			Map<Id, Integer> link2xBins = distConfig.getLink2xBin();
			Map<Id, Integer> link2yBins = distConfig.getLink2yBin();
			
			IntervalHandler intervalHandler = new IntervalHandler();
			eventsManager.addHandler(intervalHandler);
					
			ArrayList<EmActivity> emActivities = new ArrayList<EmActivity>();
			intervalHandler.addActivitiesToTimetables(emActivities, link2xBins, link2yBins, distConfig.getSimulationEndTime());			
			
			ResponsibilityCostModule rcm = new ResponsibilityCostModule(emActivities, timeBinSize, link2xBins, link2yBins);
			//EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
			ResponsibilityTravelDisutilityCalculatorFactory rtdcf = new ResponsibilityTravelDisutilityCalculatorFactory(emissionModule, rcm );
			controler.setTravelDisutilityFactory(rtdcf);

			ScoringFunctionFactory respScoringFunctionFactory = new ResponisibilityScoringFunctionFactory(rcm);
			controler.setScoringFunctionFactory(respScoringFunctionFactory );
			
			controler.addControlerListener(new InternalizeResponsibilityControlerListener(emissionModule, rcm));

			controler.setOverwriteFiles(true);
			controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
			controler.addControlerListener(new EmissionControlerListener());
			controler.run();
			

	}

}
