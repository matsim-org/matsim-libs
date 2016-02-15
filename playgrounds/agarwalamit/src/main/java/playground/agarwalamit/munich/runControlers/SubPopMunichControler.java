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

import javax.inject.Provider;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.example.EmissionControlerListener;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

import playground.agarwalamit.InternalizationEmissionAndCongestion.EmissionCongestionTravelDisutilityCalculatorFactory;
import playground.agarwalamit.InternalizationEmissionAndCongestion.InternalizeEmissionsCongestionControlerListener;
import playground.agarwalamit.munich.controlerListner.MyEmissionCongestionMoneyEventControlerListner;
import playground.agarwalamit.munich.controlerListner.MyTollAveragerControlerListner;
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

		if(offline){

			args = new String [] {"false","false","false","../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/config_wrappedSubActivities_usrGrp_baseCase_msa_typDurRelative.xml"
					,"1.0","../../../../repos/runs-svn/detEval/emissionCongestionInternalization/output/1pct/run12/baseCase/","true","false"};
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
			config.network().setInputFile("../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml");
			config.plans().setInputFile("../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/mergedPopulation_All_1pct_scaledAndMode_workStartingTimePeakAllCommuter0800Var2h_gk4_wrappedSubActivities_usrGrp.xml.gz");
			config.plans().setInputPersonAttributeFile("../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/personsAttributes_1pct_usrGrp.xml.gz");
			config.counts().setCountsFileName("../../../../repos/runs-svn/detEval/emissionCongestionInternalization/input/counts-2008-01-10_correctedSums_manuallyChanged_strongLinkMerge.xml");
		}

		final Controler controler = new Controler(config);

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				addPlanStrategyBinding("SubtourModeChoice_".concat("COMMUTER_REV_COMMUTER")).toProvider(new javax.inject.Provider<PlanStrategy>() {
					String[] availableModes = {"car", "pt_COMMUTER_REV_COMMUTER"};
					String[] chainBasedModes = {"car", "bike"};

					@Override
					public PlanStrategy get() {
						final Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
						builder.addStrategyModule(new SubtourModeChoice(controler.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(controler.getScenario(), tripRouterProvider));
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
		ecg.setEmissionVehicleFile("../../munich/input/emissionVehicles_1pct.xml.gz");

		if(offline){
			ecg.setAverageColdEmissionFactorsFile("../../../input/matsimHBEFAStandardsFiles/EFA_ColdStart_vehcat_2005average.txt");
			ecg.setAverageWarmEmissionFactorsFile("../../../input/matsimHBEFAStandardsFiles/EFA_HOT_vehcat_2005average.txt");
			ecg.setDetailedColdEmissionFactorsFile("../../../input/matsimHBEFAStandardsFiles/EFA_ColdStart_SubSegm_2005detailed.txt");
			ecg.setDetailedWarmEmissionFactorsFile("../../../input/matsimHBEFAStandardsFiles/EFA_HOT_SubSegm_2005detailed.txt");
			ecg.setEmissionRoadTypeMappingFile("../../../input/munich/roadTypeMapping.txt");
			ecg.setEmissionVehicleFile("../../../input/munich/emissionVehicles_1pct.xml.gz");
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
			final EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(emissionTducf);
				}
			});
			controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));

		} else if(internalizeCongestion){

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
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
			final EmissionCongestionTravelDisutilityCalculatorFactory emissionCongestionTravelDisutilityCalculatorFactory = new EmissionCongestionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule, tollHandler);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(emissionCongestionTravelDisutilityCalculatorFactory);
				}
			});
			controler.addControlerListener(new InternalizeEmissionsCongestionControlerListener(emissionModule, emissionCostModule, (MutableScenario) controler.getScenario(), tollHandler));

		}

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
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
	}
}