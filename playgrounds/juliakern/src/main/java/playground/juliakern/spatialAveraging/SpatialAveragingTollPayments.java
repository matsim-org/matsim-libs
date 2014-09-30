package playground.juliakern.spatialAveraging;
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
//package playground.julia.spatialAveraging;
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
//import org.matsim.api.core.v01.population.Population;
//import org.matsim.contrib.emissions.events.EmissionEventsReader;
//import org.matsim.contrib.emissions.types.ColdPollutant;
//import org.matsim.contrib.emissions.types.WarmPollutant;
//import org.matsim.core.api.experimental.events.EventsManager;
//import org.matsim.core.config.Config;
//import org.matsim.core.config.ConfigUtils;
//import org.matsim.core.config.MatsimConfigReader;
//import org.matsim.core.events.EventsUtils;
//import org.matsim.core.events.MatsimEventsReader;
//import org.matsim.core.events.handler.EventHandler;
//import org.matsim.core.population.MatsimPopulationReader;
//import org.matsim.core.scenario.ScenarioUtils;
//
//import playground.benjamin.internalization.EmissionCostModule;
//import playground.benjamin.scenarios.munich.analysis.filter.LocationFilter;
//import playground.benjamin.scenarios.zurich.analysis.MoneyEventHandler;
//import playground.julia.newSpatialAveraging.SpatialAveragingWriter;
//import playground.vsp.analysis.modules.userBenefits.UserBenefitsCalculator;
//import playground.vsp.analysis.modules.userBenefits.WelfareMeasure;
//
///**
// * @author julia, benjamin
// *
// */
//public class SpatialAveragingTollPayments {
//	private static final Logger logger = Logger.getLogger(SpatialAveragingTollPayments.class);
//
//	final double scalingFactor = 100.;
//	private final static String runNumber1 = "exposurePricing";
//	private final static String runDirectory1 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_policyCase_exposurePricing/";
//
////	private final static String runNumber1 = "pricing";
////	private final static String runDirectory1 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_policyCase_pricing/";
////	private final static String runDirectory1 = "../../runs-svn/detEval/emissionInternalization/output/output_baseCase_ctd_newCode/";
////	private final static String runNumber2 = "zone30";
////	private final static String runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_zone30/";
////	private final static String runNumber2 = "pricing";
////	private final static String runDirectory2 = "../../runs-svn/detEval/emissionInternalization/output/output_policyCase_pricing_newCode/";
////	private final String netFile1 = runDirectory2 + "output_network.xml.gz";
//	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
//	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
////
////	private static String configFile1 = runDirectory1 + "output_config.xml.gz";
//	//private final static Integer lastIteration1 = getLastIteration(configFile1);
//	private final static Integer lastIteration1 = 1500;
////	private static String configFile2 = runDirectory1 + "output_config.xml.gz";
//	private final static Integer lastIteration2 = 1500;
////	private final static Integer lastIteration2 = getLastIteration(configFile2);
//	private final String plansFile1 = runDirectory1 + "ITERS/it.1500/1500.plans.xml";
////	private final String plansFile2 = runDirectory2 + "ITERS/it.1500/1500.plans.xml";
//	private final String emissionFile = runDirectory1 + "ITERS/it.1500/1500.emission.events.xml.gz";
//	private final String eventsfile = runDirectory1 +  "ITERS/it.1500/1500.events.xml.gz";
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
//	final int noOfXbins = 160;
//	final int noOfYbins = 120;
//
//	final double smoothingRadius_m = 1000.;
//	final double area_in_smoothing_circle_sqkm = (Math.PI * this.smoothingRadius_m * this.smoothingRadius_m) / (1000. * 1000.);
//	
//	final boolean compareToBaseCase = true;
//	
//	SpatialAveragingUtils sau;
//	LocationFilter lf;
//	Network network;
//
//	String outPathStub = runDirectory1 + "analysis/spatialAveraging/";
//
//	private SpatialAveragingWriter saWriter;
//
//	private void run() throws IOException{
//		this.sau = new SpatialAveragingUtils(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, munichShapeFile, null);
//		this.lf = new LocationFilter();
//		
//		Scenario scenario = loadScenario(netFile1);
//		this.network = scenario.getNetwork();		
//		MatsimPopulationReader mpr = new MatsimPopulationReader(scenario);
//		mpr.readFile(plansFile1);
//		
//		Config config = scenario.getConfig();
//		Population pop = scenario.getPopulation();
//		
//		
//		// get paid toll
//		EventsManager eventsManager = EventsUtils.createEventsManager();
//		MoneyEventHandler moneyEventHandler = new MoneyEventHandler();
//		eventsManager.addHandler(moneyEventHandler);
//		MatsimEventsReader mer = new MatsimEventsReader(eventsManager);
//		mer.readFile(eventsfile);
//		
//		Map<Id, Double> personId2paidToll = moneyEventHandler.getPersonId2TollMap();
//		
//		
////		// calculate paid toll per person -- for internalization
////		Map<Id, Double> personId2paidToll = new HashMap<Id, Double>();
////		EventsManager eventsManager = EventsUtils.createEventsManager();
////		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
////		
////		SimpleWarmEmissionEventHandler weeh = new SimpleWarmEmissionEventHandler();
////		eventsManager.addHandler(weeh);
////		SimpleColdEmissionEventHandler ceeh = new SimpleColdEmissionEventHandler();
////		eventsManager.addHandler(ceeh);
////		emissionReader.parse(emissionFile);
////		
////		Map<Id, Map<WarmPollutant, Double>> personId2warmEmissions = weeh.getPersonId2warmEmissions();
////		Map<Id, Map<ColdPollutant, Double>> personId2coldEmissions = ceeh.getPersonId2coldEmissions();
////		EmissionCostModule emissionCostModule = new EmissionCostModule(emissionCostFactor );
////		
////		for(Id personId: pop.getPersons().keySet()){
////			
////			Map<WarmPollutant, Double> warmEmissions = personId2warmEmissions.get(personId);
////			Map<ColdPollutant, Double> coldEmissions = personId2coldEmissions.get(personId);
////		
////			Double warmEmissionCosts =0.0;
////			Double coldEmissionCosts =0.0;
////			
////			if(warmEmissions!=null){
////				warmEmissionCosts = emissionCostModule.calculateWarmEmissionCosts(warmEmissions);
////			}
////			if(coldEmissions!=null){		
////				coldEmissionCosts = emissionCostModule.calculateColdEmissionCosts(coldEmissions);
////			}
////			Double personalEmissionCost = warmEmissionCosts+coldEmissionCosts;
////			personId2paidToll.put(personId, personalEmissionCost);		
////		}
//			
//		
//		double [][] weightsBaseCase = calculateWeights(pop);
//		double [][] normalizedWeightsBaseCase = this.sau.normalizeArray(weightsBaseCase);
//		
//		double [][] userTollPayments = fillUserBenefits(personId2paidToll, pop);
//	//	double [][] normalizedUserTollPayments = this.sau.normalizeArray(userTollPayments);
//		
//		double [][] averageUserTollPaymentsBaseCase = calculateAverage(normalizedUserTollPayments, normalizedWeightsBaseCase);
//		
//		this.saWriter = new SpatialAveragingWriter(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, munichShapeFile, null);
//		this.saWriter.writeRoutput(normalizedUserTollPayments, outPathStub + runNumber1 + "." + lastIteration1 + ".Routput." + "PaidTollsByHomeLocation.txt");
//		this.saWriter.writeRoutput(averageUserTollPaymentsBaseCase, outPathStub + runNumber1 + "." + lastIteration1 + ".Routput." + "PaidTollsPerPersonByHomeLocationAverage.txt");
//		//this.sau.writeRoutput(normalizedWeightsBaseCase, outPathStub + runNumber1 + "." + lastIteration1 + ".Routput." + "NormalizedWeightsBaseCase.txt");
//		
//		
//	}
//
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
//	private double [][] calculateWeights(Population pop){
//		double[][] weights = new double[noOfXbins][noOfYbins];
//
//		for(Id personId : pop.getPersons().keySet()){ 
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
////	private double [][] calculateWeights(Population pop){
////		double[][] weights = new double[noOfXbins][noOfYbins];
////
////		for(Id personId : pop.getPersons().keySet()){ 
////			Person person = pop.getPersons().get(personId);
////			Coord homeCoord = this.lf.getHomeActivityCoord(person);
////			
////				double personCount = 1.0;
////				// one person stands for this.scalingFactor persons
////				double scaledPersonCount = this.scalingFactor * personCount;
////				for(int xIndex = 0 ; xIndex < noOfXbins; xIndex++){
////					for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
////						Coord cellCentroid = this.sau.findCellCentroid(xIndex, yIndex);
////						double weightOfPersonForCell = this.sau.calculateWeightOfPointForCell(homeCoord.getX(), homeCoord.getY(), cellCentroid.getX(), cellCentroid.getY());
////						weights[xIndex][yIndex] += weightOfPersonForCell * scaledPersonCount;
////					}
////				}
////			
////		}
////		return weights;
////	}
//	
//	private double [][] fillUserBenefits(Map<Id, Double> personId2Utility, Population pop){
//		double[][] scaledWeightedBenefits = new double [noOfXbins][noOfYbins];
//
//		for(Id personId : personId2Utility.keySet()){ 
//			Person person = pop.getPersons().get(personId);
//			Coord homeCoord = this.lf.getHomeActivityCoord(person);
//			
//				double benefitOfPerson = personId2Utility.get(personId);
//				// one person stands for this.scalingFactor persons; thus, that person earns the sum of these person's benefits (additivity required!)
//				double scaledBenefitOfPerson = this.scalingFactor * benefitOfPerson;
//
//				for(int xIndex = 0 ; xIndex < noOfXbins; xIndex++){
//					for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
//						Coord cellCentroid = this.sau.findCellCentroid(xIndex, yIndex);
//						double weightOfPersonForCell = this.sau.calculateWeightOfPointForCell(homeCoord.getX(), homeCoord.getY(), cellCentroid.getX(), cellCentroid.getY());
//						// negative since payments = - moneyevent
//						scaledWeightedBenefits[xIndex][yIndex] += -weightOfPersonForCell * scaledBenefitOfPerson;
//					}
//				}
//			
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
//		new SpatialAveragingTollPayments().run();
//	}
//}