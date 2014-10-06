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
package playground.juliakern.spatialAveraging;

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
import playground.benjamin.scenarios.munich.analysis.spatialAvg.SpatialAveragingWriter;


/**
 * @author benjamin
 *
 */
public class SpatialAveragingDemandEmissions {
	private static final Logger logger = Logger.getLogger(SpatialAveragingDemandEmissions.class);

	final Double scalingFactor = 100.; //100
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

	private static String configFile1 = runDirectory1 + "output_config.xml";
	private final static Integer lastIteration1 = getLastIteration(configFile1);
	private static String configFile2 = runDirectory1 + "output_config.xml";
	private final static Integer lastIteration2 = getLastIteration(configFile2);
	private final String emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
//	private final String emissionFile1 = "./input/warmEmissionEvent.xml";
//	private final String emissionFile1 = "./input/1500.emission.events.xml";
	private final String emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration2 + "/" + lastIteration2 + ".emission.events.xml.gz";
	

//	final Double scalingFactor = 100.;
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
	
	
//	final Double scalingFactor = 10.;
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
	final Double xMin = 4452550.25;
	final Double xMax = 4479483.33;
	final Double yMin = 5324955.00;
	final Double yMax = 5345696.81;
	final int noOfXbins = 16; //160
	final int noOfYbins = 12; //120
	
	final int noOfTimeBins = 1;
	
	final Double smoothingRadius_m = 500.;
	
	final String pollutant2analyze = WarmPollutant.NO2.toString();
	final boolean compareToBaseCase = false;
	final boolean useLineMethod = false;
	private boolean writeRoutput = true;
	private boolean writeGisOutput = false;
	
	SpatialAveragingUtils sau;
	SpatialAveragingUtilsExtended saue;
	SpatialAveragingWriter saWriter;
	Double simulationEndTime;
	Network network;
	
	EmissionUtils emissionUtils = new EmissionUtils();
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	String outPathStub;

	Map<Double, Double[][]> time2NormalizedWeightedEmissionsBaseCase = new HashMap<Double, Double[][]>();
	Map<Double, Double[][]> time2NormalizedWeightedDemandBaseCase = new HashMap<Double, Double[][]>();
	Map<Double, Double[][]> time2SpecificEmissionsBaseCase = new HashMap<Double, Double[][]>();

	Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered;
	Map<Double, Map<Id<Link>, Double>> time2CountsPerLink;
	Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkFilledAndFiltered;

	private Map<Double, Double[][]> time2NormalizedWeightedDemand;
	private Map<Double, Double[][]> time2NormalizedWeightedEmissions;
	private Map<Double, Double[][]> time2SpecificEmissions;

	private boolean useVisBoundary = false;
	
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
		runBaseCase();
		if(compareToBaseCase){
			runCompareCase(emissionFile2);
		}
	}
	
	private void runBaseCase() throws IOException{
		
		/*
		 * 1. initialize scenario, spatial averaging utils
		 */
		initialize();
		
		// delete after testing ...
		//compareLinkLength(); 
	
		/*
		 * 2. parse emission file and store overall emission amounts by time bins 
		 */
		
		Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFilledAndFilteredBaseCase = new HashMap<>(); 
		Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkBaseCase = new HashMap<>();
		Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkFilledAndFilteredBaseCase = new HashMap<>();
		
		parseAndProcessEmissions(emissionFile1);
		time2EmissionsTotalFilledAndFilteredBaseCase = this.time2EmissionsTotalFilledAndFiltered;
		time2CountsPerLinkBaseCase = this.time2CountsPerLink;
		time2CountsPerLinkFilledAndFilteredBaseCase = this.time2CountsPerLinkFilledAndFiltered;
		
		/*
		 * 3. calculate weighted emissions per cell, weighted demand per cell
		 */
		
		calculateWeightedEmissionsAndDemands( time2EmissionsTotalFilledAndFilteredBaseCase, time2CountsPerLinkFilledAndFilteredBaseCase);
		time2NormalizedWeightedDemandBaseCase = this.time2NormalizedWeightedDemand;
		time2NormalizedWeightedEmissionsBaseCase = this.time2NormalizedWeightedEmissions;
		time2SpecificEmissionsBaseCase = this.time2SpecificEmissions;
		
		/*
		 * 4. write output
		 */
		writeOutput(time2NormalizedWeightedEmissionsBaseCase, time2NormalizedWeightedDemandBaseCase, time2SpecificEmissionsBaseCase);
		
	}
	
	private void runCompareCase(String emissionFile) throws IOException{
	
		/*
		 * 1. parse emission file and store overall emission amounts by time bins 
		 */
		
		Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFilledAndFilteredCompareCase = new HashMap<>(); 
		Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkCompareCase = new HashMap<>();
		Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkFilledAndFilteredCompareCase = new HashMap<>();
		
		//parseAndProcessEmissions(emissionFile, time2EmissionsTotalFilledAndFilteredCompareCase, time2CountsPerLinkCompareCase, time2CountsPerLinkFilledAndFilteredCompareCase);
		parseAndProcessEmissions(emissionFile);
		time2EmissionsTotalFilledAndFilteredCompareCase = this.time2EmissionsTotalFilledAndFiltered;
		time2CountsPerLinkCompareCase = this.time2CountsPerLink;
		time2CountsPerLinkFilledAndFilteredCompareCase = this.time2CountsPerLinkFilledAndFiltered;
		
		/*
		 * 2. calculate weighted emissions per cell, weighted demand per cell
		 */
		Map<Double, Double[][]> time2NormalizedWeightedEmissionsCompareCase = new HashMap<Double, Double[][]>();
		Map<Double, Double[][]> time2NormalizedWeightedDemandCompareCase = new HashMap<Double, Double[][]>();
		Map<Double, Double[][]> time2SpecificEmissionsCompareCase = new HashMap<Double, Double[][]>();
		
		//calculateWeightedEmissionsAndDemands(time2NormalizedWeightedEmissionsCompareCase, time2NormalizedWeightedDemandCompareCase, time2SpecificEmissionsCompareCase, time2EmissionsTotalFilledAndFilteredCompareCase, time2CountsPerLinkFilledAndFilteredCompareCase);
		calculateWeightedEmissionsAndDemands(time2EmissionsTotalFilledAndFilteredCompareCase, time2CountsPerLinkFilledAndFilteredCompareCase);
		time2NormalizedWeightedEmissionsCompareCase = this.time2NormalizedWeightedEmissions;
		time2NormalizedWeightedDemandCompareCase = this.time2NormalizedWeightedDemand;
		time2SpecificEmissionsCompareCase = this.time2SpecificEmissions;
		
		/*
		 * 3. calculate differences to base case
		 */
		Map<Double, Double[][]> time2AbsoluteEmissionDifferences = calculateAbsoluteDifferencesPerBin(time2NormalizedWeightedEmissionsBaseCase, time2NormalizedWeightedEmissionsCompareCase);
		Map<Double, Double[][]> time2AbsoluteDemandDifferences = calculateAbsoluteDifferencesPerBin(time2NormalizedWeightedDemandBaseCase, time2NormalizedWeightedDemandCompareCase);
		Map<Double, Double[][]> time2SpecificEmissionDifferences = calculateAbsoluteDifferencesPerBin(time2SpecificEmissionsBaseCase, time2SpecificEmissionsCompareCase);
		
		/*
		 * 4. write output
		 */
		outPathStub = runDirectory1 + "analysis/spatialAveraging/" + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
		writeOutput(time2AbsoluteEmissionDifferences, time2AbsoluteDemandDifferences, time2SpecificEmissionDifferences);
	}

	private void initialize() {
		this.sau = new SpatialAveragingUtils(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, munichShapeFile, targetCRS);
		this.saue = new SpatialAveragingUtilsExtended(smoothingRadius_m);
		this.saWriter = new SpatialAveragingWriter(xMin, xMax, yMin, yMax, noOfXbins, noOfYbins, smoothingRadius_m, munichShapeFile, targetCRS, useVisBoundary );
		
		this.simulationEndTime = getEndTime(configFile1);
		this.network = loadScenario(netFile1).getNetwork();		
		outPathStub = runDirectory1 + "analysis/spatialAveraging/" + runNumber1 + "." + lastIteration1;
	}

	private void parseAndProcessEmissions(String emissionFile){
		this.time2EmissionsTotalFilledAndFiltered = processEmissions(emissionFile);
		this.time2CountsPerLink = warmHandler.getTime2linkIdLeaveCount();
		this.time2CountsPerLinkFilledAndFiltered = setNonCalculatedCountsAndFilter(this.time2CountsPerLink);
		//TODO 
//		this.warmHandler.reset(0);
//		this.coldHandler.reset(0);
	}
	
	private void calculateWeightedEmissionsAndDemands(Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered1, 
			Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkFilledAndFiltered1) {
		
		this.time2NormalizedWeightedEmissions = fillAndNormalizeWeightedEmissionValues(time2EmissionsTotalFilledAndFiltered1);
		this.time2NormalizedWeightedDemand = fillAndNormalizeWeightedDemandValues(time2CountsPerLinkFilledAndFiltered1);
		this.time2SpecificEmissions = calculateSpecificEmissionsPerBin(time2NormalizedWeightedEmissions, time2NormalizedWeightedDemand);		
	}
	
	private void writeOutput(Map<Double, Double[][]> time2NormalizedWeightedEmissions, Map<Double, Double[][]> time2NormalizedWeightedDemand, Map<Double, Double[][]> time2SpecificEmissions) throws IOException{
		logger.info("start writing r output to " + outPathStub + ".Routput...");
		for(Double endOfTimeInterval: time2NormalizedWeightedEmissions.keySet()){
			if(writeRoutput){
				logger.info("start writing r output to " + outPathStub + ".Routput...");
				this.saWriter.writeRoutput(time2NormalizedWeightedEmissions.get(endOfTimeInterval), outPathStub + ".Routput." + pollutant2analyze.toString() + ".g." + endOfTimeInterval + ".txt");
				this.saWriter.writeRoutput(time2NormalizedWeightedDemand.get(endOfTimeInterval), outPathStub + ".Routput.Demand.vkm." + endOfTimeInterval + ".txt");
				this.saWriter.writeRoutput(time2SpecificEmissions.get(endOfTimeInterval), outPathStub+ ".Routput." + pollutant2analyze + ".gPerVkm." + endOfTimeInterval + ".txt");
			}
			if(writeGisOutput){
//				this.saWriter.writeGISoutput(time2NormalizedWeightedEmissions, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".g.movie.shp");
//				this.saWriter.writeGISoutput(time2NormalizedWeightedDemand, outPathStub + ".GISoutput.Demand.vkm.movie.shp");
//				this.saWriter.writeGISoutput(time2SpecificEmissions, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".gPerVkm.movie.shp");
			}
		}
		logger.info("Done writing output.");
	}
	
	private Map<Double, Double[][]> fillAndNormalizeWeightedDemandValues(
			Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkFilledAndFiltered1) {
		Map<Double, Double[][]> time2WeightedDemand1 = fillWeightedDemandValues(time2CountsPerLinkFilledAndFiltered1);
		Map<Double, Double[][]> time2NormalizedWeightedDemand1 = normalizeAllArrays(time2WeightedDemand1);
		return time2NormalizedWeightedDemand1;
	}

	private Map<Double, Double[][]> fillAndNormalizeWeightedEmissionValues(
			Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered1) {
		
		Map<Double, Double[][]> time2WeightedEmissions1 = fillWeightedEmissionValues(time2EmissionsTotalFilledAndFiltered1);
		Map<Double, Double[][]> time2NormalizedWeightedEmissions1 = normalizeAllArrays(time2WeightedEmissions1);
		
		return time2NormalizedWeightedEmissions1;
	}

	private void compareLinkLength() {
		
		Double totalnetworklength =0.0;
		Double totalcoordlength =0.0;
		
		// all links of the network
		for(Id linkId: this.network.getLinks().keySet()){
			Double linklengthfromnetwork = this.network.getLinks().get(linkId).getLength();
			Coord fromNodeCoord = this.network.getLinks().get(linkId).getFromNode().getCoord();
			Coord toNodeCoord = this.network.getLinks().get(linkId).getToNode().getCoord();
			Double linklengthfromcoords = saue.getLinkLengthUsingFromAndToNode(fromNodeCoord, toNodeCoord);
			Double diff = (linklengthfromcoords-linklengthfromnetwork);
			System.out.println("difference " + diff + "relative diff " + diff/linklengthfromnetwork);
			totalcoordlength+= linklengthfromcoords;
			totalnetworklength+=linklengthfromnetwork;
		}
		
		System.out.println("total network length " + totalnetworklength);
		System.out.println("total coord length " + totalcoordlength);
		
	}

	private Map<Double, Double[][]> fillWeightedEmissionValues(Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered){
		Map<Double, Double[][]> time2weightedEmissions = new HashMap<Double, Double[][]>();
		
		for(Double endOfTimeInterval : time2EmissionsTotalFilledAndFiltered.keySet()){
			Double[][]weightedEmissions = new Double[noOfXbins][noOfYbins];
			for(int i=0; i<noOfXbins; i++){
				for(int j=0; j<noOfYbins; j++){
					weightedEmissions[i][j]=0.0;
				}
			}
			
			for(Id<Link> linkId : time2EmissionsTotalFilledAndFiltered.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();
				Coord fromNodeCoord = this.network.getLinks().get(linkId).getFromNode().getCoord();
				Coord toNodeCoord = this.network.getLinks().get(linkId).getToNode().getCoord();
				
				Double value = time2EmissionsTotalFilledAndFiltered.get(endOfTimeInterval).get(linkId).get(this.pollutant2analyze);
				if(value>0.0){
					System.out.println("emission value --- " + value);
				}
				Double scaledValue = this.scalingFactor * value;
				
				/* maybe calculate the following once and look it up here?
				 * suggest rather not to:
				 * for one time bin this is calculated only once since emissions are aggregated per link
				 * otherwise for each link a 160x120 array of Double is stored
				 * 
				 * nearly 18,000 nodes, 40,000 links
				 * 19,200 cells
				 * 
				 */
				for(int xIndex=0; xIndex<noOfXbins; xIndex++){
					for (int yIndex=0; yIndex<noOfYbins; yIndex++){
						Coord cellCentroid = this.sau.findCellCentroid(xIndex, yIndex);
						Double weightOfLinkForCell =0.0;
						if(!useLineMethod){
							 weightOfLinkForCell = this.sau.calculateWeightOfPointForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
						}else{
							 weightOfLinkForCell = this.saue.calculateWeightOfLineForCellV2(fromNodeCoord, toNodeCoord, cellCentroid.getX(), cellCentroid.getY());
						}
						weightedEmissions[xIndex][yIndex] += weightOfLinkForCell * scaledValue;					
					}
				}
			}
			time2weightedEmissions.put(endOfTimeInterval, weightedEmissions);
		}
		return time2weightedEmissions;
	}
	
	private Map<Double, Double[][]> fillWeightedDemandValues(Map<Double, Map<Id<Link>, Double>> time2CountsPerLinkFilledAndFiltered) {
		Map<Double, Double[][]> time2weightedDemand = new HashMap<Double, Double[][]>();
		
		for(Double endOfTimeInterval : time2CountsPerLinkFilledAndFiltered.keySet()){
			Double[][]weightedDemand = new Double[noOfXbins][noOfYbins];
			
			for(int i=0; i<noOfXbins; i++){
				for(int j=0; j<noOfYbins; j++){
					weightedDemand[i][j]=0.0;
				}
			}
			
			for(Id<Link> linkId : time2CountsPerLinkFilledAndFiltered.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				Double xLink = linkCoord.getX();
				Double yLink = linkCoord.getY();
				Coord fromNodeCoord = this.network.getLinks().get(linkId).getFromNode().getCoord();
				Coord toNodeCoord = this.network.getLinks().get(linkId).getToNode().getCoord();
				Double linkLength_km = this.network.getLinks().get(linkId).getLength() / 1000.;
				
				Double count = time2CountsPerLinkFilledAndFiltered.get(endOfTimeInterval).get(linkId);
				Double vkm = count * linkLength_km;
				Double scaledVkm = this.scalingFactor * vkm;
				
				// maybe calculate the following once and look it up here? see above
				for(int xIndex=0; xIndex<noOfXbins; xIndex++){
					for (int yIndex=0; yIndex<noOfYbins; yIndex++){
						Coord cellCentroid = this.sau.findCellCentroid(xIndex, yIndex);
						Double weightOfLinkForCell =0.0;
						if(!useLineMethod){
							weightOfLinkForCell = this.sau.calculateWeightOfPointForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
						}else{
							weightOfLinkForCell = this.saue.calculateWeightOfLineForCellV2(fromNodeCoord, toNodeCoord, cellCentroid.getX(), cellCentroid.getY());
						}
						weightedDemand[xIndex][yIndex] += weightOfLinkForCell * scaledVkm;					
					}
				}
			}
			time2weightedDemand.put(endOfTimeInterval, weightedDemand);
		}
		return time2weightedDemand;
	}

	private Map<Double, Double[][]> calculateSpecificEmissionsPerBin(
			Map<Double, Double[][]> time2weightedEmissions,
			Map<Double, Double[][]> time2weightedDemand) {
		
		Map<Double, Double[][]> time2specificEmissions = new HashMap<Double, Double[][]>();
		for( Double endOfTimeInterval : time2weightedEmissions.keySet()){
			Double [][] specificEmissions = new Double[noOfXbins][noOfYbins];
			for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
					specificEmissions[xIndex][yIndex] = time2weightedEmissions.get(endOfTimeInterval)[xIndex][yIndex] / time2weightedDemand.get(endOfTimeInterval)[xIndex][yIndex];
				}
			}
			time2specificEmissions.put(endOfTimeInterval, specificEmissions);
		}
		return time2specificEmissions;
	}
	
	private Map<Double, Double[][]> calculateAbsoluteDifferencesPerBin(
			Map<Double, Double[][]> time2weightedValues1,
			Map<Double, Double[][]> time2weightedValues2){
		
		Map<Double, Double[][]> time2absoluteDifferences = new HashMap<Double, Double[][]>();
		for(Double endOfTimeInterval : time2weightedValues1.keySet()){
			Double [][] absoluteDifferences = new Double[noOfXbins][noOfYbins];
			for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
				for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
					absoluteDifferences[xIndex][yIndex] = time2weightedValues2.get(endOfTimeInterval)[xIndex][yIndex] - time2weightedValues1.get(endOfTimeInterval)[xIndex][yIndex];
				}
			}
			time2absoluteDifferences.put(endOfTimeInterval, absoluteDifferences);
		}		
		return time2absoluteDifferences;
	}

	private Map<Double, Double[][]> normalizeAllArrays(
			Map<Double, Double[][]> time2Array) {
		Map<Double, Double[][]> time2NormalizedArray = new HashMap<Double, Double[][]>();
			for(Double endOfTimeInterval : time2Array.keySet()){
				Double[][] normalizedArray = this.sau.normalizeArray(time2Array.get(endOfTimeInterval));
				time2NormalizedArray.put(endOfTimeInterval, normalizedArray);
			}
		return time2NormalizedArray;
	}

	private Map<Double, Map<Id<Link>, Double>> setNonCalculatedCountsAndFilter(Map<Double, Map<Id<Link>, Double>> time2CountsPerLink) {
		Map<Double, Map<Id<Link>, Double>> time2CountsTotalFiltered = new HashMap<>();
	
		for(Double endOfTimeInterval : time2CountsPerLink.keySet()){
			Map<Id<Link>, Double> linkId2Count = time2CountsPerLink.get(endOfTimeInterval);
			Map<Id<Link>, Double> linkId2CountFiltered = new HashMap<>();
		
			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				if(this.sau.isInResearchArea(linkCoord)){
					Id<Link> linkId = link.getId();
	
					if(linkId2Count.get(linkId) == null){
						linkId2CountFiltered.put(linkId, 0.);
					} else {
						linkId2CountFiltered.put(linkId, linkId2Count.get(linkId));
					}
				}
			}
			time2CountsTotalFiltered.put(endOfTimeInterval, linkId2CountFiltered);
		}
		return time2CountsTotalFiltered;
	}

	private Map<Double, Map<Id<Link>, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotalFilled = new HashMap<Double, Map<Id<Link>, SortedMap<String, Double>>>();
		
		for(Double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id<Link>, SortedMap<String, Double>> emissionsTotalFilled = this.emissionUtils.setNonCalculatedEmissionsForNetwork(this.network, time2EmissionsTotal.get(endOfTimeInterval));
			time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
		}
		return time2EmissionsTotalFilled;
	}

	private Map<Double, Map<Id<Link>, Map<String, Double>>> filterEmissionLinks(Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id<Link>, Map<String, Double>>> time2EmissionsTotalFiltered = new HashMap<>();
		
		for(Double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id<Link>, SortedMap<String, Double>> emissionsTotal = time2EmissionsTotal.get(endOfTimeInterval);
			Map<Id<Link>, Map<String, Double>> emissionsTotalFiltered = new HashMap<>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				if(this.sau.isInResearchArea(linkCoord)){
					Id<Link> linkId = link.getId();
					emissionsTotalFiltered.put(linkId, emissionsTotal.get(linkId));
				}
			}
			time2EmissionsTotalFiltered.put(endOfTimeInterval, emissionsTotalFiltered);
		}
		return time2EmissionsTotalFiltered;
	}

	private Map<Double, Map<Id<Link>, SortedMap<String, Double>>> sumUpEmissionsPerTimeInterval(
			Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> time2warmEmissionsTotal,
			Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> time2coldEmissionsTotal) {
	
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2totalEmissions = new HashMap<>();
	
		for(Double endOfTimeInterval: time2warmEmissionsTotal.keySet()){
			Map<Id<Link>, Map<WarmPollutant, Double>> warmEmissions = time2warmEmissionsTotal.get(endOfTimeInterval);
			
			Map<Id<Link>, SortedMap<String, Double>> totalEmissions = new HashMap<>();
			if(time2coldEmissionsTotal.get(endOfTimeInterval) == null){
				for(Id<Link> id : warmEmissions.keySet()){
					SortedMap<String, Double> warmEmissionsOfLink = this.emissionUtils.convertWarmPollutantMap2String(warmEmissions.get(id));
					totalEmissions.put(id, warmEmissionsOfLink);
				}
			} else {
				Map<Id<Link>, Map<ColdPollutant, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);
				totalEmissions = this.emissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions);
			}
			time2totalEmissions.put(endOfTimeInterval, totalEmissions);
		}
		return time2totalEmissions;
	}

	private Map<Double, Map<Id<Link>, Map<String, Double>>> processEmissions(String emissionFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		this.warmHandler = new EmissionsPerLinkWarmEventHandler(this.simulationEndTime, noOfTimeBins);
		this.coldHandler = new EmissionsPerLinkColdEventHandler(this.simulationEndTime, noOfTimeBins);
		eventsManager.addHandler(this.warmHandler);
		eventsManager.addHandler(this.coldHandler);
		emissionReader.parse(emissionFile);
		
		Map<Double, Map<Id<Link>, Map<WarmPollutant, Double>>> time2WarmEmissionsTotal1 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		System.out.println("number of links with warm emissions");
		for(Double et: time2WarmEmissionsTotal1.keySet()){
			System.out.println(time2WarmEmissionsTotal1.get(et).size());
		}
		Map<Double, Map<Id<Link>, Map<ColdPollutant, Double>>> time2ColdEmissionsTotal1 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotal1 = sumUpEmissionsPerTimeInterval(time2WarmEmissionsTotal1, time2ColdEmissionsTotal1);
		Map<Double, Map<Id<Link>, SortedMap<String, Double>>> time2EmissionsTotalFilled1 = setNonCalculatedEmissions(time2EmissionsTotal1);
		
		return filterEmissionLinks(time2EmissionsTotalFilled1);
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