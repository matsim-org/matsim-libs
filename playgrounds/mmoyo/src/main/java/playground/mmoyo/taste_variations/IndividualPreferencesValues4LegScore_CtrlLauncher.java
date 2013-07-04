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

package playground.mmoyo.taste_variations;

import java.io.File;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.cadyts.pt.CadytsPtConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

/**
 * Launches a standard transit simulation. Values for leg scoring are read from a object attributes
 * This class uses the sandard MATSim leg scoring, only the default values are replaced by individual preferences values
 */

public class IndividualPreferencesValues4LegScore_CtrlLauncher {
	//formerly public class SVD_CtrlLauncher4 {
	
	public IndividualPreferencesValues4LegScore_CtrlLauncher( final Scenario scn, final String svdSolutionsFile, boolean doZoneConversion, final double scoreWeight){
		
		final Controler controler = new Controler(scn);
		controler.setOverwriteFiles(true);
		
		CadytsPtConfigGroup ccc = new CadytsPtConfigGroup() ;
		controler.getConfig().addModule(CadytsPtConfigGroup.GROUP_NAME, ccc) ;
		

		//scoring 
		final Map <Id, IndividualPreferences> svdMap = new SVDValuesAsObjAttrReader(scn.getPopulation().getPersons().keySet()).readFile(svdSolutionsFile); 
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Plan plan) {
				
				//create personalized PlanCalcScoreConfigGroup with personalized values for leg scoring
				PlanCalcScoreConfigGroup persCalcScoreCnfigGroup = new PlanCalcScoreConfigGroup(); // a copy so that old/new values are safe 
				persCalcScoreCnfigGroup = scn.getConfig().planCalcScore();
				IndividualPreferences svdValues = svdMap.get(plan.getPerson().getId());      		
				
				persCalcScoreCnfigGroup.setTravelingPt_utils_hr(svdValues.getWeight_trTime());
				persCalcScoreCnfigGroup.setUtilityOfLineSwitch(svdValues.getWeight_changes());
				persCalcScoreCnfigGroup.setTravelingWalk_utils_hr(svdValues.getWeight_trWalkTime());
				persCalcScoreCnfigGroup.setMonetaryDistanceCostRatePt(svdValues.getWeight_trDistance()); //svdValues does not consider walk distance, only in vehTime!
				persCalcScoreCnfigGroup.setMarginalUtilityOfMoney(1.0); // this is because marginalUtilityOfDistancePt_m = MonetaryDistanceCostRatePt * MarginalUtilityOfMoney!!   

				CharyparNagelScoringParameters personalizedParams = new CharyparNagelScoringParameters(persCalcScoreCnfigGroup);
				
				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(personalizedParams)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(personalizedParams));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(personalizedParams, controler.getScenario().getNetwork()));
				
				//persCalcScoreCnfigGroup = null;  // For performance would be perfect, validate that it works
				//personalizedParams =null;// For performance would be perfect, validate that it works
				
				return scoringFunctionAccumulator;
			}
		}
		) ;
		////////////////////////////////////////////////////////////////////////////////////////////////
		
		//make sure plans have scores= null and that first plan is selected
		for (Person person : scn.getPopulation().getPersons().values()){
			((PersonImpl)person).setSelectedPlan(person.getPlans().get(0));
			for (Plan plan : person.getPlans()){
				plan.setScore(null);
			}
		}
		
		//add analyzer for specific bus line
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(doZoneConversion);
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);			
		
		controler.run();	
	}
	
	
	public static void main(String[] args) {
		String configFile;
		String calibrationOutputPlans;
		String strDoZoneConversion;
		double scoreWeight;
		if (args.length>0){
			configFile = args[0];
			calibrationOutputPlans = args[1];
			strDoZoneConversion = args[2];
			scoreWeight = Double.parseDouble(args[3]);
		}else{
			configFile = "../../";
			calibrationOutputPlans = "../../";
			strDoZoneConversion = "false";
			scoreWeight = 1.0;
		}
		
		//real config scenario
		DataLoader dataLoader = new DataLoader();
		Scenario scn =dataLoader.loadScenario(configFile);
		final TransitSchedule schedule = scn.getTransitSchedule();
		
		////////////////////////////////////////////////////////////////////////////////////////////////////////
		//calculate individualized values from calibration output
		DataLoader dummyDataLoader = new DataLoader();									//prepare data to parse
		ScenarioImpl dummyScn = (ScenarioImpl) dummyDataLoader.createScenario(); 
		MatsimNetworkReader matsimNetReader = new MatsimNetworkReader(dummyScn);
		matsimNetReader.readFile(scn.getConfig().network().getInputFile());
		Network dummyNet = dummyScn.getNetwork();
		
		MyLeastSquareSolutionCalculator solver = new MyLeastSquareSolutionCalculator(dummyNet, schedule, "SVD"); //calculate svd values
		PopSecReader reader = new PopSecReader(dummyScn, solver);
		reader.readFile(calibrationOutputPlans);
		
		File file = new File(calibrationOutputPlans);      					//write solutions file
		String solutionFile = file.getPath() + "SVDSolutions.xml.gz";
		solver.writeSolutionObjAttr(solutionFile);
		
		dummyDataLoader = null;
		dummyScn = null;
		matsimNetReader = null;
		dummyNet = null;
		solver = null;
		reader = null;
		file = null;
		solver = null;
		////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		
		//start svd scoring run
		boolean doZoneConversion = Boolean.parseBoolean(strDoZoneConversion);
		new IndividualPreferencesValues4LegScore_CtrlLauncher(scn, solutionFile, doZoneConversion, scoreWeight);
	}
	
}