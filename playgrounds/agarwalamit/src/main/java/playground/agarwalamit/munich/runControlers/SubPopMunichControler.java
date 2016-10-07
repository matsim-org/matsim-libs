/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich.runControlers;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.example.EmissionControlerListener;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import playground.agarwalamit.InternalizationEmissionAndCongestion.EmissionCongestionTravelDisutilityCalculatorFactory;
import playground.agarwalamit.InternalizationEmissionAndCongestion.InternalizeEmissionsCongestionControlerListener;
import playground.agarwalamit.analysis.modalShare.ModalShareFromEvents;
import playground.agarwalamit.mixedTraffic.patnaIndia.input.joint.JointCalibrationControler;
import playground.agarwalamit.munich.controlerListner.MyEmissionCongestionMoneyEventControlerListner;
import playground.agarwalamit.munich.controlerListner.MyTollAveragerControlerListner;
import playground.agarwalamit.munich.utils.MunichPersonFilter;
import playground.agarwalamit.munich.utils.MunichPersonFilter.MunichUserGroup;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author amit
 */

public class SubPopMunichControler {

	public static void main(String[] args) {

		boolean offline = false;

		if(args.length==0) offline = true;

		if(offline){
			args = new String [] {
					"false",
					"false",
					"false",
					"../../../../repos/runs-svn/detEval/emissionCongestionInternalization/diss/input/config_wrappedSubActivities_usrGrp_baseCase.xml",
					"1.0",
					"../../../../repos/runs-svn/detEval/emissionCongestionInternalization/diss/output/baseCase2/",
					"false",
					"false"
			};
		}

		boolean internalizeEmission = Boolean.valueOf(args [0]); 
		boolean internalizeCongestion = Boolean.valueOf(args [1]);
		boolean internalizeBoth = Boolean.valueOf(args [2]);

		String configFile = args[3];

		String emissionEfficiencyFactor ="1.0";
		String considerCO2Costs = "true";
		String emissionCostFactor = args[4];

		String outputDir = args[5];
		boolean averageTollAfterRePlanning = Boolean.valueOf(args [6]);
		boolean writeInfoForEachPersonInEachIteration = Boolean.valueOf(args [7]);

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(outputDir);

		if(offline){
			config.network().setInputFile("network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml");
			config.plans().setInputFile("mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4_wrappedSubActivities_usrGrp.xml.gz");
			config.plans().setInputPersonAttributeFile("personsAttributes_1pct_usrGrp.xml.gz");
			config.counts().setInputFile("counts-2008-01-10_correctedSums_manuallyChanged_strongLinkMerge.xml");
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		final Controler controler = new Controler(scenario);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				String ug = MunichUserGroup.Rev_Commuter.toString();
				addPlanStrategyBinding(DefaultPlanStrategiesModule.DefaultStrategy.SubtourModeChoice.name().concat("_").concat(ug)).toProvider(new javax.inject.Provider<PlanStrategy>() {
					final String[] availableModes = {"car", "pt_".concat(ug)};
					final String[] chainBasedModes = {"car", "bike"};
					@Inject
					Scenario sc;

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new SubtourModeChoice(sc.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});
			}
		});

		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		controler.getConfig().addModule(ecg);

		ecg.setAverageColdEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_ColdStart_vehcat_2005average.txt");
		ecg.setAverageWarmEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_HOT_vehcat_2005average.txt");
		ecg.setDetailedColdEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_ColdStart_SubSegm_2005detailed.txt");
		ecg.setDetailedWarmEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_HOT_SubSegm_2005detailed.txt");
		ecg.setEmissionRoadTypeMappingFile("../../munich/input/roadTypeMapping.txt");
		controler.getConfig().vehicles().setVehiclesFile("../../munich/input/emissionVehicles_1pct.xml.gz");

		if(offline){
			ecg.setAverageColdEmissionFactorsFile("../../../input/matsimHBEFAStandardsFiles/EFA_ColdStart_vehcat_2005average.txt");
			ecg.setAverageWarmEmissionFactorsFile("../../../input/matsimHBEFAStandardsFiles/EFA_HOT_vehcat_2005average.txt");
			ecg.setDetailedColdEmissionFactorsFile("../../../input/matsimHBEFAStandardsFiles/EFA_ColdStart_SubSegm_2005detailed.txt");
			ecg.setDetailedWarmEmissionFactorsFile("../../../input/matsimHBEFAStandardsFiles/EFA_HOT_SubSegm_2005detailed.txt");
			ecg.setEmissionRoadTypeMappingFile("../../../input/munich/roadTypeMapping.txt");
			controler.getConfig().vehicles().setVehiclesFile("../../../input/munich/emissionVehicles_1pct.xml.gz");
		}

		ecg.setUsingDetailedEmissionCalculation(true);
		//===only emission events genertaion; used with all runs for comparisons
		EmissionModule emissionModule = new EmissionModule(ScenarioUtils.loadScenario(config));
		emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		if(internalizeEmission){
			// this is needed by *both* following modules:
			EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));

			// this affects the router by overwriting its generalized cost function (TravelDisutility):
			final EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, 
					emissionCostModule, config.planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(emissionTducf);
				}
			});
			controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));

		} else if(internalizeCongestion){

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, config.planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(),tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario)controler.getScenario()) ));

		} else if(internalizeBoth) {

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));
			final EmissionCongestionTravelDisutilityCalculatorFactory emissionCongestionTravelDisutilityCalculatorFactory = 
					new EmissionCongestionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule, tollHandler, config.planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(emissionCongestionTravelDisutilityCalculatorFactory);
				}
			});
			controler.addControlerListener(new InternalizeEmissionsCongestionControlerListener(emissionModule, emissionCostModule, (MutableScenario) controler.getScenario(), tollHandler));
		}

		// ride is one of network mode, thus travel disutility is required and every link must allow rider too
		Set<String> modes = new HashSet<>();
		modes.add("car");modes.add("ride");

		for(Link l : controler.getScenario().getNetwork().getLinks().values()) {
			l.setAllowedModes(modes);
		}

		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				addTravelTimeBinding("ride").to(networkTravelTime());
				addTravelDisutilityFactoryBinding("ride").to(carTravelDisutilityFactoryKey());
			}
		});

		controler.getConfig().controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		controler.getConfig().controler().setCreateGraphs(true);
		controler.getConfig().controler().setDumpDataAtEnd(true);

		if(internalizeEmission==false && internalizeBoth ==false){
			controler.addControlerListener(new EmissionControlerListener());
		}

		if(averageTollAfterRePlanning){
			controler.addControlerListener(new MyTollAveragerControlerListner());
		}

		if(writeInfoForEachPersonInEachIteration){
			// not sure for true functionality yet
			EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));
			controler.addControlerListener(new MyEmissionCongestionMoneyEventControlerListner(emissionCostModule,emissionModule));
		}

		controler.run();

		// delete unnecessary iterations folder here.
		int firstIt = controler.getConfig().controler().getFirstIteration();
		int lastIt = controler.getConfig().controler().getLastIteration();
		String OUTPUT_DIR = config.controler().getOutputDirectory();
		for (int index =firstIt+1; index <lastIt; index ++){
			String dirToDel = OUTPUT_DIR+"/ITERS/it."+index;
			Logger.getLogger(JointCalibrationControler.class).info("Deleting the directory "+dirToDel);
			IOUtils.deleteDirectory(new File(dirToDel),false);
		}

		new File(OUTPUT_DIR+"/analysis/").mkdir();
		String outputEventsFile = OUTPUT_DIR+"/output_events.xml.gz";
		// write some default analysis
		
		{
			String userGroup = MunichUserGroup.Urban.toString();
			ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new MunichPersonFilter());
			msc.run();
			msc.writeResults(OUTPUT_DIR+"/analysis/modalShareFromEvents_"+userGroup+".txt");	
		}
		{
			String userGroup = MunichUserGroup.Rev_Commuter.toString();
			ModalShareFromEvents msc = new ModalShareFromEvents(outputEventsFile, userGroup, new MunichPersonFilter());
			msc.run();
			msc.writeResults(OUTPUT_DIR+"/analysis/modalShareFromEvents_"+userGroup+".txt");
		}
	}
}