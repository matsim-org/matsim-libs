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

import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.example.EmissionControlerListener;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.otfvis.OTFVisFileWriterModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
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
public class MunichControler {

	public static void main(String[] args) {

		//		String clusterFolder = "/Users/amit/Documents/cluster/";
		//		args = new String [5];
		//		args[0] =String.valueOf("false");
		//		args[1] ="false";
		//		args[2] ="false";
		//		args[3] =clusterFolder+"/ils4/agarwal/munich/outputTest/run5/run8/baseCaseCtd/config_subActivities_baseCaseCtd_msa.xml";
		//		args[4] = "1.0";	
		//		args[5] ="/Users/amit/Documents/workspace/output/munich/baseCaseCtd/";

		boolean internalizeEmission = Boolean.valueOf(args [0]); 
		boolean internalizeCongestion = Boolean.valueOf(args [1]);
		boolean both = Boolean.valueOf(args [2]);
		String configFile = args[3];

		String emissionEfficiencyFactor ="1.0";
		String considerCO2Costs = "true";
		String emissionCostFactor = args[4];//"1.0";

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(args[5]);

		//		config.network().setInputFile("/Users/amit/Documents/workspace/input/munich/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml");
		//		config.plans().setInputFile(clusterFolder+"/ils4/agarwal/munich/outputTest/run5/run7/baseCaseCtd/ITERS/it.1400/1400.plans.xml.gz");
		//		config.counts().setCountsFileName("/Users/amit/Documents/workspace/input/munich//counts-2008-01-10_correctedSums_manuallyChanged_strongLinkMerge.xml");

		//===vsp defaults
		//		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
		//		config.vspExperimental().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration.toString());
		//		config.timeAllocationMutator().setMutationRange(7200.);
		//		config.timeAllocationMutator().setAffectingDuration(false);
		//		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.ABORT);

		Controler controler = new Controler(config);

		//===emission files

		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		controler.getConfig().addModule(ecg);

		ecg.setAverageColdEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_ColdStart_vehcat_2005average.txt");
		ecg.setAverageWarmEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_HOT_vehcat_2005average.txt");
		ecg.setDetailedColdEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_ColdStart_SubSegm_2005detailed.txt");
		ecg.setDetailedWarmEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_HOT_SubSegm_2005detailed.txt");
		ecg.setEmissionRoadTypeMappingFile("../../munich/input/roadTypeMapping.txt");
		ecg.setEmissionVehicleFile("../../munich/input/emissionVehicles_1pct.xml.gz");

		//	      	ecg.setAverageColdEmissionFactorsFile("/Users/amit/Documents/workspace/input/matsimHBEFAStandardsFiles/EFA_ColdStart_vehcat_2005average.txt");
		//			ecg.setAverageWarmEmissionFactorsFile("/Users/amit/Documents/workspace/input/matsimHBEFAStandardsFiles/EFA_HOT_vehcat_2005average.txt");
		//			ecg.setDetailedColdEmissionFactorsFile("/Users/amit/Documents/workspace/input/matsimHBEFAStandardsFiles/EFA_ColdStart_SubSegm_2005detailed.txt");
		//			ecg.setDetailedWarmEmissionFactorsFile("/Users/amit/Documents/workspace/input/matsimHBEFAStandardsFiles/EFA_HOT_SubSegm_2005detailed.txt");
		//			ecg.setEmissionRoadTypeMappingFile("/Users/amit/Documents/workspace/input/munich/roadTypeMapping.txt");
		//			ecg.setEmissionVehicleFile("/Users/amit/Documents/workspace/input/munich/emissionVehicles_1pct.xml.gz");

		ecg.setUsingDetailedEmissionCalculation(true);
		//===only emission events genertaion; used with all runs for comparisons
		EmissionModule emissionModule = new EmissionModule(ScenarioUtils.loadScenario(config));
		emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		if(internalizeEmission)
		{

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

			// this is essentially the syntax to use the randomizing router instead; needs "scheme" (which implements RoadPricingScheme); needs
			// a way to insert a new scheme in every iteration (because emissions costs change by iteration).
			//			TravelDisutilityIncludingToll.Builder travelDisutilityFactory = new TravelDisutilityIncludingToll.Builder(
			//					services.getTravelDisutilityFactory(), scheme, services.getConfig().planCalcScore().getMarginalUtilityOfMoney()
			//					) ;
			//			travelDisutilityFactory.setSigma( 3. );
			//			controler.setTravelDisutilityFactory(travelDisutilityFactory);

			// this generates money events and thus affects the scoring:
			controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
		}

		if(internalizeCongestion) 
		{
			TollHandler tollHandler = new TollHandler(controler.getScenario());
			final TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler, config.planCalcScore());
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bindCarTravelDisutilityFactory().toInstance(tollDisutilityCalculatorFactory);
				}
			});
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(),tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (MutableScenario)controler.getScenario()) ));
		}

		if(both) {
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

		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controler.getConfig().controler().setCreateGraphs(true);
		controler.getConfig().controler().setDumpDataAtEnd(true);
		controler.addOverridingModule(new OTFVisFileWriterModule());

		if(internalizeEmission==false && both==false){
			controler.addControlerListener(new EmissionControlerListener());
		}

		if(Boolean.valueOf(args [6])){
			controler.addControlerListener(new MyTollAveragerControlerListner());
		}
		if(Boolean.valueOf(args [7])){
			// not sure for true functionality yet
			EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));
			controler.addControlerListener(new MyEmissionCongestionMoneyEventControlerListner(emissionCostModule,emissionModule));
		}
		controler.run();	
	}
}