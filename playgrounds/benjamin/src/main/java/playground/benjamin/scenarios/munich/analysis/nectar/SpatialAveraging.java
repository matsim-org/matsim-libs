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
package playground.benjamin.scenarios.munich.analysis.nectar;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.matsim.core.utils.misc.Time;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.vsp.emissions.events.EmissionEventsReader;
import playground.vsp.emissions.types.ColdPollutant;
import playground.vsp.emissions.types.WarmPollutant;
import playground.vsp.emissions.utils.EmissionUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.util.Assert;

/**
 * @author benjamin
 *
 */
public class SpatialAveraging {
	private static final Logger logger = Logger.getLogger(SpatialAveraging.class);

//	final double scalingFactor = 100.;
//	private final static String runNumber1 = "baseCase";
//	private final static String runDirectory1 = "../../runs-svn/detEval/latsis/output/output_baseCase_ctd_newCode/";
//	private final static String runNumber2 = "zone30";
//	private final static String runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_zone30/";
////	private final static String runNumber2 = "pricing";
////	private final static String runDirectory2 = "../../runs-svn/detEval/latsis/output/output_policyCase_pricing_newCode/";
//	private final String netFile1 = runDirectory1 + "output_network.xml.gz";
//	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";
//
//	private static String configFile1 = runDirectory1 + "output_config.xml.gz";
//	private final static Integer lastIteration1 = getLastIteration(configFile1);
//	private static String configFile2 = runDirectory1 + "output_config.xml.gz";
//	private final static Integer lastIteration2 = getLastIteration(configFile2);
//	private final String emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + lastIteration1 + ".emission.events.xml.gz";
//	private final String emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration2 + "/" + lastIteration2 + ".emission.events.xml.gz";
	
	final double scalingFactor = 10.;
	private final static String runNumber1 = "981";
	private final static String runNumber2 = "983";
	private final static String runDirectory1 = "../../runs-svn/run" + runNumber1 + "/";
	private final static String runDirectory2 = "../../runs-svn/run" + runNumber2 + "/";
	private final String netFile1 = runDirectory1 + runNumber1 + ".output_network.xml.gz";
	private final String munichShapeFile = "../../detailedEval/Net/shapeFromVISUM/urbanSuburban/cityArea.shp";

	private static String configFile1 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
	private final static Integer lastIteration1 = getLastIteration(configFile1);
	private static String configFile2 = runDirectory1 + runNumber1 + ".output_config.xml.gz";
	private final static Integer lastIteration2 = getLastIteration(configFile2);
	private final String emissionFile1 = runDirectory1 + "ITERS/it." + lastIteration1 + "/" + runNumber1 + "." + lastIteration1 + ".emission.events.xml.gz";
	private final String emissionFile2 = runDirectory2 + "ITERS/it." + lastIteration2 + "/" + runNumber2 + "." + lastIteration2 + ".emission.events.xml.gz";

	Network network;
	Collection<SimpleFeature> featuresInMunich;
	EmissionUtils emissionUtils = new EmissionUtils();
	EmissionsPerLinkWarmEventHandler warmHandler;
	EmissionsPerLinkColdEventHandler coldHandler;
	SortedSet<String> listOfPollutants;
	double simulationEndTime;
	String outPathStub;

	final CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG:20004");
	static double xMin = 4452550.25;
	static double xMax = 4479483.33;
	static double yMin = 5324955.00;
	static double yMax = 5345696.81;

	final int noOfTimeBins = 1;
	final int noOfXbins = 160;
	final int noOfYbins = 120;
	final double smoothingRadius_m = 500.;
	final double area_in_smoothing_circle_sqkm = (Math.PI * this.smoothingRadius_m * this.smoothingRadius_m) / (1000. * 1000.);
	final double smoothinRadiusSquared_m = smoothingRadius_m * smoothingRadius_m;
	final String pollutant2analyze = WarmPollutant.NO2.toString();
	final boolean compareToBaseCase = true;
	
	private void run() throws IOException{
		this.simulationEndTime = getEndTime(configFile1);
		this.listOfPollutants = emissionUtils.getListOfPollutants();
		Scenario scenario = loadScenario(netFile1);
		this.network = scenario.getNetwork();		
		this.featuresInMunich = ShapeFileReader.getAllFeatures(munichShapeFile);
		
		processEmissions(emissionFile1);
		Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2WarmEmissionsTotal1 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2ColdEmissionsTotal1 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
		Map<Double, Map<Id, Double>> time2CountsPerLink1 = this.warmHandler.getTime2linkIdLeaveCount();

		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal1 = sumUpEmissionsPerTimeInterval(time2WarmEmissionsTotal1, time2ColdEmissionsTotal1);
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled1 = setNonCalculatedEmissions(time2EmissionsTotal1);
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered1 = filterEmissionLinks(time2EmissionsTotalFilled1);
		Map<Double, Map<Id, Double>> time2CountsPerLinkFilledAndFiltered1 = setNonCalculatedCountsAndFilter(time2CountsPerLink1);

		this.warmHandler.reset(0);
		this.coldHandler.reset(0);

		Map<Double, double[][]> time2WeightedEmissions1 = fillWeightedEmissionValues(time2EmissionsTotalFilledAndFiltered1);
		Map<Double, double[][]> time2NormalizedWeightedEmissions1 = normalizeAllArrays(time2WeightedEmissions1);
				
		Map<Double, double[][]> time2WeightedDemand1 = fillWeightedDemandValues(time2CountsPerLinkFilledAndFiltered1);
		Map<Double, double[][]> time2NormalizedWeightedDemand1 = normalizeAllArrays(time2WeightedDemand1);
		
		/* Sum over weighted values for cell does not need to be normalized, since normalization canceles out.
		 * Result is an average value for cell 
		 * TODO: check if results look similar...*/
		Map<Double, double[][]> time2SpecificEmissions1 = calculateSpecificEmissionsPerBin(time2WeightedEmissions1, time2WeightedDemand1);
//		Map<Double, double[][]> time2SpecificEmissions1 = calculateSpecificEmissionsPerBin(time2NormalizedWeightedEmissions1, time2NormalizedWeightedDemand1);
		

		outPathStub = runDirectory1 + "analysis/spatialAveraging/" + runNumber1 + "." + lastIteration1;
		for(double endOfTimeInterval: time2WeightedEmissions1.keySet()){
			writeRoutput(time2NormalizedWeightedEmissions1.get(endOfTimeInterval), outPathStub + ".Routput." + pollutant2analyze.toString() + ".g." + endOfTimeInterval + ".txt");
			writeRoutput(time2NormalizedWeightedDemand1.get(endOfTimeInterval), outPathStub + ".Routput.Demand.vkm." + endOfTimeInterval + ".txt");
			writeRoutput(time2SpecificEmissions1.get(endOfTimeInterval), outPathStub+ ".Routput." + pollutant2analyze + ".gPerVkm." + endOfTimeInterval + ".txt");
			
//			writeGISoutput(time2WeightedEmissions1, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".g.movie.shp");
//			writeGISoutput(time2WeightedDemand1, outPathStub + ".GISoutput.Demand.vkm.movie.shp");
//			writeGISoutput(time2SpecificEmissions, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".gPerVkm.movie.shp");
		}
		
		if(compareToBaseCase){
			processEmissions(emissionFile2);
			Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2WarmEmissionsTotal2 = this.warmHandler.getWarmEmissionsPerLinkAndTimeInterval();
			Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2ColdEmissionsTotal2 = this.coldHandler.getColdEmissionsPerLinkAndTimeInterval();
			Map<Double, Map<Id, Double>> time2CountsPerLink2 = this.warmHandler.getTime2linkIdLeaveCount();

			Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal2 = sumUpEmissionsPerTimeInterval(time2WarmEmissionsTotal2, time2ColdEmissionsTotal2);
			Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled2 = setNonCalculatedEmissions(time2EmissionsTotal2);
			Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered2 = filterEmissionLinks(time2EmissionsTotalFilled2);
			Map<Double, Map<Id, Double>> time2CountsPerLinkFilledAndFiltered2 = setNonCalculatedCountsAndFilter(time2CountsPerLink2);

			Map<Double, double[][]> time2WeightedEmissions2 = fillWeightedEmissionValues(time2EmissionsTotalFilledAndFiltered2);
			Map<Double, double[][]> time2NormalizedWeightedEmissions2 = normalizeAllArrays(time2WeightedEmissions2);
			
			Map<Double, double[][]> time2WeightedDemand2 = fillWeightedDemandValues(time2CountsPerLinkFilledAndFiltered2);
			Map<Double, double[][]> time2NormalizedWeightedDemand2 = normalizeAllArrays(time2WeightedDemand2);
			
			/* Sum over weighted values for cell does not need to be normalized, since normalization canceles out.
			 * Result is an average value for cell 
			 * TODO: check if results look similar...*/
			Map<Double, double[][]> time2SpecificEmissions2 = calculateSpecificEmissionsPerBin(time2WeightedEmissions2, time2WeightedDemand2);
//			Map<Double, double[][]> time2SpecificEmissions2 = calculateSpecificEmissionsPerBin(time2NormalizedWeightedEmissions2, time2NormalizedWeightedDemand2);
			
			/* Sum over weighted values for cell is normalized to "per sqkm" (dependent on calcluateWeightOfLinkForCell)
			 * Values NEED to be additive (e.g. vkm, g, counts, or AVERAGE g/vkm!)
			 * Make sure coordinate system is metric */
			Map<Double, double[][]> time2AbsoluteEmissionDifferences = calculateAbsoluteDifferencesPerBin(time2NormalizedWeightedEmissions1, time2NormalizedWeightedEmissions2);
			Map<Double, double[][]> time2AbsoluteDemandDifferences = calculateAbsoluteDifferencesPerBin(time2NormalizedWeightedDemand1, time2NormalizedWeightedDemand2);
			Map<Double, double[][]> time2SpecificEmissionDifferences = calculateAbsoluteDifferencesPerBin(time2SpecificEmissions1, time2SpecificEmissions2);

			outPathStub = runDirectory1 + "analysis/spatialAveraging/" + runNumber2 + "." + lastIteration2 + "-" + runNumber1 + "." + lastIteration1 + ".absoluteDelta";
			for(double endOfTimeInterval : time2AbsoluteDemandDifferences.keySet()){
				writeRoutput(time2AbsoluteEmissionDifferences.get(endOfTimeInterval), outPathStub + ".Routput." + pollutant2analyze + ".g." + endOfTimeInterval + ".txt");
				writeRoutput(time2AbsoluteDemandDifferences.get(endOfTimeInterval),	outPathStub + ".Routput.Demand.vkm." + endOfTimeInterval + ".txt");
				writeRoutput(time2SpecificEmissionDifferences.get(endOfTimeInterval), outPathStub + ".Routput." + pollutant2analyze.toString() + ".gPerVkm." + endOfTimeInterval + ".txt");

//				writeGISoutput(time2AbsoluteEmissionDifferences, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".g.movie.shp");
//				writeGISoutput(time2AbsoluteDemandDifferences, outPathStub + ".GISoutput.Demand.vkm.movie.shp");
//				writeGISoutput(time2SpecificEmissionDifferences, outPathStub +  ".GISoutput." + pollutant2analyze.toString() + ".gPerVkm.movie.shp");
			}	
		}
	}
	
	private void writeRoutput(double[][] results, String outputPathForR) {
		try {
			BufferedWriter buffW = new BufferedWriter(new FileWriter(outputPathForR));
			String valueString = new String();
			valueString = "\t";
			
			//x-coordinates as first row
			for(int xIndex = 0; xIndex < results.length; xIndex++){
				valueString += findBinCenterX(xIndex) + "\t";
			}
			buffW.write(valueString);
			buffW.newLine();
			valueString = new String();
			
			for(int yIndex = 0; yIndex < results[0].length; yIndex++){
				//y-coordinates as first column
				valueString += findBinCenterY(yIndex) + "\t";
				//table contents
				for(int xIndex = 0; xIndex < results.length; xIndex++){ 
						Coord cellCentroid = findCellCentroid(xIndex, yIndex);
						if(isInMunich(cellCentroid)){
							valueString += Double.toString(results[xIndex][yIndex]) + "\t"; 
						} else {
							valueString += "NA" + "\t";
						}
				}
				buffW.write(valueString);
				buffW.newLine();
				valueString = new String();
			}
		buffW.close();	
		} catch (IOException e) {
			throw new RuntimeException("Failed writing output for R.");
		}	
		logger.info("Finished writing output for R to " + outputPathForR);
	}

	private void writeGISoutput(Map<Double, double[][]> time2results, String outputPathForGIS) throws IOException {
		
		PointFeatureFactory factory = new PointFeatureFactory.Builder()
		.setCrs(this.targetCRS)
		.setName("EmissionPoint")
		.addAttribute("Time", String.class)
		.addAttribute("Emissions", Double.class)
		.create();
		Collection<SimpleFeature> features = new ArrayList<SimpleFeature>();
		
		for(Double endOfTimeInterval : time2results.keySet()){
//			if(endOfTimeInterval < Time.MIDNIGHT){ // time manager in QGIS does not accept time beyond midnight...
				for(int xIndex = 0; xIndex < noOfXbins; xIndex++){
					for(int yIndex = 0; yIndex < noOfYbins; yIndex++){
						Coord cellCentroid = findCellCentroid(xIndex, yIndex);
						if(isInMunich(cellCentroid)){
							String dateTimeString = convertSeconds2dateTimeFormat(endOfTimeInterval);
							double result = time2results.get(endOfTimeInterval)[xIndex][yIndex];
							SimpleFeature feature = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()), new Object[] {dateTimeString, result}, null);
							features.add(feature);
						}
					}
				}
//			}
		}
		ShapeFileWriter.writeGeometries(features, outputPathForGIS);
		logger.info("Finished writing output for GIS to " + outputPathForGIS);
	}
	
	private Map<Double, double[][]> fillWeightedEmissionValues(Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFilledAndFiltered) {
		Map<Double, double[][]> time2weightedEmissions = new HashMap<Double, double[][]>();
		
		for(Double endOfTimeInterval : time2EmissionsTotalFilledAndFiltered.keySet()){
			double[][]weightedEmissions = new double[noOfXbins][noOfYbins];
			
			for(Id linkId : time2EmissionsTotalFilledAndFiltered.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				double xLink = linkCoord.getX();
				double yLink = linkCoord.getY();
				
				double value = time2EmissionsTotalFilledAndFiltered.get(endOfTimeInterval).get(linkId).get(this.pollutant2analyze);
				double scaledValue = this.scalingFactor * value;
				
				// TODO: maybe calculate the following once and look it up here?
				for(int xIndex=0; xIndex<noOfXbins; xIndex++){
					for (int yIndex=0; yIndex<noOfYbins; yIndex++){
						Coord cellCentroid = findCellCentroid(xIndex, yIndex);
						double weightOfLinkForCell = calculateWeightOfLinkForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
						weightedEmissions[xIndex][yIndex] += weightOfLinkForCell * scaledValue;					
					}
				}
			}
			time2weightedEmissions.put(endOfTimeInterval, weightedEmissions);
		}
		return time2weightedEmissions;
	}
	
	private Map<Double, double[][]> fillWeightedDemandValues(Map<Double, Map<Id, Double>> time2CountsPerLinkFilledAndFiltered) {
		Map<Double, double[][]> time2weightedDemand = new HashMap<Double, double[][]>();
		
		for(Double endOfTimeInterval : time2CountsPerLinkFilledAndFiltered.keySet()){
			double[][]weightedDemand = new double[noOfXbins][noOfYbins];
			
			for(Id linkId : time2CountsPerLinkFilledAndFiltered.get(endOfTimeInterval).keySet()){
				Coord linkCoord = this.network.getLinks().get(linkId).getCoord();
				double xLink = linkCoord.getX();
				double yLink = linkCoord.getY();
				double linkLength_km = this.network.getLinks().get(linkId).getLength() / 1000.;
				
				double count = time2CountsPerLinkFilledAndFiltered.get(endOfTimeInterval).get(linkId);
				double vkm = count * linkLength_km;
				double scaledVkm = this.scalingFactor * vkm;
				
				// TODO: maybe calculate the following once and look it up here?
				for(int xIndex=0; xIndex<noOfXbins; xIndex++){
					for (int yIndex=0; yIndex<noOfYbins; yIndex++){
						Coord cellCentroid = findCellCentroid(xIndex, yIndex);
						double weightOfLinkForCell = calculateWeightOfLinkForCell(xLink, yLink, cellCentroid.getX(), cellCentroid.getY());
						weightedDemand[xIndex][yIndex] += weightOfLinkForCell * scaledVkm;					
					}
				}
			}
			time2weightedDemand.put(endOfTimeInterval, weightedDemand);
		}
		return time2weightedDemand;
	}

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

	private Map<Double, double[][]> normalizeAllArrays(
			Map<Double, double[][]> time2Array) {
		Map<Double, double[][]> time2NormalizedArray = new HashMap<Double, double[][]>();
			for(Double endOfTimeInterval : time2Array.keySet()){
				double[][] normalizedArray = normalizeArray(time2Array.get(endOfTimeInterval));
				time2NormalizedArray.put(endOfTimeInterval, normalizedArray);
			}
		return time2NormalizedArray;
	}

	private double[][] normalizeArray(double[][] array) {
		double [][] normalizedArray = new double[noOfXbins][noOfYbins];
		for(int xIndex = 0; xIndex<noOfXbins; xIndex++){
			for(int yIndex = 0; yIndex<noOfYbins; yIndex++){
				normalizedArray[xIndex][yIndex] = array[xIndex][yIndex] / this.area_in_smoothing_circle_sqkm;
			}
		}
		return normalizedArray;
	}
	
	private boolean isInMunich(Coord cellCentroid) {
		boolean isInMunichShape = false;
		GeometryFactory factory = new GeometryFactory();
		Geometry geo = factory.createPoint(new Coordinate(cellCentroid.getX(), cellCentroid.getY()));
		for(SimpleFeature feature : this.featuresInMunich){
			if(((Geometry) feature.getDefaultGeometry()).contains(geo)){
				isInMunichShape = true;
				break;
			}
		}
		return isInMunichShape;
	}

	private boolean isInResearchArea(Coord linkCoord) {
		Double xLink = linkCoord.getX();
		Double yLink = linkCoord.getY();
		
		if(xLink > xMin && xLink < xMax){
			if(yLink > yMin && yLink < yMax){
				return true;
			}
		}
		return false;
	}

	private String convertSeconds2dateTimeFormat(double endOfTimeInterval) {
		String date = "2012-04-13 ";
		String time = Time.writeTime(endOfTimeInterval, Time.TIMEFORMAT_HHMM);
		String dateTimeString = date + time;
		return dateTimeString;
	}

	private double calculateWeightOfLinkForCell(double x1, double y1, double x2, double y2) {
		double distanceSquared = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
		return Math.exp((-distanceSquared) / (smoothinRadiusSquared_m));
	}
	
//	private double calculateWeightOfLinkForCell(double x1, double y1, double x2, double y2) {
//		// check if x and y values are in the same cell:
//		if ( mapXCoordToBin(x1) == mapXCoordToBin(x2) ) {
//			if ( mapYCoordToBin(y1) == mapYCoordToBin(y2) ) {
//				return 1. ;
//			}
//		}
//		return 0. ;
//	}

	private double findBinCenterY(int yIndex) {
		double yBinCenter = yMin + ((yIndex + .5) / noOfYbins) * (yMax - yMin);
		Assert.equals(mapYCoordToBin(yBinCenter), yIndex);
		return yBinCenter ;
	}

	private double findBinCenterX(int xIndex) {
		double xBinCenter = xMin + ((xIndex + .5) / noOfXbins) * (xMax - xMin);
		Assert.equals(mapXCoordToBin(xBinCenter), xIndex);
		return xBinCenter ;
	}

	private Coord findCellCentroid(int xIndex, int yIndex) {
		double xCentroid = findBinCenterX(xIndex);
		double yCentroid = findBinCenterY(yIndex);
		Coord cellCentroid = new CoordImpl(xCentroid, yCentroid);
		return cellCentroid;
	}

	private Integer mapYCoordToBin(double yCoord) {
		if (yCoord <= yMin || yCoord >= yMax) return null; // yCoord is not in area of interest
		double relativePositionY = ((yCoord - yMin) / (yMax - yMin) * noOfYbins); // gives the relative position along the y-range
		return (int) relativePositionY; // returns the number of the bin [0..n-1]
	}

	private Integer mapXCoordToBin(double xCoord) {
		if (xCoord <= xMin || xCoord >= xMax) return null; // xCorrd is not in area of interest
		double relativePositionX = ((xCoord - xMin) / (xMax - xMin) * noOfXbins); // gives the relative position along the x-range
		return (int) relativePositionX; // returns the number of the bin [0..n-1]
	}

	private Map<Double, Map<Id, Double>> setNonCalculatedCountsAndFilter(Map<Double, Map<Id, Double>> time2CountsPerLink) {
		Map<Double, Map<Id, Double>> time2CountsTotalFiltered = new HashMap<Double, Map<Id,Double>>();
	
		for(Double endOfTimeInterval : time2CountsPerLink.keySet()){
			Map<Id, Double> linkId2Count = time2CountsPerLink.get(endOfTimeInterval);
			Map<Id, Double> linkId2CountFiltered = new HashMap<Id, Double>();
		
			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				if(isInResearchArea(linkCoord)){
					Id linkId = link.getId();
	
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

	private Map<Double, Map<Id, SortedMap<String, Double>>> setNonCalculatedEmissions(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotalFilled = new HashMap<Double, Map<Id, SortedMap<String, Double>>>();
		
		for(double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, SortedMap<String, Double>> emissionsTotalFilled = this.emissionUtils.setNonCalculatedEmissionsForNetwork(this.network, time2EmissionsTotal.get(endOfTimeInterval));
			time2EmissionsTotalFilled.put(endOfTimeInterval, emissionsTotalFilled);
		}
		return time2EmissionsTotalFilled;
	}

	private Map<Double, Map<Id, Map<String, Double>>> filterEmissionLinks(Map<Double, Map<Id, SortedMap<String, Double>>> time2EmissionsTotal) {
		Map<Double, Map<Id, Map<String, Double>>> time2EmissionsTotalFiltered = new HashMap<Double, Map<Id, Map<String, Double>>>();
		
		for(Double endOfTimeInterval : time2EmissionsTotal.keySet()){
			Map<Id, SortedMap<String, Double>> emissionsTotal = time2EmissionsTotal.get(endOfTimeInterval);
			Map<Id, Map<String, Double>> emissionsTotalFiltered = new HashMap<Id, Map<String, Double>>();

			for(Link link : network.getLinks().values()){
				Coord linkCoord = link.getCoord();
				if(isInResearchArea(linkCoord)){
					Id linkId = link.getId();
					emissionsTotalFiltered.put(linkId, emissionsTotal.get(linkId));
				}
			}
			time2EmissionsTotalFiltered.put(endOfTimeInterval, emissionsTotalFiltered);
		}
		return time2EmissionsTotalFiltered;
	}

	private Map<Double, Map<Id, SortedMap<String, Double>>> sumUpEmissionsPerTimeInterval(
			Map<Double, Map<Id, Map<WarmPollutant, Double>>> time2warmEmissionsTotal,
			Map<Double, Map<Id, Map<ColdPollutant, Double>>> time2coldEmissionsTotal) {
	
		Map<Double, Map<Id, SortedMap<String, Double>>> time2totalEmissions = new HashMap<Double, Map<Id, SortedMap<String, Double>>>();
	
		for(double endOfTimeInterval: time2warmEmissionsTotal.keySet()){
			Map<Id, Map<WarmPollutant, Double>> warmEmissions = time2warmEmissionsTotal.get(endOfTimeInterval);
			
			Map<Id, SortedMap<String, Double>> totalEmissions = new HashMap<Id, SortedMap<String, Double>>();
			if(time2coldEmissionsTotal.get(endOfTimeInterval) == null){
				for(Id id : warmEmissions.keySet()){
					SortedMap<String, Double> warmEmissionsOfLink = this.emissionUtils.convertWarmPollutantMap2String(warmEmissions.get(id));
					totalEmissions.put(id, warmEmissionsOfLink);
				}
			} else {
				Map<Id, Map<ColdPollutant, Double>> coldEmissions = time2coldEmissionsTotal.get(endOfTimeInterval);
				totalEmissions = this.emissionUtils.sumUpEmissionsPerId(warmEmissions, coldEmissions);
			}
			time2totalEmissions.put(endOfTimeInterval, totalEmissions);
		}
		return time2totalEmissions;
	}

	private void processEmissions(String emissionFile) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		EmissionEventsReader emissionReader = new EmissionEventsReader(eventsManager);
		this.warmHandler = new EmissionsPerLinkWarmEventHandler(this.simulationEndTime, noOfTimeBins);
		this.coldHandler = new EmissionsPerLinkColdEventHandler(this.simulationEndTime, noOfTimeBins);
		eventsManager.addHandler(this.warmHandler);
		eventsManager.addHandler(this.coldHandler);
		emissionReader.parse(emissionFile);
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
		new SpatialAveraging().run();
	}
}