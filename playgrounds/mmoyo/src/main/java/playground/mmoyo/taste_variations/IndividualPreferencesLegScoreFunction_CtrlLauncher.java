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
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.cadyts.general.CadytsConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
import playground.mmoyo.io.PopSecReader;
import playground.mmoyo.utils.DataLoader;

/**
 * Starts a individualized scoring run, replaces leg scoring by a least square sense-based function scoring .
 */

public class IndividualPreferencesLegScoreFunction_CtrlLauncher {
	//public class SVD_CtrlLauncher {

	public IndividualPreferencesLegScoreFunction_CtrlLauncher( Scenario scn, final String svdSolutionsFile, boolean doZoneConversion, final double scoreWeight){
		Population pop = scn.getPopulation();
		final Network net = scn.getNetwork();
		final TransitSchedule schedule = scn.getTransitSchedule();
		
		final Controler controler = new Controler(scn);
		controler.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );

		CadytsConfigGroup ccc = new CadytsConfigGroup() ;
		controler.getConfig().addModule(ccc) ;
		
		
		//leg scoring based on data obtained with least square calculation of individual preferences 
		final Map <Id, IndividualPreferences> svdMap = new SVDValuesAsObjAttrReader(pop.getPersons().keySet()).readFile(svdSolutionsFile);
		final CharyparNagelScoringParameters params = CharyparNagelScoringParameters.getBuilder(scn.getConfig().planCalcScore()).createCharyparNagelScoringParameters(); //M
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Person person) {
			
				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				//scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));
				
				//set individualized preferences-based leg scoring function
				IndividualPreferences indValues = svdMap.get(person.getId());
				IndividualPreferencesLegScoring indPrefLegScoring = new IndividualPreferencesLegScoring(person.getSelectedPlan(), indValues, net, schedule, scoreWeight);
				scoringFunctionAccumulator.addScoringFunction(indPrefLegScoring);
 
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
		new IndividualPreferencesLegScoreFunction_CtrlLauncher(scn, solutionFile, doZoneConversion, scoreWeight);
	}
	
}