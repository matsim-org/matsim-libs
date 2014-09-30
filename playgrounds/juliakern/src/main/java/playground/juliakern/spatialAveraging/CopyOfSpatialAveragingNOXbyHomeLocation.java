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
//import org.matsim.core.population.MatsimPopulationReader;
//import org.matsim.core.scenario.ScenarioUtils;
//
//import playground.benjamin.internalization.EmissionCostModule;
//import playground.benjamin.scenarios.munich.analysis.filter.LocationFilter;
//import playground.julia.newSpatialAveraging.SpatialAveragingWriter;
//
///**
// * @author julia, benjamin
// *
// */
//public class CopyOfSpatialAveragingNOXbyHomeLocation {
//	private static final Logger logger = Logger.getLogger(CopyOfSpatialAveragingNOXbyHomeLocation.class);
//
//	final double scalingFactor = 100.;
//	private final static String runNumber1 = "baseCase";
//	private final static String runDirectory1 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_baseCase_ctd/";
////	private final static String runNumber2 = "pricing";
////	private final static String runDirectory2 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_pricing/";
//
//	private final static String runNumber2 = "exposurePricing";
//	private final static String runDirectory2 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_exposurePricing/";
////	private final static String runNumber2 = "pricing";
////	private final static String runDirectory2 = "../../runs-svn/detEval/emissionInternalization/output/output_policyCase_pricing_newCode/";
////	private final String netFile1 = runDirectory2 + "output_network.xml.gz";
//	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
//	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
////
////	private static String configFile1 = runDirectory1 + "output_config.xml.gz";
//	//private final static Integer lastIteration1 = getLastIteration(configFile1);
////	private final static Integer lastIteration1 = 1500;
////	private static String configFile2 = runDirectory1 + "output_config.xml.gz";
////	private final static Integer lastIteration2 = 1500;
////	private final static Integer lastIteration2 = getLastIteration(configFile2);
//	private final String plansFile1 = runDirectory1 + "ITERS/it.1500/1500.plans.xml";
//	private final String plansFile2 = runDirectory2 + "ITERS/it.1500/1500.plans.xml";
//	private final String emissionFile = runDirectory1 + "ITERS/it.1500/1500.emission.events.xml.gz";
//	private final String emissionFile2 = runDirectory2 + "ITERS/it.1500/1500.emission.events.xml.gz";
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
//	final boolean compareToBaseCase = false;
//	
//	SpatialAveragingUtils sau;
//	LocationFilter lf;
//	Network network;
//
//	String outPathStub = runDirectory1 + "analysis/spatialAveraging/welfare/";
//
//	private double emissionCostFactor = 1.0;
//
//	private boolean substractAverageValue = false; // personal value
//
//	private String outputPathForR = "../../runs-svn/detEval/emissionInternalization/output/output_baseCase_ctd_newCode/analysis/";
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
//		
//		
//		// calculate nox emissions per person
//		Map<Id, Double> personId2NoxAmount = new HashMap<Id, Double>();
//		EventsManager eventsManager = EventsUtils.createEventsManager();
//		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
//		
//		SimpleWarmEmissionEventHandler weeh = new SimpleWarmEmissionEventHandler();
//		eventsManager.addHandler(weeh);
//		SimpleColdEmissionEventHandler ceeh = new SimpleColdEmissionEventHandler();
//		eventsManager.addHandler(ceeh);
//		emissionReader.parse(emissionFile);
//		
//		Map<Id, Double> personId2noxWarm = weeh.getPersonId2pollutant(WarmPollutant.NOX);
//		Map<Id, Double> personId2noxCold = ceeh.getPersonId2pollutant(ColdPollutant.NOX);
//		
//		for(Id personId: pop.getPersons().keySet()){
//			if(personId2noxWarm.get(personId)!=null){
//				Double value = personId2noxWarm.get(personId);
//				if(personId2noxCold.get(personId)!=null){
//					value += personId2noxCold.get(personId);
//				}
//				personId2NoxAmount.put(personId, value);
//			}
//			
//			
//		}
//		
//		double [][] populationDensity = calculateWeights(pop);
//		double [][] normalizedDensity = this.sau.normalizeArray(populationDensity);
//		double [][] noxAmountBaseCase = fillAmount(personId2NoxAmount, pop);
//		double [][] normalizedNoxAmountBaseCase = this.sau.normalizeArray(noxAmountBaseCase);
//		double [][] averageNOXAmountPerPerson = calculateAverage(noxAmountBaseCase, populationDensity);
//		
//		this.saWriter.writeRoutput(normalizedDensity, outputPathForR +"populationDensity.txt");
//		this.saWriter.writeRoutput(normalizedNoxAmountBaseCase, outputPathForR+"totalNOXAmountByHomeLocation.txt");
//		this.saWriter.writeRoutput(averageNOXAmountPerPerson, outputPathForR+"averageNOXamountPerPersonByHomeLocation.txt");
//		
//		if(compareToBaseCase){
//			Scenario scenario2 = loadScenario(netFile1);
//			MatsimPopulationReader mpr2 = new MatsimPopulationReader(scenario2);
//			mpr2.readFile(plansFile2);
//			
//			Config config2 = scenario2.getConfig();
//			Population pop2 = scenario2.getPopulation();
//			
//			// calculate nox emissions per person
//			Map<Id, Double> personId2NoxAmountPolicy = new HashMap<Id, Double>();
//			EventsManager eventsManagerPolicy = EventsUtils.createEventsManager();
//			EmissionEventsReader emissionReaderPolicy = new EmissionEventsReader(eventsManager);
//			
//			SimpleWarmEmissionEventHandler weehPolicy = new SimpleWarmEmissionEventHandler();
//			eventsManager.addHandler(weehPolicy);
//			SimpleColdEmissionEventHandler ceehPolicy = new SimpleColdEmissionEventHandler();
//			eventsManager.addHandler(ceehPolicy);
//			emissionReader.parse(emissionFile2);
//			
//			Map<Id, Double> personId2noxWarmPolicy = weehPolicy.getPersonId2pollutant(WarmPollutant.NOX);
//			Map<Id, Double> personId2noxColdPolicy = ceehPolicy.getPersonId2pollutant(ColdPollutant.NOX);
//			
//			
//			for(Id personId: pop.getPersons().keySet()){
//				if(personId2noxWarm.get(personId)!=null){
//					Double value = personId2noxWarm.get(personId);
//					if(personId2noxCold.get(personId)!=null){
//						value += personId2noxCold.get(personId);
//					}
//					personId2NoxAmount.put(personId, value);
//				}
//				
//				
//			}
//			
//			double [][] populationDensityPolicy = calculateWeights(pop2);
//			double [][] normalizedDensityPolicy = this.sau.normalizeArray(populationDensityPolicy);
//			double [][] noxAmountPolicy = fillAmount(personId2NoxAmountPolicy, pop2);
//			double [][] normalizedNoxAmountPolicy = this.sau.normalizeArray(noxAmountPolicy);
//			double [][] averageNOXAmountPerPersonPolicy = calculateAverage(noxAmountPolicy, populationDensityPolicy);
//			
//			//calculate differences
//			double [][] normalizedNoxDifferences = calculateDifferences(normalizedNoxAmountPolicy, normalizedNoxAmountBaseCase);
//			double [][] normalizedAvgDifferences = calculateDifferences(averageNOXAmountPerPersonPolicy, averageNOXAmountPerPerson);
//			
//			this.saWriter.writeRoutput(normalizedNoxDifferences, outputPathForR + runNumber2 + "_-_" + runNumber1 + "differencesNOXAmountByHomeLocation.txt");
//			this.saWriter.writeRoutput(normalizedAvgDifferences, outputPathForR + runNumber2 + "_-_" + runNumber1 + "differencesAverageNOXAmountByHomeLocation.txt");
////			double [][] weightsPolicyCase = calculateWeights(personId2Utility2, pop2);
////			double [][] normalizedWeightsPolicyCase = this.sau.normalizeArray(weightsPolicyCase);
////			
////			double [][] userBenefitsPolicyCase = fillUserBenefits(personId2Utility2, pop2);
////			double [][] normalizedUserBenefitsPolicyCase = this.sau.normalizeArray(userBenefitsPolicyCase);
////			"../../runs-svn/detEval/emissionInternalization/output/output_policyCase_pricing_newCode/";
////			double [][] averageUserBenefitsPolicyCase = calculateAverage(normalizedUserBenefitsPolicyCase, normalizedWeightsPolicyCase);
////			
////			// calculate differences base case <-> policy case
////			double [][] normalizedUserBenefitDifferences = calculateDifferences(normalizedUserBenefitsPolicyCase, normalizedUserBenefitsBaseCase);
////			double [][] normalizedAverageUserBenefitDifferences = calculateDifferences(averageUserBenefitsPolicyCase, averageUserBenefitsBaseCase);
////			// double [][] normalizedWeightsDifferences = calculateDifferences(normalizedWeightsPolicyCase, normalizedWeightsBaseCase);
////
////			String outputPath = outPathStub + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".Routput.";
////			String outNormalizedDifferences;
////			String outNormalizedAverageDifferences;
////			if(substractAverageValue){
////				outNormalizedDifferences = outputPath + "UserBenefitsDifferences.AverageRefund.txt";
////				outNormalizedAverageDifferences = outputPath + "UserBenefitsAverageDifferences.AverageRefund.txt";
////			}else{
////				outNormalizedDifferences = outputPath + "UserBenefitsDifferences.PersonalRefund.txt";
////				outNormalizedAverageDifferences = outputPath + "UserBenefitsAverageDifferences.PersonalRefund.txt";		
////			}
////			
////			this.sau.writeRoutput(normalizedUserBenefitDifferences, outNormalizedDifferences);
////			this.sau.writeRoutput(normalizedAverageUserBenefitDifferences, outNormalizedAverageDifferences);
////			//	this.sau.writeRoutput(normalizedWeightsDifferences, outPathStub + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".Routput_" + "WeightsDifferences.txt"); // should be zero
//		}
//	}
//
//private double[][] fillAmount(Map<Id, Double> personId2NoxAmount,
//			Population pop) {
//	double[][] noxAmount = new double[noOfXbins][noOfYbins];
//	for(Id personId: personId2NoxAmount.keySet()){
//		Person person = pop.getPersons().get(personId);
//		Coord homeCoord = this.lf.getHomeActivityCoord(person);
//		if (this.sau.isInResearchArea(homeCoord)){
//			int xHome = sau.getXbin(homeCoord.getX());
//			int yHome = sau.getYbin(homeCoord.getY());
//			//TODO check this
//			if(noxAmount[xHome][yHome]>0.){
//				noxAmount[xHome][yHome]+=personId2NoxAmount.get(personId);
//			}else{
//				noxAmount[xHome][yHome]=personId2NoxAmount.get(personId);
//			}
//		}
//		
//	}
//return noxAmount;
//	}
//
////private double[][] calculatePopulationDensity(Population pop) {
////			double[][] populationDensity = new double[noOfXbins][noOfYbins];
////			for(Id personId: pop.getPersons().keySet()){
////				Person person = pop.getPersons().get(personId);
////				Coord homeCoord = this.lf.getHomeActivityCoord(person);
////				if (this.sau.isInResearchArea(homeCoord)){
////					int xHome = sau.getXbin(homeCoord.getX());
////					int yHome = sau.getYbin(homeCoord.getY());
////					//TODO check this
////					if(populationDensity[xHome][yHome]>0.){
////						populationDensity[xHome][yHome]+=1.;
////					}else{
////						populationDensity[xHome][yHome]=1.;
////					}
////				}
////				
////			}
////		return populationDensity;
////	}
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
//					//throw new RuntimeException("Weights for " + xIndex + "," + yIndex + "is undefined. Aborting ...");
//					average[xIndex][yIndex]=0.;
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
//		new CopyOfSpatialAveragingNOXbyHomeLocation().run();
//	}
//}