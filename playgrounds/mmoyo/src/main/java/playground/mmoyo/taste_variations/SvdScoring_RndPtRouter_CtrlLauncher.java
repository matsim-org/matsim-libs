///* *********************************************************************** *
// * project: org.matsim.*
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2013 by the members listed in the COPYING,        *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.mmoyo.taste_variations;
//
//import java.util.Map;
//
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.core.controler.Controler;
//import org.matsim.pt.router.PreparedTransitSchedule;
//import org.matsim.pt.router.TransitRouterConfig;
//import org.matsim.pt.router.TransitRouterFactory;
//import org.matsim.pt.router.TransitRouterNetwork;
//import org.matsim.pt.transitSchedule.api.TransitSchedule;
//
//import playground.mmoyo.analysis.stopZoneOccupancyAnalysis.CtrlListener4configurableOcuppAnalysis;
//import playground.mmoyo.randomizerPtRouter.RndPtRouterFactory;
//import playground.mmoyo.utils.DataLoader;
//
//public class SvdScoring_RndPtRouter_CtrlLauncher {
//
//	public static void main(String[] args) {
//		String configFile;
//		String svdSolutionFile;
//		String strDoStzopZoneConversion;
//		if (args.length>0){
//			configFile = args[0];
//			svdSolutionFile = args[1];
//			strDoStzopZoneConversion = args[2];
//		}else{
//			configFile = "../../";
//			svdSolutionFile = "../../";
//			strDoStzopZoneConversion = "false";
//		}
//		
//		//load data
//		DataLoader dataLoader = new DataLoader();
//		Scenario scn =dataLoader.loadScenario(configFile);
//		Population pop = scn.getPopulation();
//		final Network net = scn.getNetwork();
//		final TransitSchedule schedule = scn.getTransitSchedule();
//		
//		//create controler
//		final Controler controler = new Controler(scn);
//		controler.setOverwriteFiles(true);
//		
//		//create and set randomized router factory
//		final TransitRouterConfig trConfig = new TransitRouterConfig( scn.getConfig() ) ;
//		final TransitRouterNetwork routerNetwork = TransitRouterNetwork.createFromSchedule(schedule, trConfig.beelineWalkConnectionDistance);
//		final PreparedTransitSchedule preparedSchedule = new PreparedTransitSchedule(schedule);
//		TransitRouterFactory randomizedTransitRouterFactory = new RndPtRouterFactory().createFactory (preparedSchedule, trConfig, routerNetwork, false, false);
//		controler.setTransitRouterFactory(randomizedTransitRouterFactory);
//		
//		//set SVD-Scoring function
//		Map <Id, SVDvalues> svdMap = new SVDValuesAsObjAttrReader(pop.getPersons().keySet()).readFile(svdSolutionFile);
//		controler.setScoringFunctionFactory(new SVDScoringfunctionFactory(svdMap, net, schedule));
//		
//		//add analyzer for specific bus line
//		CtrlListener4configurableOcuppAnalysis ctrlListener4configurableOcuppAnalysis = new CtrlListener4configurableOcuppAnalysis(controler);
//		final boolean doStopZoneConversion = Boolean.parseBoolean(strDoStzopZoneConversion);
//		strDoStzopZoneConversion= null;
//		ctrlListener4configurableOcuppAnalysis.setStopZoneConversion(doStopZoneConversion);
//		controler.addControlerListener(ctrlListener4configurableOcuppAnalysis);
//		
//		controler.run();
//	}
//
//}
