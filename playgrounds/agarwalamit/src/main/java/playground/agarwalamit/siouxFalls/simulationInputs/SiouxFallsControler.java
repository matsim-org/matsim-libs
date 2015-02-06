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
package playground.agarwalamit.siouxFalls.simulationInputs;

import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.example.EmissionControlerListener;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFFileWriterFactory;

import playground.agarwalamit.InternalizationEmissionAndCongestion.EmissionCongestionTravelDisutilityCalculatorFactory;
import playground.agarwalamit.InternalizationEmissionAndCongestion.InternalizeEmissionsCongestionControlerListener;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;
import playground.ikaddoura.internalizationCar.WelfareAnalysisControlerListener;
import playground.vsp.congestion.controler.CongestionPricingContolerListner;
import playground.vsp.congestion.handlers.MarginalCongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author amit
 */
public class SiouxFallsControler {

	public static void main(String[] args) {

		boolean internalizeEmission = Boolean.valueOf(args [0]); //run0 false, false false; run1 true, false false; run2 false,true, false; run 3 false, false true
		boolean internalizeCongestion = Boolean.valueOf(args [1]);
		boolean both = Boolean.valueOf(args [2]);

		String configFile = args[3];

		String emissionEfficiencyFactor ="1.0";
		String considerCO2Costs = "true";
		String emissionCostFactor = "1.0";

		Config config = ConfigUtils.loadConfig(configFile);
		config.controler().setOutputDirectory(args[4]);

		//===vsp defaults
//		config.vspExperimental().setRemovingUnneccessaryPlanAttributes(true);
//		config.vspExperimental().setActivityDurationInterpretation(ActivityDurationInterpretation.tryEndTimeThenDuration.toString());
//		config.timeAllocationMutator().setMutationRange(7200.);
//		config.timeAllocationMutator().setAffectingDuration(false);
//		config.vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.ABORT);

		Controler controler = new Controler(config);

		//===emission files
		
	      EmissionsConfigGroup ecg = new EmissionsConfigGroup() ;
	      controler.getConfig().addModule(ecg);
	    
		ecg.setAverageColdEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_ColdStart_vehcat_2005average.txt");
		ecg.setAverageWarmEmissionFactorsFile("../../matsimHBEFAStandardsFiles/EFA_HOT_vehcat_2005average.txt");
		ecg.setEmissionRoadTypeMappingFile("../../siouxFalls/input/SiouxFalls_roadTypeMapping.txt");
		ecg.setEmissionVehicleFile("../../siouxFalls/input/SiouxFalls_emissionVehicles.xml");
		ecg.setUsingDetailedEmissionCalculation(false);
		//===only emission events genertaion; used with all runs for comparisons
		EmissionModule emissionModule = new EmissionModule(ScenarioUtils.loadScenario(config));
		emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();
		
		if(internalizeEmission)
		{
			//===internalization of emissions
			EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));
			EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
			controler.setTravelDisutilityFactory(emissionTducf);
			controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));
		}

		if(internalizeCongestion) 
		{
			//=== internalization of congestion
			TollHandler tollHandler = new TollHandler(controler.getScenario());
			TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			controler.addControlerListener(new CongestionPricingContolerListner(controler.getScenario(),tollHandler, new MarginalCongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl)controler.getScenario()) ));
		}
		
		if(both) {
			TollHandler tollHandler = new TollHandler(controler.getScenario());
			EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));
			EmissionCongestionTravelDisutilityCalculatorFactory emissionCongestionTravelDisutilityCalculatorFactory = new EmissionCongestionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule, tollHandler);
			controler.setTravelDisutilityFactory(emissionCongestionTravelDisutilityCalculatorFactory);
			controler.addControlerListener(new InternalizeEmissionsCongestionControlerListener(emissionModule, emissionCostModule, (ScenarioImpl) controler.getScenario(), tollHandler));
		}

		controler.setOverwriteFiles(true);
        controler.getConfig().controler().setCreateGraphs(true);
        controler.setDumpDataAtEnd(true);
		controler.addSnapshotWriterFactory("otfvis", new OTFFileWriterFactory());
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));
		
		if(Boolean.valueOf(args[0])==false && Boolean.valueOf(args[2])==false){
			controler.addControlerListener(new EmissionControlerListener());
		}
		controler.run();	

	}

}
