/* *********************************************************************** *
 * project: org.matsim.*
 * SpatialAveragingForLinkEmissions.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.julia.newSpatialAveraging;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.events.EmissionEventsReader;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.contrib.emissions.utils.EmissionUtils;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkColdEventHandler;
import playground.benjamin.scenarios.munich.analysis.nectar.EmissionsPerLinkWarmEventHandler;
import playground.julia.newSpatialAveraging.SpatialAveragingWriter;


/**
 * @author benjamin
 *
 */
public class SpatialAveragingDemandEmissions {
	private static final Logger logger = Logger.getLogger(SpatialAveragingDemandEmissions.class);

	final double scalingFactor = 100.; //100
	private final static String runNumber1 = "baseCase";
	private final static String runDirectory1 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_baseCase_ctd/";
	private final static String runNumber2 = "zone30";
	private final static String runDirectory2 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_policyCase_zone30/";
//	private final static String runNumber2 = "pricing";
//	private final static String runDirectory2 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_policyCase_pricing/";
//	private final static String runNumber2 = "exposurePricing";
//	private final static String runDirectory2 = "../../runs-svn/detEval/exposureInternalization/internalize1pct/output/output_policyCase_exposurePricing/";
	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

	private static String configFile1 = runDirectory1 + "output_config.xml.gz";
	private final static Integer lastIteration1 = getLastIteration(configFile1);
	private static String configFile2 = runDirectory1 + "output_config.xml.gz";
	private final static Integer lastIteration2 = getLastIteration(configFile2);
//	private final String emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
	private final String emissionFile1 = "./input/warmEmissionEvent.xml";
//	private final String emissionFile1 = "./input/1500.emission.events.xml";
	private final String emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration2 + "/" + lastIteration2 + ".emission.events.xml.gz";
	

//	final double scalingFactor = 100.;
//	private final static String runNumber1 = "baseCase";
//	private final static String runDirectory1 = "../../runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/";
////	private final static String runNumber2 = "zone30";
////	private final static String runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_zone30/";
//	private final static String runNumber2 = "pricing";
//	private final static String runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_pricing_newCode/";
//	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
//	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
//
//	private static String configFile1 = runDirectory1 + "output_config.xml.gz";
//	private final static Integer lastIteration1 = getLastIteration(configFile1);
//	private static String configFile2 = runDirectory1 + "output_config.xml.gz";
//	private final static Integer lastIteration2 = getLastIteration(configFile2);
//	private final String emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
//	private final String emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration2 + "/" + lastIteration2 + ".emission.events.xml.gz";
	
	
//	final double scalingFactor = 10.;
//	private final static String runNumber1 = "981";
//	private final static String runNumber2 = "983";
//	private final static String runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
//	private final static String runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
//	private final String netFile1 = runDirectory1 + runNumber1 + ".output_network.xml.gz";
//	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
//
//	private static String configFile1 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
//	private final static Integer lastIteration1 = getLastIteration(configFile1);
//	private static String configFile2 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
//	private final static Integer lastIteration2 = getLastIteration(configFile2);
//	private final String emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + runNumber1 + "." + lastIteration1 + ".emission.events.xml.gz";
//	private final String emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration2 + "/" + runNumber2 + "." + lastIteration2 + ".emission.events.xml.gz";

	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	final double xMin = 4452550.25;
	final double xMax = 4479483.33;
	final double yMin = 5324955.00;
	final double yMax = 5345696.81;
	final int noOfXbins = 16; //160
	final int noOfYbins = 12; //120
	
	final int noOfTimeBins = 1;
	
	final double smoothingRadius_m = 500.;
	
	final String pollutant2analyze = WarmPollutant.NO2.toString();
	final boolean compareToBaseCase = false;
	final boolean useLineMethod = true;
	private boolean writeRoutput = true;
	private boolean writeGisOutput = false;
	
	SpatialAveragingWriter saWriter;
	double simulationEndTime;
	Network network;
	
	EmissionUtils emissionUtils = new EmissionUtils();
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	String outPathStub;

	private EmissionsPerLinkAndTimeIntervalEventHandler emissionHandler;

	private LinkWeightUtil linkWeightUtil;

	private SpatialGrid[] timeInterval2GridBaseCase;
	
	/*
	 * process first emission file: calculate weighted emissions per cell
	 * write R or GIS output files (weighted emissions, weighted demand, specific emissions)
	 * 
	 * if comparision to base case is selected: 
	 * process second emission file: calculate weighted emissions per cell
	 * calculate differences to base case
	 * write R or GIS output files
	 * 
	 */
	
	private void run() throws IOException{
		
		if(useLineMethod){
			linkWeightUtil = new LinkLineWeightUtil(smoothingRadius_m);
		}else{
			linkWeightUtil = new SimpleLinkWeightUtil(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, munichShapeFile, targetCRS);
		}
		this.saWriter = new SpatialAveragingWriter(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, munichShapeFile, targetCRS);
		
		this.simulationEndTime = getEndTime(configFile1);
		this.network = loadScenario(netFile1).getNetwork();		
		
		runBaseCase();
		if(compareToBaseCase){
			//runCompareCase(emissionFile2);
		}
	}
	
	private void runBaseCase() throws IOException{
		
		/*
		 * 1. initialize scenario, spatial averaging utils
		 */
		

		outPathStub = runDirectory1 + "analysis/spatialAveraging/" + runNumber1 + "." + lastIteration1;
	
		/*
		 * 2. parse emission file and store overall emission amounts by time bins 
		 */
		
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		this.emissionHandler = new EmissionsPerLinkAndTimeIntervalEventHandler(this.simulationEndTime, noOfTimeBins, pollutant2analyze);
		eventsManager.addHandler(emissionHandler);
		emissionReader.parse(emissionFile1);
		
		Map <Integer, Map<Id, Double>>timeInterval2Link2Pollutant = emissionHandler.getTimeIntervals2EmissionsPerLink();
		timeInterval2GridBaseCase= new SpatialGrid[noOfTimeBins];
		
		for(int timeInterval:timeInterval2Link2Pollutant.keySet()){
			SpatialGrid sGrid = new SpatialGrid(noOfXbins, noOfYbins, this.xMin, this.xMax, this.yMin, this.yMax);
			
			for(Id linkId: timeInterval2Link2Pollutant.get(timeInterval).keySet()){
				sGrid.addLinkValue(network.getLinks().get(linkId), timeInterval2Link2Pollutant.get(timeInterval).get(linkId), linkWeightUtil);
			}
			sGrid.multiplyAllCells(linkWeightUtil.getNormalizationFactor());
			// store results for comparision
			timeInterval2GridBaseCase[timeInterval] = sGrid;
			Double endOfTimeInterval = timeInterval/noOfTimeBins*simulationEndTime;
			// print tables
			if(writeRoutput){
				logger.info("start writing r output to " + outPathStub + ".Routput...");
				
				this.saWriter.writeRoutput(sGrid.getAverageValuesOfGrid(), outPathStub + ".Routput." + pollutant2analyze.toString() + ".g." + endOfTimeInterval + ".txt");
				this.saWriter.writeRoutput(sGrid.getWeightsOfGrid(), outPathStub + ".Routput.Demand.vkm." + endOfTimeInterval + ".txt");
				this.saWriter.writeRoutput(sGrid.getAverageValuesOfGrid(), outPathStub+ ".Routput." + pollutant2analyze + ".gPerVkm." + endOfTimeInterval + ".txt");
				}
				if(writeGisOutput){
					this.saWriter.writeGISoutput(sGrid.getWeightedValuesOfGrid(), outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".g.movie.shp", endOfTimeInterval);
					this.saWriter.writeGISoutput(sGrid.getWeightsOfGrid(), outPathStub + ".GISoutput.Demand.vkm.movie.shp", endOfTimeInterval);
					this.saWriter.writeGISoutput(sGrid.getAverageValuesOfGrid(), outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".gPerVkm.movie.shp",endOfTimeInterval);
				}
			}
			logger.info("Done writing output.");
		}
			
	
	
//	private void runCompareCase(String emissionFile) throws IOException{
//	
//		/*
//		 * 1. parse emission file and store overall emission amounts by time bins 
//		 */
//		
//		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFilteredCompareCase = new HashMap<Double, Map<Id,Map<String,Double>>>(); 
//		Map<Double, Map<Id, Double>> time2CountsPerLinkCompareCase = new HashMap<Double, Map<Id,Double>>();
//		Map<Double, Map<Id, Double>> time2CountsPerLinkFilledAndFilteredCompareCase = new HashMap<Double, Map<Id,Double>>();
//		
//		//parseAndProcessEmissions(emissionFile, time2EmissionsTotalFilledAndFilteredCompareCase, time2CountsPerLinkCompareCase, time2CountsPerLinkFilledAndFilteredCompareCase);
//		parseAndProcessEmissions(emissionFile);
//		time2EmissionsTotalFilledAndFilteredCompareCase = this.time2EmissionsTotalFilledAndFiltered;
//		time2CountsPerLinkCompareCase = this.time2CountsPerLink;
//		time2CountsPerLinkFilledAndFilteredCompareCase = this.time2CountsPerLinkFilledAndFiltered;
//		
//		/*
//		 * 2. calculate weighted emissions per cell, weighted demand per cell
//		 */
//		Map<Double, double[][]> time2NormalizedWeightedEmissionsCompareCase = new HashMap<Double, double[][]>();
//		Map<Double, double[][]> time2NormalizedWeightedDemandCompareCase = new HashMap<Double, double[][]>();
//		Map<Double, double[][]> time2SpecificEmissionsCompareCase = new HashMap<Double, double[][]>();
//		
//		//calculateWeightedEmissionsAndDemands(time2NormalizedWeightedEmissionsCompareCase, time2NormalizedWeightedDemandCompareCase, time2SpecificEmissionsCompareCase, time2EmissionsTotalFilledAndFilteredCompareCase, time2CountsPerLinkFilledAndFilteredCompareCase);
//		calculateWeightedEmissionsAndDemands(time2EmissionsTotalFilledAndFilteredCompareCase, time2CountsPerLinkFilledAndFilteredCompareCase);
//		time2NormalizedWeightedEmissionsCompareCase = this.time2NormalizedWeightedEmissions;
//		time2NormalizedWeightedDemandCompareCase = this.time2NormalizedWeightedDemand;
//		time2SpecificEmissionsCompareCase = this.time2SpecificEmissions;
//		
//		/*
//		 * 3. calculate differences to base case
//		 */
//		Map<Double, double[][]> time2AbsoluteEmissionDifferences = calculateAbsoluteDifferencesPerBin(time2NormalizedWeightedEmissionsBaseCase, time2NormalizedWeightedEmissionsCompareCase);
//		Map<Double, double[][]> time2AbsoluteDemandDifferences = calculateAbsoluteDifferencesPerBin(time2NormalizedWeightedDemandBaseCase, time2NormalizedWeightedDemandCompareCase);
//		Map<Double, double[][]> time2SpecificEmissionDifferences = calculateAbsoluteDifferencesPerBin(time2SpecificEmissionsBaseCase, time2SpecificEmissionsCompareCase);
//		
//		/*
//		 * 4. write output
//		 */
//		outPathStub = runDirectory1 + "analysis/spatialAveraging/" + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
//		writeOutput(time2AbsoluteEmissionDifferences, time2AbsoluteDemandDifferences, time2SpecificEmissionDifferences);
//	}

	



	
	
	private Map<Double, double[][]> calculateSpecificEmissionsPerBin(
			Map<Double, double[][]> time2weightedEmissions,
			Map<Double, double[][]> time2weightedDemand) {
		
		Map<Double, double[][]> time2specificEmissions = new HashMap<Double, double[][]>();
		for( Double endOfTimeInterval : time2weightedEmissions.keySet()){
			double [][] specificEmissions = new double[noOfXbins][noOfYbins];
			for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
					specificEmissions[xIndex][yIndex] = time2weightedEmissions.get(endOfTimeInterval)[xIndex][yIndex] / time2weightedDemand.get(endOfTimeInterval)[xIndex][yIndex];
				}
			}
			time2specificEmissions.put(endOfTimeInterval, specificEmissions);
		}
		return time2specificEmissions;
	}
	
	private Map<Double, double[][]> calculateAbsoluteDifferencesPerBin(
			Map<Double, double[][]> time2weightedValues1,
			Map<Double, double[][]> time2weightedValues2){
		
		Map<Double, double[][]> time2absoluteDifferences = new HashMap<Double, double[][]>();
		for(Double endOfTimeInterval : time2weightedValues1.keySet()){
			double [][] absoluteDifferences = new double[noOfXbins][noOfYbins];
			for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
					absoluteDifferences[xIndex][yIndex] = time2weightedValues2.get(endOfTimeInterval)[xIndex][yIndex] - time2weightedValues1.get(endOfTimeInterval)[xIndex][yIndex];
				}
			}
			time2absoluteDifferences.put(endOfTimeInterval, absoluteDifferences);
		}		
		return time2absoluteDifferences;
	}

	private Scenario loadScenario(String netFile) {
		Config config = ConfigUtils.createConfig();
		config.network().setInputFile(netFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		return scenario;
	}

	private Double getEndTime(String configfile) {
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configfile);
		Double endTime = config.qsim().getEndTime();
		logger.info("Simulation end time is: " + endTime / 3600 + " hours.");
		logger.info("Aggregating emissions for " + (int) (endTime / 3600 / noOfTimeBins) + " hour time bins.");
		return endTime;
	}

	private static Integer getLastIteration(String configFile) {
		Config config = ConfigUtils.createConfig();
		MatsimConfigReader configReader = new MatsimConfigReader(config);
		configReader.readFile(configFile);
		Integer lastIteration = config.controler().getLastIteration();
		return lastIteration;
	}

	public static void main(String[] args) throws IOException{
		new SpatialAveragingDemandEmissions().run();
	}
}