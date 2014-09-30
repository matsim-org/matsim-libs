package playground.juliakern.responsibilityOffline;
///* *********************************************************************** *
// * project: org.matsim.*
// * SpatialAveragingForLinkEmissions.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
//package playground.julia.responsibilityOffline;
//
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//import org.apache.log4j.Logger;
//import org.matsim.api.core.v01.Coord;
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.network.Network;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.contrib.emissions.events.EmissionEventsReader;
//import org.matsim.contrib.emissions.types.ColdPollutant;
//import org.matsim.contrib.emissions.types.WarmPollutant;
//import org.matsim.core.api.experimental.events.EventsManager;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.config.MatsimConfigReader;
//import org.matsim.core.controler.Controler;
//import org.matsim.core.controler.events.ScoringEvent;
//import org.matsim.core.controler.events.StartupEvent;
//import org.matsim.core.events.EventsUtils;
//import org.matsim.core.events.MatsimEventsReader;
//import org.matsim.core.events.handler.EventHandler;
//import org.matsim.core.population.MatsimPopulationReader;
//import org.matsim.core.scenario.ScenarioUtils;
//
//import playground.benjamin.internalization.EmissionCostModule;
//import playground.benjamin.scenarios.munich.analysis.filter.LocationFilter;
//import playground.julia.distribution.GridTools;
//import playground.julia.distribution.withScoringFast.EmissionControlerListener;
//import playground.julia.newInternalization.IntervalHandler;
//import playground.julia.newSpatialAveraging.SpatialAveragingWriter;
//import playground.julia.spatialAveraging.SimpleWarmEmissionEventHandler;
//import playground.julia.spatialAveraging.SpatialAveragingUtils;
//import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
//import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;
//
///**
// * @author julia, benjamin
// *
// */
//public class SpatialAveragingWelfare {
//	private static final Logger logger = Logger.getLogger(SpatialAveragingWelfare.class);
//
//	final double scalingFactor = 100.;
//	private final static String runNumber1 = "baseCase";
//	private final static String runDirectory1 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_baseCase_ctd/";
////	private final static String runNumber2 = "zone30";
////	private final static String runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_zone30/";
//	private final static String runNumber2 = "exposurePricing";
////	private final static String runDirectory2 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_policyCase_exposurePricing/";
//	private final static String runDirectory2 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_policyCase_zone30/";
////	private final String netFile1 = runDirectory2 + "output_network.xml.gz";
//	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
//	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
////
//	private static String configFile1 = runDirectory1 + "output_config.xml.gz";
//	//private final static Integer lastIteration1 = getLastIteration(configFile1);
//	private final static Integer lastIteration1 = 1500;
//	private static String configFile2 = runDirectory1 + "output_config.xml.gz";
//	private final static Integer lastIteration2 = 1500;
////	private final static Integer lastIteration2 = getLastIteration(configFile2);
//	private final String plansFile1 = runDirectory1 + "ITERS/it.1500/1500.plans.xml";
//	private final String plansFile2 = runDirectory2 + "ITERS/it.1500/1500.plans.xml";
//	private final String emissionFile1 = runDirectory1 + "ITERS/it.1500/1500.emission.events.xml.gz";
//	private final String emissionFile2 = runDirectory2 + "ITERS/it.1500/1500.emission.events.xml.gz";
//	private final String eventsFile1 = runDirectory1 + "ITERS/it.1500/1500.events.xml.gz";
//	private final String eventsFile2 = runDirectory2 + "ITERS/it.1500/1500.events.xml.gz";
//	
////	final double scalingFactor = 10.;
////	private final static String runNumber1 = "981";
////	private final static String runNumber2 = "983";
////	private final static String runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
////	private final static String runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
////	private final String netFile1 = runDirectory1 + runNumber1 + ".output_network.xml.gz";
////	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
//
////	private static String configFile1 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
////	private final static Integer lastIteration1 = getLastIteration(configFile1);
////	private final String plansFile1 = runDirectory1 + runNumber1 + ".output_plans.xml";
////	private final String plansFile2 = runDirectory2 + runNumber2 + ".output_plans.xml.gz";
//
//	final double xMin = 4452550.25;
//	final double xMax = 4479483.33;
//	final double yMin = 5324955.00;
//	final double yMax = 5345696.81;
//	
//	final int noOfXbins = 160;
//	final int noOfYbins = 120;
//
//	private Double timeBinSize = 60*60.;
//	private Double simulationEndTime = 30*60*60.;
//	
//	final double smoothingRadius_m = 1000.;
////	final double area_in_smoothing_circle_sqkm = (Math.PI * this.smoothingRadius_m * this.smoothingRadius_m) / (1000. * 1000.);
//	
//	final boolean compareToBaseCase = true;
//	
//	SpatialAveragingUtils sau;
//	LocationFilter lf;
//	Network network;
//
//	String outPathStub = runDirectory1 + "analysis/spatialAveraging/welfare/";
////	String outPathStub = "./output/analysis/";
//
//	private double emissionCostFactor = 1.0;
//
//	private boolean substractAverageValue = false; // else: no refund (todo? personal refund - not here people didnt behave like they paid the toll)
//
//	private Double marUtilOfMoney = 1.0;
//
//	private boolean baseCaseIsInternalization=false;
//
//	private boolean compareCaseIsInternalization =true;
//
//	private SpatialAveragingWriter saWriter;
//
//	private void run() throws IOException{
//		this.sau = new SpatialAveragingUtils(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, munichShapeFile, null);
//		this.saWriter = new SpatialAveragingWriter(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, munichShapeFile, null);
//		this.lf = new LocationFilter();
//		
//		Scenario scenario = loadScenario(netFile1);
//		this.network = scenario.getNetwork();		
//		MatsimPopulationReader mpr = new MatsimPopulationReader(scenario);
//		mpr.readFile(plansFile1);
//		
//		Config config = scenario.getConfig();
//		Population pop = scenario.getPopulation();
//		Controler controler=  new Controler(config);
//		
//		// map links to cells
//		GridTools gt = new GridTools(network.getLinks(), xMin, xMax, yMin, yMax);
//		Map<Id, Integer> link2xbins = gt.mapLinks2Xcells(noOfXbins);
//		Map<Id, Integer> link2ybins = gt.mapLinks2Ycells(noOfYbins);
//		
//		// calc durations
//		IntervalHandler intervalHandler = new IntervalHandler(timeBinSize, simulationEndTime, noOfXbins, noOfYbins, link2xbins, link2ybins); 
//				//new IntervalHandler(timeBinSize, simulationEndTime, noOfXbins, noOfYbins, link2xbins, link2ybins, gt, network);
//		intervalHandler.reset(0);
//		intervalHandler.reset(0);
//		EventsManager eventsManager = EventsUtils.createEventsManager();
//		eventsManager.addHandler(intervalHandler);
//		MatsimEventsReader mer = new MatsimEventsReader(eventsManager);
//		mer.readFile(eventsFile1);
//		HashMap<Double, Double[][]> durations = intervalHandler.getDuration();
//		//eventsManager.removeHandler(intervalHandler);
//		
//		
//		// calc emission costs
//		EmissionCostDensityHandler ecdh = new EmissionCostDensityHandler(durations, link2xbins, link2ybins);
//		eventsManager.addHandler(ecdh);
//		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
//		emissionReader.parse(emissionFile1);
//		Map<Id, Double> person2causedEmCosts = ecdh.getPerson2causedEmCosts();
//		
//		// TODO wenn das die internalisierung ist, bekommen die agenten ihre jeweils bezahlten kosten zurueck???
//		
//		// recalc score
//		for(Id person: person2causedEmCosts.keySet()){
//			Plan plan = pop.getPersons().get(person).getSelectedPlan();
////			System.out.println(plan.getScore());
//			plan.setScore(plan.getScore()-person2causedEmCosts.get(person)*marUtilOfMoney);
////			System.out.println(plan.getScore());
//		}
////		
////		// calculate paid toll per person
////		Map<Id, Double> personId2paidToll = new HashMap<Id, Double>();
////		EventsManager eventsManager = EventsUtils.createEventsManager();
////		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
////		
////		SimpleWarmEmissionEventHandler weeh = new SimpleWarmEmissionEventHandler();
////		eventsManager.addHandler(weeh);
//////		SimpleColdEmissionEventHandler ceeh = new SimpleColdEmissionEventHandler();
//////		eventsManager.addHandler(ceeh);
////		emissionReader.parse(emissionFile1);
////		
////		Map<Id, Map<WarmPollutant, Double>> personId2warmEmissions = weeh.getPersonId2warmEmissions();
////		Map<Id, Map<ColdPollutant, Double>> personId2coldEmissions = ceeh.getPersonId2coldEmissions();
////		EmissionCostModule emissionCostModule = new EmissionCostModule(emissionCostFactor );
//		
////		if(baseCaseIsInternalization){
////			for(Id personId: personId2warmEmissions.keySet()){
////			Map<WarmPollutant, Double> warmEmissions = personId2warmEmissions.get(personId);
////			Double personalEmissionCost = emissionCostModule.calculateWarmEmissionCosts(warmEmissions);
////			
////			Plan selPlan = pop.getPersons().get(personId).getSelectedPlan();
////			selPlan.setScore(selPlan.getScore()+personalEmissionCost*marUtilOfMoney);
////			}
////		}
//		
//		UserBenefitsCalculator ubc = new UserBenefitsCalculator(config, WelfareMeasure.LOGSUM, false);
//		ubc.calculateUtility_money(pop);
//		Map<Id, Double> personId2Utility = ubc.getPersonId2MonetizedUtility();
//		
////		for(Id personId : personId2Utility.keySet()){
////			logger.info(personId.toString() + " has utility " + personId2Utility.get(personId) +" and caused em costs" + person2causedEmCosts.get(personId));
////		}
//		
//		// subtract average/personal toll value from monetized utility
//		if(substractAverageValue ){
//			// calculate average value
//			Double totalPaidTolls = 0.0;
//			for(Id personId: person2causedEmCosts.keySet()){
//				totalPaidTolls += person2causedEmCosts.get(personId);
//			}
//			Double averagePaidToll = totalPaidTolls/personId2Utility.size();
//			
//			for(Id personId: personId2Utility.keySet()){
//				//System.out.println("person " + personId.toString() + " caused em costs " + person2causedEmCosts.get(personId));
//				personId2Utility.put(personId, personId2Utility.get(personId)-averagePaidToll*marUtilOfMoney);
//			}
//		}	
////		}else{ // personal costs
////			for(Id personId: personId2Utility.keySet())
////			if(personId2paidToll.containsKey(personId)){
////				personId2Utility.put(personId, personId2Utility.get(personId)-personId2paidToll.get(personId));
////			}
////		}
//		
//		
//		logger.info("There were " + ubc.getPersonsWithoutValidPlanCnt() + " persons without any valid plan.");
//		
//		double [][] weightsBaseCase = calculateWeights(personId2Utility, pop);
//		double [][] normalizedWeightsBaseCase = this.sau.normalizeArray(weightsBaseCase);
//		
//		double [][] userBenefitsBaseCase = fillUserBenefits(personId2Utility, pop);
//		double [][] normalizedUserBenefitsBaseCase = this.sau.normalizeArray(userBenefitsBaseCase);
//		
//		double [][] averageUserBenefitsBaseCase = calculateAverage(normalizedUserBenefitsBaseCase, normalizedWeightsBaseCase);
//		
//		this.saWriter.writeRoutput(normalizedUserBenefitsBaseCase, outPathStub + runNumber1 + "." + lastIteration1 + ".Routput." + "UserBenefits.txt");
//		this.saWriter.writeRoutput(averageUserBenefitsBaseCase, outPathStub + runNumber1 + "." + lastIteration1 + ".Routput." + "UserBenefitsAverage.txt");
////		this.sau.writeRoutput(normalizedWeightsBaseCase, outPathStub + runNumber1 + "." + lastIteration1 + ".Routput." + "NormalizedWeightsBaseCase.txt");
//		
//		Double totalWelfare = 0.0;
//		for(Double wfa : ubc.getPersonId2MonetizedUtility().values()){
//			totalWelfare += wfa;
//		}
//		Double averageWelfare = totalWelfare/ubc.getPersonId2MonetizedUtility().size();
//		
//		logger.info("base case welfare total" +totalWelfare + " base case avg Welfare" + averageWelfare);
//		
//		if(compareToBaseCase){
//			
//			Scenario scenario2 = loadScenario(netFile1);
//			this.network = scenario2.getNetwork();		
//			MatsimPopulationReader mpr2 = new MatsimPopulationReader(scenario2);
//			mpr2.readFile(plansFile2);
//		
//			Config config2 = scenario2.getConfig();
//			Population pop2 = scenario2.getPopulation();
//			Controler controler2=  new Controler(config2);
//		
//		// map links to cells
//			GridTools gt2 = new GridTools(network.getLinks(), xMin, xMax, yMin, yMax);
//			Map<Id, Integer> link2xbins2 = gt2.mapLinks2Xcells(noOfXbins);
//			Map<Id, Integer> link2ybins2 = gt2.mapLinks2Ycells(noOfYbins);
//		
//		// calc durations
//			IntervalHandler intervalHandler2 = new IntervalHandler(timeBinSize, simulationEndTime, noOfXbins, noOfYbins, link2xbins2, link2ybins2); 
//					//new IntervalHandler(timeBinSize, simulationEndTime, noOfXbins, noOfYbins, link2xbins2, link2ybins2, gt2, network);
//			intervalHandler2.reset(0);
//			EventsManager eventsManager2 = EventsUtils.createEventsManager();
//			eventsManager2.addHandler(intervalHandler2);
//			MatsimEventsReader mer2 = new MatsimEventsReader(eventsManager2);
//			mer2.readFile(eventsFile2);
//			HashMap<Double, Double[][]> durations2 = intervalHandler2.getDuration();
//		//	eventsManager.removeHandler(intervalHandler);
//		
//
//		
//		// calc emission costs
//		EmissionCostDensityHandler ecdh2 = new EmissionCostDensityHandler(durations2, link2xbins2, link2ybins2);
//		eventsManager2.addHandler(ecdh2);
//		EmissionEventsReader emissionReader2 = new EmissionEventsReader(eventsManager2);
//		emissionReader2.parse(emissionFile2);
//		Map<Id, Double> person2causedEmCosts2 = ecdh2.getPerson2causedEmCosts();
//		
//		// TODO wenn das die internalisierung ist, bekommen die agenten ihre jeweils bezahlten kosten zurueck???
//		
//		// recalc score
//		for(Id person: person2causedEmCosts2.keySet()){
//			Plan plan = pop2.getPersons().get(person).getSelectedPlan(); //TODO test wheter positive/negative
//			plan.setScore(plan.getScore()-person2causedEmCosts2.get(person)*marUtilOfMoney);
//		}
////		
////		// calculate paid toll per person
////		Map<Id, Double> personId2paidToll = new HashMap<Id, Double>();
////		EventsManager eventsManager = EventsUtils.createEventsManager();
////		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
////		
////		SimpleWarmEmissionEventHandler weeh = new SimpleWarmEmissionEventHandler();
////		eventsManager.addHandler(weeh);
//////		SimpleColdEmissionEventHandler ceeh = new SimpleColdEmissionEventHandler();
//////		eventsManager.addHandler(ceeh);
////		emissionReader.parse(emissionFile1);
////		
////		Map<Id, Map<WarmPollutant, Double>> personId2warmEmissions = weeh.getPersonId2warmEmissions();
////		Map<Id, Map<ColdPollutant, Double>> personId2coldEmissions = ceeh.getPersonId2coldEmissions();
////		EmissionCostModule emissionCostModule = new EmissionCostModule(emissionCostFactor );
//		
////		if(baseCaseIsInternalization){
////			for(Id personId: personId2warmEmissions.keySet()){
////			Map<WarmPollutant, Double> warmEmissions = personId2warmEmissions.get(personId);
////			Double personalEmissionCost = emissionCostModule.calculateWarmEmissionCosts(warmEmissions);
////			
////			Plan selPlan = pop.getPersons().get(personId).getSelectedPlan();
////			selPlan.setScore(selPlan.getScore()+personalEmissionCost*marUtilOfMoney);
////			}
////		}
//		
//		UserBenefitsCalculator ubc2 = new UserBenefitsCalculator(config, WelfareMeasure.LOGSUM, false);
//		ubc2.calculateUtility_money(pop2);
//		Map<Id, Double> personId2Utility2 = ubc2.getPersonId2MonetizedUtility();
//		
//		// subtract average/personal toll value from monetized utility
//		if(substractAverageValue ){
//			// calculate average value
//			Double totalPaidTolls = 0.0;
//			for(Id personId: person2causedEmCosts2.keySet()){
//				totalPaidTolls += person2causedEmCosts2.get(personId);
//			}
//			Double averagePaidToll2 = totalPaidTolls/personId2Utility2.size();
//			
//			for(Id personId: personId2Utility2.keySet()){
//				personId2Utility2.put(personId, personId2Utility.get(personId)-averagePaidToll2*marUtilOfMoney);
//			}
//		}	
////		}else{ // personal costs
////			for(Id personId: personId2Utility.keySet())
////			if(personId2paidToll.containsKey(personId)){
////				personId2Utility.put(personId, personId2Utility.get(personId)-personId2paidToll.get(personId));
////			}
////		}
////			 */
////			Scenario scenario2 = loadScenario(netFile1);
////			MatsimPopulationReader mpr2 = new MatsimPopulationReader(scenario2);
////			mpr2.readFile(plansFile2);
////			
////			Config config2 = scenario2.getConfig();
////			Population pop2 = scenario2.getPopulation();
////			
////			Controler controler2=  new Controler(config2);
////			EmissionControlerListener ecl2 = new EmissionControlerListener(controler2, network, eventsFile2, emissionFile2);
////		
////			StartupEvent event2 = new StartupEvent(controler2);
////			ecl2.notifyStartup(event2);
////			ScoringEvent scoringEvent2 = new ScoringEvent(controler2, 0);
////			ecl2.notifyScoring(scoringEvent2);
////			Map<Id, Double> causedEmCosts2 = ecl2.getCausedEmCosts();
////		
////			// recalc score
////			for(Id person: causedEmCosts2.keySet()){
////				Plan plan = pop2.getPersons().get(person).getSelectedPlan();
////				plan.setScore(plan.getScore()-causedEmCosts2.get(person)*marUtilOfMoney );
////			}
////			
////			// calculate paid toll per person
////			Map<Id, Double> personId2paidToll2 = new HashMap<Id, Double>();
////			EventsManager eventsManager2 = EventsUtils.createEventsManager();
////			EmissionEventsReader emissionReader2 = new EmissionEventsReader(eventsManager2);
////		
////			SimpleWarmEmissionEventHandler weeh2 = new SimpleWarmEmissionEventHandler();
////			eventsManager.addHandler(weeh2);
////			//		SimpleColdEmissionEventHandler ceeh2 = new SimpleColdEmissionEventHandler();
////			//		eventsManager2.addHandler(ceeh2);
////			emissionReader2.parse(emissionFile2);
////		
////			Map<Id, Map<WarmPollutant, Double>> personId2warmEmissions2 = weeh2.getPersonId2warmEmissions();
//////		Map<Id, Map<ColdPollutant, Double>> personId2coldEmissions = ceeh.getPersonId2coldEmissions();
////			EmissionCostModule emissionCostModule2 = new EmissionCostModule(emissionCostFactor );
////		
////			if(compareCaseIsInternalization){
////				for(Id personId: personId2warmEmissions2.keySet()){
////					Map<WarmPollutant, Double> warmEmissions = personId2warmEmissions2.get(personId);
////					Double personalEmissionCost = emissionCostModule2.calculateWarmEmissionCosts(warmEmissions);
////			
////					Plan selPlan = pop2.getPersons().get(personId).getSelectedPlan();
////					selPlan.setScore(selPlan.getScore()+personalEmissionCost*marUtilOfMoney);
////				}
////			}
////			 
////			
////			UserBenefitsCalculator ubc2 = new UserBenefitsCalculator(config2, WelfareMeasure.LOGSUM, false);
////			ubc2.calculateUtility_money(pop2);
////			Map<Id, Double> personId2Utility2 = ubc2.getPersonId2MonetizedUtility();
//			logger.info("There were " + ubc2.getPersonsWithoutValidPlanCnt() + " persons without any valid plan.");
//			
////			if(substractAverageValue ){
////				// calculate average value
////				Double totalPaidTolls = 0.0;
////				for(Id personId: causedEmCosts2.keySet()){
////					totalPaidTolls += causedEmCosts2.get(personId);
////				}
////				Double averagePaidToll = totalPaidTolls/personId2Utility2.size();
////				
////				for(Id personId: personId2Utility2.keySet()){
////					personId2Utility2.put(personId, personId2Utility2.get(personId)-averagePaidToll*marUtilOfMoney);
////				}
////			}	
//			
//			
//			double [][] weightsPolicyCase = calculateWeights(personId2Utility2, pop2);
//			double [][] normalizedWeightsPolicyCase = this.sau.normalizeArray(weightsPolicyCase);
//			
//			double [][] userBenefitsPolicyCase = fillUserBenefits(personId2Utility2, pop2);
//			double [][] normalizedUserBenefitsPolicyCase = this.sau.normalizeArray(userBenefitsPolicyCase);
//			
//			double [][] averageUserBenefitsPolicyCase = calculateAverage(normalizedUserBenefitsPolicyCase, normalizedWeightsPolicyCase);
//			
//			// calculate differences base case <-> policy case
//			double [][] normalizedUserBenefitDifferences = calculateDifferences(normalizedUserBenefitsPolicyCase, normalizedUserBenefitsBaseCase);
//			double [][] normalizedAverageUserBenefitDifferences = calculateDifferences(averageUserBenefitsPolicyCase, averageUserBenefitsBaseCase);
//			// double [][] normalizedWeightsDifferences = calculateDifferences(normalizedWeightsPolicyCase, normalizedWeightsBaseCase);
//
//			String outputPath = outPathStub + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".Routput.";
//			String outNormalizedDifferences;
//			String outNormalizedAverageDifferences;
//			if(substractAverageValue){
//				outNormalizedDifferences = outputPath + "UserBenefitsDifferences.AverageRefund.txt";
//				outNormalizedAverageDifferences = outputPath + "UserBenefitsAverageDifferences.AverageRefund.txt";
//			}else{
//				outNormalizedDifferences = outputPath + "UserBenefitsDifferences.NoRefund.txt";
//				outNormalizedAverageDifferences = outputPath + "UserBenefitsAverageDifferences.NoRefund.txt";
//			}
//			
//			this.saWriter.writeRoutput(normalizedUserBenefitDifferences, outNormalizedDifferences);
//			this.saWriter.writeRoutput(normalizedAverageUserBenefitDifferences, outNormalizedAverageDifferences);
//			//	this.sau.writeRoutput(normalizedWeightsDifferences, outPathStub + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".Routput_" + "WeightsDifferences.txt"); // should be zero
//			
//			Double totalWelfare2 = 0.0;
//			for(Double wfa : ubc2.getPersonId2MonetizedUtility().values()){
//				totalWelfare2 += wfa;
//			}
//			Double averageWelfare2 = totalWelfare/ubc2.getPersonId2MonetizedUtility().size();
//			
//			logger.info("base case welfare total" +totalWelfare2 + " base case avg Welfare" + averageWelfare2);
//		}
//	}
//
//	private double[][] calculateDifferences(double[][] normalizedArrayPolicyCase, double[][] normalizedArrayBaseCase) {
//		double [][] diff = new double[noOfXbins][noOfYbins];
//		for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
//			for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
//				diff[xIndex][yIndex]= normalizedArrayPolicyCase[xIndex][yIndex] - normalizedArrayBaseCase[xIndex][yIndex];
//			}
//		}
//		return diff;
//	}
//
//	private double[][] calculateAverage(double[][] userBenefits, double[][] weights) {
//		double[][] average = new double [noOfXbins][noOfYbins];
//		for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
//			for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
//				if(weights[xIndex][yIndex] > 0){
//					average[xIndex][yIndex]= userBenefits[xIndex][yIndex] / weights[xIndex][yIndex];
//				} else {
//					throw new RuntimeException("Weights for " + xIndex + "," + yIndex + "is undefined. Aborting ...");
//				}
//			}
//		}
//		return average;
//	}
//
//	private double [][] calculateWeights(Map<Id, Double> personId2Utility, Population pop){
//		double[][] weights = new double[noOfXbins][noOfYbins];
//
//		for(Id personId : personId2Utility.keySet()){ 
//			Person person = pop.getPersons().get(personId);
//			Coord homeCoord = this.lf.getHomeActivityCoord(person);
//			if (this.sau.isInResearchArea(homeCoord)){
//				double personCount = 1.0;
//				// one person stands for this.scalingFactor persons
//				double scaledPersonCount = this.scalingFactor * personCount;
//				for(int xIndex = 0 ; xIndex < noOfXbins; xIndex++){
//					for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
//						Coord cellCentroid = this.sau.findCellCentroid(xIndex, yIndex);
//						double weightOfPersonForCell = this.sau.calculateWeightOfPointForCell(homeCoord.getX(), homeCoord.getY(), cellCentroid.getX(), cellCentroid.getY());
//						weights[xIndex][yIndex] += weightOfPersonForCell * scaledPersonCount;
//					}
//				}
//			}
//		}
//		return weights;
//	}
//	
//	private double [][] fillUserBenefits(Map<Id, Double> personId2Utility, Population pop){
//		double[][] scaledWeightedBenefits = new double [noOfXbins][noOfYbins];
//
//		for(Id personId : personId2Utility.keySet()){ 
//			Person person = pop.getPersons().get(personId);
//			Coord homeCoord = this.lf.getHomeActivityCoord(person);
//			if (this.sau.isInResearchArea(homeCoord)){
//				double benefitOfPerson = personId2Utility.get(personId);
//				// one person stands for this.scalingFactor persons; thus, that person earns the sum of these person's benefits (additivity required!)
//				double scaledBenefitOfPerson = this.scalingFactor * benefitOfPerson;
//
//				for(int xIndex = 0 ; xIndex < noOfXbins; xIndex++){
//					for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
//						Coord cellCentroid = this.sau.findCellCentroid(xIndex, yIndex);
//						double weightOfPersonForCell = this.sau.calculateWeightOfPointForCell(homeCoord.getX(), homeCoord.getY(), cellCentroid.getX(), cellCentroid.getY());
//						scaledWeightedBenefits[xIndex][yIndex] += weightOfPersonForCell * scaledBenefitOfPerson;
//					}
//				}
//			} else {
//				// do nothing...
//			}
//		}
//		return scaledWeightedBenefits;
//	}
//	
//	private Scenario loadScenario(String netFile) {
//		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile(netFile);
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		return scenario;
//	}
//
//	private static Integer getLastIteration(String configFile) {
//		Config config = ConfigUtils.createConfig();
//		MatsimConfigReader configReader = new MatsimConfigReader(config);
//		configReader.readFile(configFile);
//		Integer lastIteration = config.controler().getLastIteration();
//		return lastIteration;
//	}
//
//	public static void main(String[] args) throws IOException{
//		new SpatialAveragingWelfare().run();
//	}
//}