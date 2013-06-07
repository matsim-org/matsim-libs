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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionAccumulator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelActivityScoring;
import org.matsim.core.scoring.functions.CharyparNagelAgentStuckScoring;
import org.matsim.core.scoring.functions.CharyparNagelLegScoring;
import org.matsim.core.scoring.functions.CharyparNagelScoringParameters;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterNetwork;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
import playground.mmoyo.randomizerPtRouter.RndPtRouterFactory;

public class SvdScoring_AndAccum_RndPtRouter_CtrlLauncher {

	public static void main(String[] args) {
		String configFile;
		String svdSolutionFile;
		String strIsStopZoneConversion; 
		if (args.length>0){
			configFile = args[0];
			svdSolutionFile = args[1];
			strIsStopZoneConversion = args[2];
		}else{
			configFile = "../../ptManuel/calibration/my_config.xml";
			svdSolutionFile = "../../input/choiceM44/500.cadytsCorrectionsNsvdValues.xml.gz";
			strIsStopZoneConversion ="false"; // "true"  , "false"
		}
		
		boolean isStopZoneConversion = Boolean.parseBoolean(strIsStopZoneConversion);
		final Config config = ConfigUtils.loadConfig(configFile) ;
		//final double beta=30. ;    //Caution: so was calibrated most of the runs!!!
		
		configFile= null; //M
		
		//set the controler
		final Scenario scn = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scn);
		controler.setOverwriteFiles(true) ;
		
		//load data
		//DataLoader dataLoader = new DataLoader();
		//Scenario scn =dataLoader.loadScenario(configFile);
		//Population pop = scn.getPopulation();
		//final Network net = scn.getNetwork();
		//final TransitSchedule schedule = scn.getTransitSchedule();
		
		//create controler
		//final Controler controler = new Controler(scn);
		//controler.setOverwriteFiles(true);
		
		//create and set randomized router factory
		final TransitSchedule schedule = controler.getScenario().getTransitSchedule();
		final TransitRouterConfig trConfig = new TransitRouterConfig( scn.getConfig() ) ;
		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.beelineWalkConnectionDistance);
		final PreparedTransitSchedule preparedSchedule = new PreparedTransitSchedule(schedule);
		TransitRouterFactory randomizedTransitRouterFactory = new RndPtRouterFactory().createFactory (preparedSchedule, trConfig, routerNetwork, false, false);
		controler.setTransitRouterFactory(randomizedTransitRouterFactory);
		
		
		//set scoring functions
		final CharyparNagelScoringParameters params = new CharyparNagelScoringParameters(config.planCalcScore()); //M
		final Population pop = scn.getPopulation();
		final Network net = scn.getNetwork();
		final Map <Id, SVDvalues> svdMap = new SVDValuesAsObjAttrReader(pop.getPersons().keySet()).readFile(svdSolutionFile);
		svdSolutionFile = null; //M
		
		controler.setScoringFunctionFactory(new ScoringFunctionFactory() {
			@Override
			public ScoringFunction createNewScoringFunction(Plan plan) {
				//if (params == null) {																			//<- see comment about performance improvement in 
				//	params = new CharyparNagelScoringParameters(config.planCalcScore()); //org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory.createNewScoringFunction
				//}

				ScoringFunctionAccumulator scoringFunctionAccumulator = new ScoringFunctionAccumulator();
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelLegScoring(params, controler.getScenario().getNetwork()));
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelActivityScoring(params)) ;
				scoringFunctionAccumulator.addScoringFunction(new CharyparNagelAgentStuckScoring(params));

				//set SVD-Scoring function
				SVDvalues svdValues = svdMap.get(plan.getPerson().getId());
				SVDscoring svdScoring = new SVDscoring(plan, svdValues, net, schedule);
				scoringFunctionAccumulator.addScoringFunction(svdScoring);
 
				return scoringFunctionAccumulator;
			}
		}) ;
		
		//add analyzer for specific bus line
		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(isStopZoneConversion);
		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);
		
		controler.run();
	}

}
